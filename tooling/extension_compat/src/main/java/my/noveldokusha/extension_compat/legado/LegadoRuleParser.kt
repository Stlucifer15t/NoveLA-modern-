package my.noveldokusha.extension_compat.legado

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.mozilla.javascript.ClassShutter
import org.mozilla.javascript.Context
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined
import java.util.concurrent.ConcurrentHashMap
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import org.jsoup.helper.W3CDom

/**
 * Результат парсинга правила Legado.
 *
 * Грамматика (по документации Legado и LegadoParser, сохранённой в docs/reference):
 *  - `||`   — альтернативы: берётся первое непустое
 *  - `&&`   — конкатенация результатов цепочек
 *  - `%%`   — транспонирование (zip) результатов цепочек
 *  - `@`    — шаги цепочки: селектор → селектор → end-правило
 *  - `##p##r` — regex-замена; `##p` — удаление совпадений; `:p` — findall
 *  - `@js:` / `<js>...</js>` — JavaScript с переменной result
 *  - `{{...}}`, `@get:{k}`, `@put:{k}` — шаблоны и переменные правил
 *  - Префиксы шагов: `@css:`, `@json:`, `@xpath:`; без префикса — CSS
 *    (XPath если начинается с `/`, JSONPath если с `$.`/`$[`)
 *  - End-правила: text, textNodes, ownText, html, all, src, href, или
 *    произвольное имя атрибута (./@attr)
 */
sealed class LegadoStep {
    data class Css(val selector: String) : LegadoStep()
    data class XPath(val expression: String) : LegadoStep()
    data class JsonPath(val path: String) : LegadoStep()
    data class Js(val code: String) : LegadoStep()
    data class End(val name: String) : LegadoStep()
    data class RegexOp(val pattern: String, val replacement: String?, val mode: Mode) : LegadoStep() {
        enum class Mode { REPLACE, REPLACE_FIRST, EXTRACT_FIRST, FIND_ALL, REMOVE }
    }
    /** `@put:{key:rule}` — вычислить правило и сохранить в переменные. */
    data class Put(val entries: Map<String, String>) : LegadoStep()
    /** `@get:{key}` — подставить сохранённую переменную как конечный результат. */
    data class Get(val key: String) : LegadoStep()

    /**
     * Инлайн-шаблон: строка с `@get:{k}` / `{{var}}` / `{{$.jsonpath}}`.
     * Подстановка выполняется при выполнении против vars/rawInput.
     */
    data class Template(val raw: String) : LegadoStep()
}

data class LegadoChain(val steps: List<LegadoStep>)
data class LegadoGroup(
    val chains: List<LegadoChain>,
    /** "" — последовательно; "&&" — конкатенация; "||" — первая непустая; "%%" — zip */
    val join: String,
    /** regex-операции, применяемые после группы (токены после ##). */
    val regexOps: List<LegadoStep.RegexOp> = emptyList(),
)

/** Скомпилированное правило: группы выполняются последовательно, контент между ними перетекает. */
class CompiledLegadoRule(val groups: List<LegadoGroup>) {
    val isEmpty: Boolean get() = groups.isEmpty() || groups.all { it.chains.all { c -> c.steps.isEmpty() } }
}

/**
 * Легаси-формы селекторов Legado: `tag.a`, `class.foo`, `id.bar`, `text.xxx`
 * (см. LegadoParser getElementsXpath).
 */
internal fun legacySelectorToCss(rule: String): String {
    val dot = rule.indexOf('.')
    if (dot <= 0) return rule
    val prefix = rule.substring(0, dot)
    val rest = rule.substring(dot + 1)
    return when (prefix) {
        "tag" -> rest
        "class" -> "." + rest.trim().replace(' ', '.')
        "id" -> "#" + rest.trim()
        "text" -> "" // text.xxx — parent элементов, содержащих xxx; обрабатывается отдельно
        else -> rule
    }
}

/**
 * Индексные суффиксы Legado: `rule.0`, `rule.!1`, `rule[0,2]`, `rule.0:5`, `rule.-1`.
 * Возвращает селектор без суффикса и спецификацию индекса.
 */
