package my.noveldokusha.extension_compat.legado

import com.jayway.jsonpath.JsonPath
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.mozilla.javascript.ClassShutter
import org.mozilla.javascript.Context
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import org.jsoup.helper.W3CDom

/**
 * Выполняет скомпилированные правила Legado над контентом.
 *
 * Контент между шагами — [Any]: [Document], [Element], [Elements], [List]<*> (смешанный),
 * [String], [net.minidev.json.JSONArray]/[JsonObject] и пр. — как в оригинальном
 * LegadoParser (getElements/getString работают с «чужим» content).
 *
 * Изоляция JS: Rhino с ClassShutter, запрещающим доступ к Java-классам кроме
 * белого списка (`org.jsoup.Jsoup` — нужен правилам-примерам Legado);
 * ограничение времени выполнения через instruction observer.
 */
class LegadoRuleEngine(
    private val jsTimeoutMs: Long = 8_000,
) {
    private val compiledCache = ConcurrentHashMap<String, CompiledLegadoRule>()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ── Публичный API ────────────────────────────────────────────────────────

    /** Единичный строковый результат (правило поля: имя, автор...). */
    fun getString(content: Any?, rule: String, vars: MutableMap<String, String> = mutableMapOf()): String {
        val strings = getStrings(content, rule, vars)
        return strings.filter { it.isNotBlank() }.joinToString("\n")
    }

    /** Список строк (bookList-поля целиком, обложки, жанры...). */
    fun getStrings(content: Any?, rule: String, vars: MutableMap<String, String> = mutableMapOf()): List<String> {
        val compiled = compile(rule)
        if (compiled.isEmpty) return listOfNotNull(content?.toString()?.takeIf { it.isNotBlank() })
        var current: Any? = content
        for (group in compiled.groups) {
            current = evalGroup(current, group, vars, asStrings = true)
        }
        return normalizeStrings(current)
    }

    /** Список элементов (bookList/chapterList). */
    fun getElements(content: Any?, rule: String, vars: MutableMap<String, String> = mutableMapOf()): List<Element> {
        val compiled = compile(rule)
        // Намеренно НЕ конвертируем контент в Document заранее: для JSON-ответов
        // это сломало бы JSONPath (строка JSON разобрана как HTML). Каждый шаг
        // сам выбирает формат по префиксу правила.
        var current: Any? = content
        for (group in compiled.groups) {
            current = evalGroup(current, group, vars, asStrings = false)
        }
        return normalizeElements(current)
    }

    fun compile(rule: String): CompiledLegadoRule =
        compiledCache.getOrPut(rule) { LegadoRuleParser.compile(rule) }

    // ── Выполнение группы ────────────────────────────────────────────────────

    private fun evalGroup(
        input: Any?,
        group: LegadoGroup,
        vars: MutableMap<String, String>,
        asStrings: Boolean,
    ): Any? {
        val results = mutableListOf<Any?>()
        for ((chainIdx, chain) in group.chains.withIndex()) {
            var current: Any? = when {
                group.join == "||" && results.any { !isBlankResult(it) } -> break
                group.join.isNotEmpty() && chainIdx > 0 && group.join != "||" -> input
                else -> input
            }
            for (step in chain.steps) {
                current = evalStep(current, step, vars, input)
            }
            results += current
            if (group.join == "||" && !isBlankResult(current)) break
        }
        val merged = when (group.join) {
            "%%" -> zipJoin(results)
            "||" -> results.firstOrNull { !isBlankResult(it) } ?: results.lastOrNull()
            else -> concatJoin(results)
        }
        var out = merged
        for (op in group.regexOps) {
            out = applyRegexOp(out, op)
        }
        return if (asStrings) out else out
    }

    private fun evalStep(current: Any?, step: LegadoStep, vars: MutableMap<String, String>, rawInput: Any?): Any? =
        when (step) {
            is LegadoStep.Css -> selectCss(current, step.selector)
            is LegadoStep.XPath -> selectXPath(current, step.expression)
            is LegadoStep.JsonPath -> selectJsonPath(current, step.path)
            is LegadoStep.Js -> evalJs(current, step.code, vars, rawInput)
            is LegadoStep.End -> extractEnd(current, step.name)
            is LegadoStep.RegexOp -> applyRegexOp(current, step)
            is LegadoStep.Put -> {
                for ((k, r) in step.entries) {
                    // Правило put вычисляется против исходного контента (как в Legado)
                    vars[k] = getString(rawInput, r, vars)
                }
                current
            }
            is LegadoStep.Get -> vars[step.key] ?: ""
            is LegadoStep.Template -> expandTemplate(step.raw, current, rawInput, vars)
        }

    /**
     * Подстановка в инлайн-шаблоне:
     *  - `@get:{key}` → vars[key]
     *  - `{{varName}}` → vars[varName] (простая переменная)
     *  - `{{$.jsonpath}}` / `{{$[...]}}` → JSONPath по rawInput (если это JSON)
     *  - `{{page}}`/`{{key}}` → vars
     */
    private fun expandTemplate(
        raw: String,
        current: Any?,
        rawInput: Any?,
        vars: MutableMap<String, String>,
    ): String {
        var out = raw
        // @get:{key}
        while (true) {
            val idx = out.indexOf("@get:{")
            if (idx < 0) break
            val end = out.indexOf('}', idx + 6)
            if (end < 0) break
            val key = out.substring(idx + 6, end)
            out = out.removeRange(idx, end + 1) + (vars[key] ?: "")
        }
        // {{...}}
        out = Regex("""\{\{(.+?)\}\}""").replace(out) { m ->
            val inner = m.groupValues[1].trim()
            when {
                inner.startsWith("$.") || inner.startsWith("$[") -> {
                    val jsonSource = when (rawInput) {
                        is String -> rawInput
                        else -> current?.toString() ?: ""
                    }
                    try {
                        val v: Any = JsonPath.read(jsonSource, inner)
                        v.toString()
                    } catch (e: Exception) {
                        try {
                            val v: Any = JsonPath.read(current?.toString() ?: "", inner)
                            v.toString()
                        } catch (e2: Exception) {
                            m.value
                        }
                    }
                }
                inner == "page" -> vars["page"] ?: "1"
                inner == "key" -> vars["key"] ?: ""
                inner == "baseURL" || inner == "baseUrl" -> vars["baseURL"] ?: ""
                else -> vars[inner] ?: m.value
            }
        }
        return out
    }

    // ── Селекторы ────────────────────────────────────────────────────────────

    private fun selectCss(content: Any?, selectorRaw: String): Any? {
        val (selector, index) = LegadoRuleParser.splitSelectorIndex(selectorRaw)
        val elements = when (content) {
            is Document -> if (selector.isEmpty()) Elements(content.body() ?: content) else content.select(selector)
            is Element -> if (selector.isEmpty()) Elements(content) else content.select(selector)
            is Elements -> {
                val out = Elements()
                content.forEach { el ->
                    if (selector.isEmpty()) out.add(el) else out.addAll(el.select(selector))
                }
                out
            }
            is List<*> -> {
                val out = Elements()
                content.filterIsInstance<Element>().forEach { el ->
                    if (selector.isEmpty()) out.add(el) else out.addAll(el.select(selector))
                }
                out
            }
            is String -> selectCss(ensureDocument(content), selectorRaw)
            else -> Elements()
        }
        return index.apply(elements.toList())
    }

    private fun selectXPath(content: Any?, expression: String): Any? {
        val doc = when (content) {
            is Document -> content
            is Element -> Jsoup.parse(content.outerHtml())
            is String -> Jsoup.parse(content)
            is List<*> -> content.filterIsInstance<Element>().firstOrNull()?.let { Jsoup.parse(it.outerHtml()) }
            else -> return emptyList<Any>()
        }
        val w3c = W3CDom().fromJsoup(doc)
        val xpath = XPathFactory.newInstance().newXPath()
        val nodes = try {
            xpath.evaluate(expression, w3c, XPathConstants.NODESET) as? org.w3c.dom.NodeList
        } catch (e: Exception) {
            Timber.d("Legado XPath failed: $expression — ${e.message}")
            return emptyList<Any>()
        }
        val out = mutableListOf<Any>()
        for (i in 0 until (nodes?.length ?: 0)) {
            val node = nodes.item(i)
            when (node) {
                is org.w3c.dom.Element -> w3cToJsoup(node)?.let { out += it }
                is org.w3c.dom.Attr -> out += node.value
                else -> out += node.textContent ?: ""
            }
        }
        return out
    }

    private fun w3cToJsoup(node: org.w3c.dom.Element): Element? = try {
        val html = StringBuilder()
        serializeW3c(node, html)
        Jsoup.parseBodyFragment(html.toString()).body()?.children()?.firstOrNull()
            ?: Jsoup.parseBodyFragment(html.toString()).body()
    } catch (e: Exception) {
        null
    }

    private fun serializeW3c(node: org.w3c.dom.Node, out: StringBuilder) {
        when (node) {
            is org.w3c.dom.Element -> {
                out.append('<').append(node.nodeName)
                val attrs = node.attributes
                for (i in 0 until attrs.length) {
                    val a = attrs.item(i)
                    out.append(' ').append(a.nodeName).append("=\"").append(a.nodeValue).append('"')
                }
                out.append('>')
                var child = node.firstChild
                while (child != null) { serializeW3c(child, out); child = child.nextSibling }
                out.append("</").append(node.nodeName).append('>')
            }
            is org.w3c.dom.Text -> out.append(node.nodeValue)
            else -> out.append(node.textContent ?: "")
        }
    }

    private fun selectJsonPath(content: Any?, path: String): Any? {
        val jsonString = when (content) {
            is String -> content
            is List<*> -> content.filterIsInstance<String>().firstOrNull() ?: return emptyList<Any>()
            is Element -> content.html()
            is Document -> content.html()
            else -> content?.toString() ?: return emptyList<Any>()
        }
        return try {
            val value: Any = JsonPath.read(jsonString, path)
            value
        } catch (e: Exception) {
            // Иногда правило применяется к объекту уже внутри списка
            try {
                val arr = JsonPath.read<Any>(jsonString, "$")
                JsonPath.read<Any>(jsonToString(arr), path)
            } catch (e2: Exception) {
                emptyList<Any>()
            }
        }
    }

    private fun jsonToString(value: Any?): String = when (value) {
        null -> ""
        is String -> value
        else -> value.toString().let {
            // JSONObject/JSONArray toString валиден для json-path
            it
        }
    }

    // ── End-правила ──────────────────────────────────────────────────────────

    private fun extractEnd(content: Any?, name: String): Any? {
        val elements = normalizeElements(content)
        if (elements.isEmpty()) {
            // content может быть строкой — для html/text вернуть как есть
            return when (content) {
                is String -> if (name == "html" || name == "all") listOf(content) else listOf(content)
                else -> emptyList<String>()
            }
        }
        return when (name) {
            "text" -> elements.map { it.wholeText().replace(Regex("\\s+"), " ").trim() }
            "textNodes" -> elements.map { el ->
                el.childNodes().filterIsInstance<org.jsoup.nodes.TextNode>()
                    .joinToString("\n") { it.text().trim() }.trim()
            }
            "ownText" -> elements.map { it.ownText().trim() }
            "html" -> elements.map { el ->
                val clone = el.clone()
                clone.select("script, style").remove()
                clone.html().replace(Regex("<br>"), "\n<br>")
            }
            "all" -> elements.map { it.outerHtml() }
            else -> elements.map { el -> el.attr(name) }.filter { it.isNotEmpty() }
        }
    }

    // ── Regex ────────────────────────────────────────────────────────────────

    private fun applyRegexOp(content: Any?, op: LegadoStep.RegexOp): Any? {
        val pattern = try {
            Regex(op.pattern)
        } catch (e: Exception) {
            Timber.d("Legado regex compile failed: ${op.pattern}")
            return content
        }
        val strings = normalizeStrings(content)
        return when (op.mode) {
            LegadoStep.RegexOp.Mode.FIND_ALL -> strings.flatMap { s -> pattern.findAll(s).map { it.value } }
            LegadoStep.RegexOp.Mode.REMOVE -> strings.map { pattern.replace(it, "") }
            LegadoStep.RegexOp.Mode.REPLACE -> strings.map { pattern.replace(it, op.replacement ?: "") }
            LegadoStep.RegexOp.Mode.REPLACE_FIRST -> strings.map {
                pattern.replaceFirst(it, op.replacement ?: "")
            }
            LegadoStep.RegexOp.Mode.EXTRACT_FIRST -> strings.mapNotNull { s ->
                val m = pattern.find(s) ?: return@mapNotNull null
                expandGroups(op.replacement ?: "\$0", m)
            }
        }
    }

    /** `$1`-референсы в replacement → значения групп (Legado: только неэкранированные). */
    private fun expandGroups(template: String, match: MatchResult): String {
        val sb = StringBuilder()
        var i = 0
        while (i < template.length) {
            val c = template[i]
            if (c == '\\' && i + 1 < template.length) {
                sb.append(template[i + 1]); i += 2; continue
            }
            if (c == '$' && i + 1 < template.length && template[i + 1].isDigit()) {
                var j = i + 1
                while (j < template.length && template[j].isDigit()) j++
                val groupIdx = template.substring(i + 1, j).toIntOrNull()
                if (groupIdx != null) {
                    sb.append(match.groupValues.getOrNull(groupIdx) ?: "")
                    i = j; continue
                }
            }
            sb.append(c); i++
        }
        return sb.toString()
    }

    // ── JavaScript (Rhino) ───────────────────────────────────────────────────

    /**
     * ContextFactory с instruction-observer: превышение wall-clock бюджета
     * прерывает выполнение правила (защита от вечных циклов в @js).
     */
    private inner class TimeoutContextFactory : org.mozilla.javascript.ContextFactory() {
        @Volatile var deadline: Long = 0

        override fun makeContext(): Context {
            val cx = super.makeContext()
            cx.optimizationLevel = -1
            cx.languageVersion = Context.VERSION_ES6
            cx.setClassShutter(LegadoClassShutter)
            cx.instructionObserverThreshold = 100_000
            return cx
        }

        override fun observeInstructionCount(cx: Context, instructionCount: Int) {
            if (System.currentTimeMillis() > deadline) {
                throw IllegalStateException("JS rule timed out after ${jsTimeoutMs}ms")
            }
        }
    }

    private val jsFactory by lazy { TimeoutContextFactory() }

    private fun evalJs(current: Any?, code: String, vars: MutableMap<String, String>, rawInput: Any?): Any? {
        return try {
            jsFactory.call { cx ->
                val scope = cx.initStandardObjects()
                val resultValue = when (current) {
                    is String -> current
                    is List<*> -> current.joinToString("") { it?.toString() ?: "" }
                    is Element -> current.outerHtml()
                    is Document -> current.outerHtml()
                    else -> current?.toString() ?: ""
                }
                ScriptableObject.putProperty(scope, "result", Context.javaToJS(resultValue, scope))
                ScriptableObject.putProperty(scope, "key", Context.javaToJS(vars["key"] ?: "", scope))
                ScriptableObject.putProperty(scope, "page", Context.javaToJS(vars["page"] ?: "1", scope))
                ScriptableObject.putProperty(scope, "baseURL", Context.javaToJS(vars["baseURL"] ?: "", scope))
                ScriptableObject.putProperty(scope, "source", Context.javaToJS(vars["source"] ?: "", scope))
                for ((k, v) in vars) {
                    if (k !in setOf("key", "page", "baseURL", "source")) {
                        ScriptableObject.putProperty(scope, k, Context.javaToJS(v, scope))
                    }
                }
                jsFactory.deadline = System.currentTimeMillis() + jsTimeoutMs
                val jsResult = cx.evaluateString(scope, code, "legado_rule", 1, null)
                when {
                    jsResult == null || jsResult is Undefined || jsResult == Context.getUndefinedValue() -> current
                    jsResult is String -> jsResult
                    jsResult is Number -> jsResult.toString()
                    jsResult is Boolean -> jsResult.toString()
                    jsResult is org.mozilla.javascript.Scriptable -> {
                        val len = try {
                            ScriptableObject.getProperty(jsResult, "length") as? Number
                        } catch (e: Exception) { null }
                        if (len != null) {
                            (0 until len.toInt()).mapNotNull { i ->
                                ScriptableObject.getProperty(jsResult, i)?.toString()?.takeIf { s ->
                                    s != "undefined" && s.isNotEmpty()
                                }
                            }.ifEmpty { listOf(jsResult.toString()) }
                        } else listOf(jsResult.toString())
                    }
                    else -> jsResult.toString()
                }
            }
        } catch (e: Throwable) {
            Timber.d("Legado @js failed: ${e.message}")
            current
        }
    }

    // ── Join ─────────────────────────────────────────────────────────────────

    private fun concatJoin(results: List<Any?>): Any? {
        val nonNull = results.filterNotNull()
        return when {
            nonNull.isEmpty() -> null
            nonNull.size == 1 -> nonNull[0]
            else -> {
                // Конкатенация: строки склеиваем, элементы — плоский список
                val allStrings = nonNull.all { it is String || it is List<*> && it.all { e -> e is String } }
                if (allStrings) {
                    nonNull.flatMap { normalizeStrings(it) }
                } else {
                    nonNull.flatMap { r -> when (r) {
                        is List<*> -> r.filterNotNull()
                        else -> listOf(r)
                    } }
                }
            }
        }
    }

    private fun zipJoin(results: List<Any?>): Any? {
        val lists = results.map { normalizeMixedList(it) }
        if (lists.isEmpty()) return null
        val maxLen = lists.maxOf { it.size }
        val out = mutableListOf<Any?>()
        for (i in 0 until maxLen) {
            for (list in lists) list.getOrNull(i)?.let { out += it }
        }
        return out
    }

    // ── Нормализация ─────────────────────────────────────────────────────────

    fun normalizeStrings(content: Any?): List<String> = when (content) {
        null -> emptyList()
        is String -> listOf(content)
        is Element -> listOf(content.wholeText())
        is Document -> listOf(content.wholeText())
        is Elements -> content.map { it.wholeText() }
        is List<*> -> content.flatMap { normalizeStrings(it) }
        is Number, is Boolean -> listOf(content.toString())
        else -> {
            // json-path значения
            if (content is Iterable<*>) content.flatMap { normalizeStrings(it) }
            else listOf(content.toString())
        }
    }.filter { it.isNotEmpty() }

    fun normalizeElements(content: Any?): List<Element> = when (content) {
        null -> emptyList()
        is Document -> listOf(content)
        is Element -> listOf(content)
        is Elements -> content.toList()
        is List<*> -> content.flatMap { normalizeElements(it) }
        is String -> normalizeElements(ensureDocument(content))
        else -> emptyList()
    }

    private fun normalizeMixedList(content: Any?): List<Any?> = when (content) {
        null -> emptyList()
        is List<*> -> content.filterNotNull()
        else -> listOf(content)
    }

    private fun isBlankResult(content: Any?): Boolean = when (content) {
        null -> true
        is String -> content.isBlank()
        is List<*> -> content.isEmpty() || content.all { it is String && it.isBlank() }
        else -> false
    }

    private fun ensureDocument(content: Any?): Document = when (content) {
        is Document -> content
        is Element -> Jsoup.parse(content.outerHtml())
        is String -> Jsoup.parse(content)
        is List<*> -> content.filterIsInstance<Element>().firstOrNull()
            ?.let { Jsoup.parse(it.outerHtml()) } ?: Document("")
        is Elements -> content.firstOrNull()?.let { Jsoup.parse(it.outerHtml()) } ?: Document("")
        else -> Document("")
    }

    companion object {
        /**
         * Запрет произвольного доступа к Java из правил @js.
         * Legado-правила исторически используют org.jsoup.Jsoup — оставляем его
         * и безобидные утилиты; всё остальное блокируем.
         */
        private object LegadoClassShutter : ClassShutter {
            private val allowed = setOf(
                "org.jsoup.Jsoup",
                "org.jsoup.nodes.Document",
                "org.jsoup.nodes.Element",
            )
            override fun visibleToScripts(fullClassName: String?): Boolean {
                if (fullClassName == null) return false
                if (fullClassName in allowed) return true
                // java.lang primitives/strings helpers
                if (fullClassName.startsWith("java.lang.")) {
                    val simple = fullClassName.removePrefix("java.lang.")
                    return simple in setOf("String", "Integer", "Long", "Double", "Float", "Boolean", "Math", "Object")
                }
                return false
            }
        }
    }
}