internal data class IndexSpec(
    /** позитивные индексы (выбрать), негативные через [exclude]. */
    val include: List<Int> = emptyList(),
    val exclude: List<Int> = emptyList(),
    val slice: Triple<Int, Int, Int?>? = null, // start, end, step
) {
    val isEmpty: Boolean get() = include.isEmpty() && exclude.isEmpty() && slice == null

    fun apply(input: List<Element>): List<Element> {
        if (isEmpty || input.isEmpty()) return input
        val len = input.size
        if (slice != null) {
            val (s, e, step) = slice
            val start = if (s < 0) (len + s).coerceAtLeast(0) else s
            val end = if (e < 0) len + e + 1 else e
            return input.subList(start.coerceAtMost(len), end.coerceIn(0, len)).let {
                if (step == null || step == 1) it else it.filterIndexed { i, _ -> i % step == 0 }
            }
        }
        if (include.isNotEmpty()) {
            return include.mapNotNull { i -> input.getOrNull(if (i < 0) len + i else i) }
        }
        val excluded = exclude.map { i -> if (i < 0) len + i else i }.toSet()
        return input.filterIndexed { i, _ -> i !in excluded }
    }
}

internal fun splitSelectorIndex(rule: String): Pair<String, IndexSpec> {
    if (rule.isEmpty()) return rule to IndexSpec()
    // [...]-суффикс: индексы/исключения через запятую
    if (rule.endsWith(']')) {
        val open = rule.lastIndexOf('[')
        // Не путать с CSS-атрибутом [class=x]: внутри только цифры/запятая/!/пробелы/двоеточия
        val inner = rule.substring(open + 1, rule.length - 1)
        if (open > 0 && inner.isNotEmpty() &&
            inner.all { it in "0123456789,!-: " } && inner.any { it.isDigit() }
        ) {
            val spec = parseIndexTokens(inner) ?: return rule to IndexSpec()
            return rule.substring(0, open) to spec
        }
        return rule to IndexSpec()
    }
    // `.N` / `.!N` / `.N:M` / `.N:M:step` суффикс (последняя точка с числом)
    val lastDot = rule.lastIndexOf('.')
    if (lastDot > 0) {
        val tail = rule.substring(lastDot + 1)
        if (tail.isNotEmpty() && (tail.first().isDigit() || tail.first() == '!' || tail.first() == '-')) {
            val spec = parseIndexTokens(tail) ?: return rule to IndexSpec()
            return rule.substring(0, lastDot) to spec
        }
    }
    return rule to IndexSpec()
}

private fun parseIndexTokens(tokens: String): IndexSpec? {
    val parts = tokens.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.isEmpty()) return null
    val include = mutableListOf<Int>()
    val exclude = mutableListOf<Int>()
    var slice: Triple<Int, Int, Int?>? = null
    for (part in parts) {
        val negated = part.startsWith("!")
        val body = if (negated) part.substring(1) else part
        if (body.contains(':')) {
            val nums = body.split(':').map { if (it.isEmpty()) -1 else it.toIntOrNull() ?: return null }
            slice = when (nums.size) {
                2 -> Triple(nums[0], nums[1], null)
                3 -> Triple(nums[0], nums[1], nums[2])
                else -> return null
            }
        } else {
            val n = body.toIntOrNull() ?: return null
            if (negated) exclude += n else include += n
        }
    }
    return IndexSpec(include, exclude, slice)
}

/**
 * Парсер правил Legado → [CompiledLegadoRule].
 * Чистый Kotlin, без выполнения — безопасно вызывать на загрузке источника.
 */
object LegadoRuleParser {

    fun compile(rule: String): CompiledLegadoRule {
        if (rule.isBlank()) return CompiledLegadoRule(emptyList())
        val alternatives = splitTopLevel(rule, "||")
        val groups = mutableListOf<LegadoGroup>()
        for (alt in alternatives) {
            val (base, regexOps) = extractRegexOps(alt)
            val joinAndChains = splitJoinChains(base)
            groups += LegadoGroup(
                chains = joinAndChains.second,
                join = joinAndChains.first,
                regexOps = regexOps,
            )
        }
        return CompiledLegadoRule(groups)
    }

    /** Разрезает по разделителю, игнорируя разделители внутри [], {}, (), {{}}, @js:. */
    internal fun splitTopLevel(input: String, separator: String): List<String> {
        if (separator !in input) return listOf(input)
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var i = 0
        var depthSquare = 0
        var depthCurly = 0
        var depthParen = 0
        var inJs = false
        var inSingle = false
        var inDouble = false
        while (i < input.length) {
            if (inJs) {
                // @js: код до конца цепочки/группы — внутри могут быть ## (regex) и @
                // Но разделители &&/||/%% на верхнем уровне всё равно режут (Legado так делает
                // через токенизатор: js-токен продолжается до ## или конца).
                if (input.startsWith("##", i) || input.startsWith("###", i)) {
                    inJs = false
                    continue
                }
                sb.append(input[i]); i++; continue
            }
            val c = input[i]
            when {
                inSingle -> { sb.append(c); if (c == '\\' && i + 1 < input.length) { sb.append(input[i + 1]); i++ } else if (c == '\'') inSingle = false }
                inDouble -> { sb.append(c); if (c == '\\' && i + 1 < input.length) { sb.append(input[i + 1]); i++ } else if (c == '"') inDouble = false }
                c == '\'' -> { inSingle = true; sb.append(c) }
                c == '"' -> { inDouble = true; sb.append(c) }
                input.startsWith("@js:", i) || input.startsWith("<js>", i) -> {
                    inJs = true; sb.append(if (input.startsWith("<js>", i)) "<js>" else "@js:"); i += if (input.startsWith("<js>", i)) 4 else 4
                }
                input.startsWith("</js>", i) -> { inJs = false; sb.append("</js>"); i += 5 }
                c == '[' -> { depthSquare++; sb.append(c); i++ }
                c == ']' -> { depthSquare = (depthSquare - 1).coerceAtLeast(0); sb.append(c); i++ }
                c == '{' -> { depthCurly++; sb.append(c); i++ }
                c == '}' -> { depthCurly = (depthCurly - 1).coerceAtLeast(0); sb.append(c); i++ }
                c == '(' -> { depthParen++; sb.append(c); i++ }
                c == ')' -> { depthParen = (depthParen - 1).coerceAtLeast(0); sb.append(c); i++ }
                depthSquare == 0 && depthCurly == 0 && depthParen == 0 && input.startsWith(separator, i) -> {
                    result += sb.toString(); sb.setLength(0); i += separator.length
                }
                else -> { sb.append(c); i++ }
            }
        }
        result += sb.toString()
        return result
    }

    private fun extractRegexOps(input: String): Pair<String, List<LegadoStep.RegexOp>> {
        // Ищем верхнеуровневый ## (вне [] и кавычек)
        val idx = topLevelIndexOf(input, "##")
        if (idx < 0) {
            // Форма `:regex` (allInOne) — только если规则 целиком начинается с `:`
            return input to emptyList()
        }
        val base = input.substring(0, idx)
        var rest = input.substring(idx + 2)
        val ops = mutableListOf<LegadoStep.RegexOp>()
        // `:regex` allInOne перед ## не бывает; разбираем хвост: pattern[##replacement][##|###|####]
        val parts = mutableListOf<String>()
        var cursor = 0
        while (true) {
            val sep = topLevelIndexOfFrom(rest, "##", cursor)
            val sep3 = rest.indexOf("###", cursor)
            val sep4 = rest.indexOf("####", cursor)
            // #### приоритетнее ### приоритетнее ##
            val use4 = sep4 >= 0 && (sep < 0 || sep4 <= sep) && (sep3 < 0 || sep4 <= sep3)
            val use3 = !use4 && sep3 >= 0 && (sep < 0 || sep3 <= sep)
            val use2 = !use4 && !use3 && sep >= 0
            when {
                use4 -> { parts += rest.substring(cursor, sep4); parts += "####"; cursor = sep4 + 4 }
                use3 -> { parts += rest.substring(cursor, sep3); parts += "###"; cursor = sep3 + 3 }
                use2 -> { parts += rest.substring(cursor, sep); parts += "##"; cursor = sep + 2 }
                else -> { parts += rest.substring(cursor); break }
            }
            if (parts.size > 6) break
        }
        // parts: [pattern] | [pattern, ##, replacement] | [pattern, ##] | [pattern, ###] | [pattern, ####]
        //        | [pattern, ##, replacement, ##|###]
        val pattern = parts.getOrNull(0) ?: return base to ops
        val mode: LegadoStep.RegexOp.Mode
        val replacement: String?
        when {
            parts.size == 1 -> { mode = LegadoStep.RegexOp.Mode.REMOVE; replacement = "" }
            parts.size == 2 && parts[1] == "###" -> { mode = LegadoStep.RegexOp.Mode.EXTRACT_FIRST; replacement = null }
            parts.size == 2 && parts[1] == "####" -> { mode = LegadoStep.RegexOp.Mode.REPLACE_FIRST; replacement = "" }
            parts.size == 2 && parts[1] == "##" -> { mode = LegadoStep.RegexOp.Mode.REMOVE; replacement = "" }
            parts.size >= 3 && parts[1] == "##" -> {
                replacement = parts[2]
                mode = when {
                    parts.size >= 4 && parts[3] == "###" -> LegadoStep.RegexOp.Mode.EXTRACT_FIRST
                    parts.size >= 4 && parts[3] == "##" -> LegadoStep.RegexOp.Mode.REPLACE_FIRST
                    else -> LegadoStep.RegexOp.Mode.REPLACE
                }
            }
            else -> { mode = LegadoStep.RegexOp.Mode.REMOVE; replacement = "" }
        }
        ops += LegadoStep.RegexOp(pattern, replacement, mode)
        return base to ops
    }

    private fun splitJoinChains(input: String): Pair<String, List<LegadoChain>> {
        val separators = listOf("&&", "%%")
        var join = ""
        var parts: List<String> = listOf(input)
        for (sep in separators) {
            val split = splitTopLevel(input, sep)
            if (split.size > 1) { join = sep; parts = split; break }
        }
        // `||` обрабатывается на уровне альтернатив (compile()), здесь — если остался
        if (join.isEmpty()) {
            val split = splitTopLevel(input, "||")
            if (split.size > 1) { join = "||"; parts = split }
        }
        return join to parts.map { parseChain(it) }
    }

    internal fun parseChain(input: String): LegadoChain {
        val steps = mutableListOf<LegadoStep>()
        // @put:{...} / @get:{...} — отдельные шаги, начинают цепочку или стоят после @
        var rest = input.trim()
        if (rest.isEmpty()) return LegadoChain(emptyList())

        // Сначала вырезаем @put:{...} из любого места (Legado выполняет put до основного прохода)
        val puts = mutableMapOf<String, String>()
        while (true) {
            val putIdx = rest.indexOf("@put:{")
            if (putIdx < 0) break
            val end = findClosing(rest, putIdx + 5, '{', '}')
            if (end < 0) break
            val body = rest.substring(putIdx + 6, end)
            // body: key:rule,key2:rule2 (внутри правила могут быть запятые в [] — режем по `:` верхнего уровня)
            parsePutBody(body)?.let { puts.putAll(it) }
            rest = rest.removeRange(putIdx, end + 1)
        }
        if (puts.isNotEmpty()) steps += LegadoStep.Put(puts)

        // @get:{key} — конечный подстановочный шаг (бывает и в середине цепочки URL)
        val getIdx = rest.indexOf("@get:{")
        if (getIdx >= 0) {
            val end = findClosing(rest, getIdx + 5, '{', '}')
            if (end >= 0 && end == rest.length - 1) {
                val key = rest.substring(getIdx + 6, end)
                steps += LegadoStep.Get(key)
                return LegadoChain(steps)
            }
        }

        // Разрезаем цепочку на шаги по @ (не внутри []/()/кавычек/@js:)
        val rawSteps = splitSteps(rest)
        for ((i, raw) in rawSteps.withIndex()) {
            val step = raw.trim()
            if (step.isEmpty()) continue
            steps += parseStep(step, isLast = i == rawSteps.lastIndex)
        }
        return LegadoChain(steps)
    }

    private fun parsePutBody(body: String): Map<String, String>? {
        if (body.isBlank()) return emptyMap()
        val result = mutableMapOf<String, String>()
        // пары key:rule через запятую верхнего уровня
        var depth = 0
        val sb = StringBuilder()
        val pairs = mutableListOf<String>()
        for (c in body) {
            when (c) {
                '[', '{' -> { depth++; sb.append(c) }
                ']', '}' -> { depth--; sb.append(c) }
                ',' -> if (depth == 0) { pairs += sb.toString(); sb.setLength(0) } else sb.append(c)
                else -> sb.append(c)
            }
        }
        pairs += sb.toString()
        for (pair in pairs) {
            val colon = pair.indexOf(':')
            if (colon > 0) result[pair.substring(0, colon).trim()] = pair.substring(colon + 1).trim()
        }
        return result
    }

    /** Разрезает цепочку на шаги по `@`, не входя в []/()/кавычки; `@@` — один шаг-разделитель. */
    internal fun splitSteps(input: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var i = 0
        var depthSquare = 0
        var depthParen = 0
        var inSingle = false
        var inDouble = false
        while (i < input.length) {
            val c = input[i]
            when {
                inSingle -> { sb.append(c); if (c == '\\' && i + 1 < input.length) { sb.append(input[i + 1]); i++ } else if (c == '\'') inSingle = false; i++ }
                inDouble -> { sb.append(c); if (c == '\\' && i + 1 < input.length) { sb.append(input[i + 1]); i++ } else if (c == '"') inDouble = false; i++ }
                c == '\'' -> { inSingle = true; sb.append(c); i++ }
                c == '"' -> { inDouble = true; sb.append(c); i++ }
                input.startsWith("@js:", i) -> {
                    // js-шаг до конца или до下一个 @ не внутри js... Legado: js-код идёт до ## или конца.
                    // В цепочке `sel@js:code` js обычно последний. Берём до конца строки.
                    sb.append(input.substring(i)); i = input.length
                }
                input.startsWith("@@@", i) -> { result += sb.toString(); sb.setLength(0); i += 3 }
                input.startsWith("@@", i) -> { result += sb.toString(); sb.setLength(0); i += 2 }
                c == '[' -> { depthSquare++; sb.append(c); i++ }
                c == ']' -> { depthSquare = (depthSquare - 1).coerceAtLeast(0); sb.append(c); i++ }
                c == '(' -> { depthParen++; sb.append(c); i++ }
                c == ')' -> { depthParen = (depthParen - 1).coerceAtLeast(0); sb.append(c); i++ }
                c == '@' && depthSquare == 0 && depthParen == 0 -> {
                    // @css:/@json:/@xpath: — режим шага; сохраняем префикс в шаге,
                    // а не теряем его при разрезе (иначе @css:.list превращается в .list
                    // без указания режима — для CSS это эквивалентно, но для @json: — нет).
                    val next = input.substring(i + 1)
                    if (sb.isEmpty() && (next.startsWith("css:") || next.startsWith("json:") || next.startsWith("xpath:"))) {
                        sb.append('@'); i++
                    } else {
                        result += sb.toString(); sb.setLength(0); i++
                    }
                }
                else -> { sb.append(c); i++ }
            }
        }
        result += sb.toString()
        return result.filter { it.isNotBlank() }
    }

    private fun parseStep(step: String, isLast: Boolean): LegadoStep {
        // Префиксы-режимы
        when {
            step.startsWith("@js:") -> return LegadoStep.Js(step.substring(4))
            step.startsWith("<js>") && step.endsWith("</js>") ->
                return LegadoStep.Js(step.substring(4, step.length - 5))
            step.startsWith("@css:") -> {
                val (sel, idx) = splitSelectorIndex(step.substring(5))
                return cssOrEnd(sel, idx, isLast)
            }
            step.startsWith("@json:") -> return LegadoStep.JsonPath(step.substring(6))
            step.startsWith("@xpath:") -> return LegadoStep.XPath(step.substring(7))
        }
        // Инлайн-шаблоны: @get:{key} внутри URL/строки, {{var}}, {{$.path}}
        if (step.contains("@get:{") || (step.contains("{{") && step.contains("}}"))) {
            return LegadoStep.Template(step)
        }
        // Тип по форме
        return when {
            step.startsWith("/") || step.startsWith("(") && step.contains("::") -> LegadoStep.XPath(step)
            step.startsWith("$.") || step.startsWith("$[") -> LegadoStep.JsonPath(step)
            step.startsWith(":") && step.length > 1 && !step.startsWith("::") ->
                // `:regex` allInOne — findall по сырому контенту
                LegadoStep.RegexOp(step.substring(1), null, LegadoStep.RegexOp.Mode.FIND_ALL)
            else -> {
                val (sel, idx) = splitSelectorIndex(step)
                cssOrEnd(sel, idx, isLast)
            }
        }
    }

    /** End-правила Legado: text/textNodes/ownText/html/all/src/href/произвольный атрибут. */
    private val END_RULES = setOf("text", "textNodes", "ownText", "html", "all", "src", "href")
    private val INDEXED_END_RULES = setOf("text", "ownText")

    private fun cssOrEnd(selector: String, index: IndexSpec, isLast: Boolean): LegadoStep {
        // End-правило распознаём ТОЛЬКО как одиночное слово без CSS-синтаксиса
        if (selector.isNotEmpty() && selector !in END_RULES &&
            selector.none { it in ".#*[]>:," } && selector.first().isLowerCase() &&
            selector.first().isLetter() && selector.all { it.isLetterOrDigit() || it == '_' } &&
            isLast
        ) {
            // одиночное слово-компонент (например `div`) — это CSS-тег, НЕ end-правило,
            // кроме явного списка END_RULES
            if (selector in END_RULES || selector == "textNodes") return LegadoStep.End(selector)
            // произвольный атрибут: Legado EndRuleXpath.get() → ./@name для неизвестных имён
            // Но тег-селекторы вроде `div`/`span` должны оставаться CSS.
            // Компромисс: известные атрибуты распознаём по списку.
            if (selector in COMMON_ATTRS) return LegadoStep.End(selector)
            return LegadoStep.Css(selector).let { if (index.isEmpty) it else LegadoStep.Css(selector) }
        }
        if (selector in END_RULES) return LegadoStep.End(selector)
        if (selector.isEmpty()) return LegadoStep.Css("")
        return LegadoStep.Css(selector)
    }

    private val COMMON_ATTRS = setOf(
        "src", "href", "content", "value", "data-src", "data-original", "data-echo",
        "alt", "title", "data-srcset", "srcset", "style", "class", "id",
    )

    private fun topLevelIndexOf(input: String, sep: String): Int = topLevelIndexOfFrom(input, sep, 0)

    private fun topLevelIndexOfFrom(input: String, sep: String, from: Int): Int {
        var i = from
        var depthSquare = 0
        var depthCurly = 0
        var inSingle = false
        var inDouble = false
        while (i < input.length) {
            val c = input[i]
            when {
                inSingle -> { if (c == '\\') i++; else if (c == '\'') inSingle = false }
                inDouble -> { if (c == '\\') i++; else if (c == '"') inDouble = false }
                c == '\'' -> inSingle = true
                c == '"' -> inDouble = true
                c == '[' -> depthSquare++
                c == ']' -> depthSquare = (depthSquare - 1).coerceAtLeast(0)
                c == '{' -> depthCurly++
                c == '}' -> depthCurly = (depthCurly - 1).coerceAtLeast(0)
                depthSquare == 0 && depthCurly == 0 && input.startsWith(sep, i) -> return i
            }
            i++
        }
        return -1
    }

    private fun findClosing(s: String, from: Int, open: Char, close: Char): Int {
        var depth = 0
        for (i in from until s.length) {
            when (s[i]) {
                open -> depth++
                close -> { depth--; if (depth == 0) return i }
            }
        }
        return -1
    }
}
