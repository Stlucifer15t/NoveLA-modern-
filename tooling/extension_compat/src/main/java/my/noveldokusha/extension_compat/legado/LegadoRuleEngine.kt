package my.noveldokusha.extension_compat.legado

import com.jayway.jsonpath.JsonPath
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import org.jsoup.helper.W3CDom
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

/**
 * The Legado rule engine.
 *
 * Supports:
 *  - CSS chains with end rules (`@text`, `@href`, `@html`, ...)
 *  - index suffixes (`.0`, `.-1`, `.!0`, `[0,2]`, `.0:5`)
 *  - regex operations (`##pattern`, `##pattern##replacement`, `###`, `####`)
 *  - alternatives `||`, concatenation `&&`, zip `%%`
 *  - legacy selectors (`id.x`, `class.x`, `tag.x`)
 *  - XPath (`//...`, `@xpath:`)
 *  - JSONPath (`$.`, `$[`, `@json:`)
 *  - JavaScript (`@js:`) with a timeout and a ClassShutter
 *  - `@get:{key}` and `{{var}}` templates
 */
class LegadoRuleEngine(
    private val jsTimeoutMs: Long = 8_000,
) {
    private val compiledCache = ConcurrentHashMap<String, CompiledRule>()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ── Public API ──────────────────────────────────────────────────────────────

    fun getString(content: Any?, rule: String, vars: MutableMap<String, String> = mutableMapOf()): String =
        getStrings(content, rule, vars).filter { it.isNotBlank() }.joinToString("\n")

    fun getStrings(content: Any?, rule: String, vars: MutableMap<String, String> = mutableMapOf()): List<String> {
        val compiled = compile(rule)
        if (compiled.alternatives.isEmpty()) {
            return listOfNotNull(content?.toString()?.takeIf { it.isNotBlank() })
        }
        for (alt in compiled.alternatives) {
            val result = evalAlternative(content, alt, vars)
            val strings = normalizeStrings(result)
            if (strings.any { it.isNotBlank() }) return strings
        }
        return emptyList()
    }

    fun getElements(content: Any?, rule: String, vars: MutableMap<String, String> = mutableMapOf()): List<Element> {
        val compiled = compile(rule)
        for (alt in compiled.alternatives) {
            val result = evalAlternative(content, alt, vars)
            val elements = normalizeElements(result)
            if (elements.isNotEmpty()) return elements
        }
        return emptyList()
    }

    fun compile(rule: String): CompiledRule = compiledCache.getOrPut(rule) { parseRule(rule) }

    // ── Compilation ────────────────────────────────────────────────────────────

    private fun parseRule(rule: String): CompiledRule {
        if (rule.isBlank()) return CompiledRule(emptyList())
        val alternatives = splitTopLevel(rule, "||")
        return CompiledRule(alternatives.map { parseAlternative(it) })
    }

    private fun parseAlternative(input: String): Alternative {
        val (base, regexOps) = extractRegexOps(input)
        val (join, parts) = splitJoinChains(base)
        return Alternative(
            chains = parts.map { parseChain(it) },
            join = join,
            regexOps = regexOps,
        )
    }

    private fun splitJoinChains(input: String): Pair<String, List<String>> {
        for (sep in listOf("&&", "%%")) {
            val split = splitTopLevel(input, sep)
            if (split.size > 1) return sep to split
        }
        return "" to listOf(input)
    }

    private fun parseChain(input: String): Chain =
        Chain(splitSteps(input).mapNotNull { parseStep(it) })

    private fun parseStep(raw: String): Step? {
        val step = raw.trim()
        if (step.isEmpty()) return null
        return when {
            step.startsWith("@js:") -> Step.Js(step.substring(4))
            step.startsWith("@css:") -> Step.Css(step.substring(5))
            step.startsWith("@json:") -> Step.JsonPath(step.substring(6))
            step.startsWith("@xpath:") -> Step.XPath(step.substring(7))
            step.contains("@get:{") || (step.contains("{{") && step.contains("}}")) -> Step.Template(step)
            step in END_RULES -> Step.End(step)
            step.startsWith("//") || (step.startsWith("(") && step.contains("::")) -> Step.XPath(step)
            step.startsWith("$.") || step.startsWith("$[") -> Step.JsonPath(step)
            else -> Step.Css(step)
        }
    }

    // ── Execution ───────────────────────────────────────────────────────────────

    private fun evalAlternative(input: Any?, alt: Alternative, vars: MutableMap<String, String>): Any? {
        val results = alt.chains.map { chain -> evalChain(input, chain, vars) }
        var out: Any? = when (alt.join) {
            "&&" -> concatJoin(results)
            "%%" -> zipJoin(results)
            else -> results.firstOrNull()
        }
        for (op in alt.regexOps) {
            out = applyRegexOp(out, op)
        }
        return out
    }

    private fun evalChain(input: Any?, chain: Chain, vars: MutableMap<String, String>): Any? {
        var current: Any? = input
        for (step in chain.steps) {
            current = evalStep(current, step, vars)
        }
        return current
    }

    private fun evalStep(current: Any?, step: Step, vars: MutableMap<String, String>): Any? =
        when (step) {
            is Step.Css -> selectCssWithIndex(current, step.selector)
            is Step.XPath -> selectXPath(current, step.expression)
            is Step.JsonPath -> selectJsonPath(current, step.path)
            is Step.Js -> evalJs(current, step.code, vars)
            is Step.End -> extractEnd(current, step.name)
            is Step.Template -> expandTemplate(step.raw, vars)
        }

    // ── CSS ──────────────────────────────────────────────────────────────────

    private fun selectCssWithIndex(content: Any?, selectorRaw: String): Any? {
        val parts = selectorRaw.split(" ").filter { it.isNotBlank() }
        if (parts.none { hasIndexSuffix(it) }) {
            return selectCss(content, selectorRaw)
        }
        var current: Any? = content
        for (part in parts) {
            val (sel, idx) = splitSelectorIndex(part)
            val selected = selectCss(current, sel)
            current = idx.apply(normalizeElements(selected))
        }
        return current
    }

    private fun selectCss(content: Any?, selectorRaw: String): Any? {
        val selector = legacySelectorToCss(selectorRaw)
        return when (content) {
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
    }

    private fun legacySelectorToCss(rule: String): String {
        val dot = rule.indexOf('.')
        if (dot <= 0) return rule
        val prefix = rule.substring(0, dot)
        val rest = rule.substring(dot + 1)
        return when (prefix) {
            "tag" -> rest
            "class" -> "." + rest.trim().replace(' ', '.')
            "id" -> "#" + rest.trim()
            else -> rule
        }
    }

    private fun hasIndexSuffix(part: String): Boolean {
        if (part.endsWith(']')) {
            val open = part.lastIndexOf('[')
            if (open > 0) {
                val inner = part.substring(open + 1, part.length - 1)
                if (inner.isNotEmpty() && inner.all { it in "0123456789,!-: " } && inner.any { it.isDigit() }) return true
            }
        }
        val lastDot = part.lastIndexOf('.')
        if (lastDot > 0) {
            val tail = part.substring(lastDot + 1)
            if (tail.isNotEmpty() && (tail.first().isDigit() || tail.first() == '!' || tail.first() == '-')) return true
        }
        return false
    }

    private fun splitSelectorIndex(rule: String): Pair<String, IndexSpec> {
        if (rule.isEmpty()) return rule to IndexSpec()
        if (rule.endsWith(']')) {
            val open = rule.lastIndexOf('[')
            val inner = rule.substring(open + 1, rule.length - 1)
            if (open > 0 && inner.isNotEmpty() &&
                inner.all { it in "0123456789,!-: " } && inner.any { it.isDigit() }
            ) {
                val spec = parseIndexTokens(inner) ?: return rule to IndexSpec()
                return rule.substring(0, open) to spec
            }
            return rule to IndexSpec()
        }
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

    // ── XPath ────────────────────────────────────────────────────────────────

    private fun selectXPath(content: Any?, expression: String): Any? {
        val doc = when (content) {
            is Document -> content
            is Element -> Jsoup.parse(content.outerHtml())
            is String -> Jsoup.parse(content)
            is List<*> -> content.filterIsInstance<Element>().firstOrNull()
                ?.let { Jsoup.parse(it.outerHtml()) }

            else -> null
        } ?: return emptyList<Any>()
        val w3c = stripXhtmlNamespace(W3CDom().fromJsoup(doc))
        val xpath = XPathFactory.newInstance().newXPath()
        val nodes = try {
            xpath.evaluate(expression, w3c, XPathConstants.NODESET) as? org.w3c.dom.NodeList
        } catch (e: Exception) {
            Timber.d("Legado XPath failed: $expression — ${e.message}")
            return emptyList<Any>()
        } ?: return emptyList<Any>()
        val out = ArrayList<Any>(nodes.length)
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            when (node) {
                is org.w3c.dom.Element -> w3cToJsoup(node)?.let { out.add(it) }
                is org.w3c.dom.Attr -> out.add(node.value)
                else -> out.add(node.textContent ?: "")
            }
        }
        return out
    }

    /**
     * `W3CDom.fromJsoup` puts every element in the XHTML namespace: the root gets
     * `xmlns="http://www.w3.org/1999/xhtml"` and the rest inherit it. XPath 1.0 matches
     * an unqualified step only against the empty namespace, so `//ul/li` finds nothing in
     * such a document — and that is the shape of most `//…` rules in the wild.
     *
     * Renaming every element into the null namespace is what makes them work. Probing
     * jsoup 1.23.1 showed the alternatives are worse: `renameNode` on a prefixed name is
     * a no-op here (names carry no prefix, the namespace is inherited), and re-parsing
     * with `Parser.xmlParser()` breaks on void elements such as `<img>` without a slash.
     */
    private fun stripXhtmlNamespace(doc: org.w3c.dom.Document): org.w3c.dom.Document {
        fun rename(node: org.w3c.dom.Node?) {
            if (node !is org.w3c.dom.Element) return
            val ns = node.namespaceURI ?: return
            runCatching { doc.renameNode(node, null, node.localName ?: node.nodeName) }
                .onFailure { Timber.d("Legado XPath: cannot drop namespace on <%s> — %s", node.nodeName, it.message) }
        }
        rename(doc.documentElement)
        val nodes = doc.getElementsByTagName("*")
        for (i in 0 until nodes.length) rename(nodes.item(i))
        return doc
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
                while (child != null) {
                    serializeW3c(child, out)
                    child = child.nextSibling
                }
                out.append("</").append(node.nodeName).append('>')
            }
            is org.w3c.dom.Text -> out.append(node.nodeValue)
            else -> out.append(node.textContent ?: "")
        }
    }

    // ── JSONPath ─────────────────────────────────────────────────────────────

    private fun selectJsonPath(content: Any?, path: String): Any? {
        val jsonString = when (content) {
            is String -> content
            is List<*> -> content.filterIsInstance<String>().firstOrNull() ?: return emptyList<Any>()
            is Element -> content.html()
            is Document -> content.html()
            else -> content?.toString() ?: return emptyList<Any>()
        }
        return try {
            JsonPath.read<Any>(jsonString, path)
        } catch (e: Exception) {
            emptyList<Any>()
        }
    }

    // ── End rules ───────────────────────────────────────────────────────────────

    private fun extractEnd(content: Any?, name: String): Any? {
        val elements = normalizeElements(content)
        if (elements.isEmpty()) {
            return when (content) {
                is String -> listOf(content)
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
                clone.html()
            }
            "all" -> elements.map { it.outerHtml() }
            else -> elements.map { el -> el.attr(name) }.filter { it.isNotEmpty() }
        }
    }

    // ── Regex ────────────────────────────────────────────────────────────────

    private fun extractRegexOps(input: String): Pair<String, List<RegexOp>> {
        val idx = topLevelIndexOf(input, "##")
        if (idx < 0) return input to emptyList()
        val base = input.substring(0, idx)
        var spec = input.substring(idx + 2)
        var mode = RegexMode.REPLACE
        when {
            spec.endsWith("####") -> {
                mode = RegexMode.REPLACE_FIRST
                spec = spec.dropLast(4)
            }
            spec.endsWith("###") -> {
                mode = RegexMode.EXTRACT_FIRST
                spec = spec.dropLast(3)
            }
        }
        val parts = splitTopLevel(spec, "##")
        val pattern = parts.getOrNull(0) ?: ""
        val replacement = parts.getOrNull(1)
        return base to listOf(RegexOp(pattern, replacement, mode))
    }

    private fun applyRegexOp(content: Any?, op: RegexOp): Any? {
        val pattern = try {
            Regex(op.pattern)
        } catch (e: Exception) {
            Timber.d("Legado regex compile failed: ${op.pattern}")
            return content
        }
        val strings = normalizeStrings(content)
        return when (op.mode) {
            RegexMode.REPLACE -> strings.map { pattern.replace(it, op.replacement ?: "") }
            RegexMode.REPLACE_FIRST -> strings.map { pattern.replaceFirst(it, op.replacement ?: "") }
            RegexMode.EXTRACT_FIRST -> strings.mapNotNull { s ->
                val m = pattern.find(s) ?: return@mapNotNull null
                expandGroups(op.replacement ?: "\$0", m)
            }
            RegexMode.REMOVE -> strings.map { pattern.replace(it, "") }
        }
    }

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

    private inner class TimeoutContextFactory : org.mozilla.javascript.ContextFactory() {
        @Volatile
        var deadline: Long = 0

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

    private fun evalJs(current: Any?, code: String, vars: MutableMap<String, String>): Any? {
        return try {
            val cx = jsFactory.enterContext()
            try {
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
                jsFactory.deadline = System.currentTimeMillis() + jsTimeoutMs
                val jsResult = cx.evaluateString(scope, code, "legado_rule", 1, null)
                when {
                    jsResult == null || jsResult is Undefined -> current
                    jsResult is String -> jsResult
                    jsResult is Number -> jsResult.toString()
                    jsResult is Boolean -> jsResult.toString()
                    else -> jsResult.toString()
                }
            } finally {
                Context.exit()
            }
        } catch (e: Throwable) {
            Timber.d("Legado @js failed: ${e.message}")
            current
        }
    }

    // ── Templates ───────────────────────────────────────────────────────────────

    private fun expandTemplate(raw: String, vars: MutableMap<String, String>): String {
        var out = raw
        while (true) {
            val idx = out.indexOf("@get:{")
            if (idx < 0) break
            val end = out.indexOf('}', idx + 6)
            if (end < 0) break
            val key = out.substring(idx + 6, end)
            out = out.removeRange(idx, end + 1) + (vars[key] ?: "")
        }
        out = Regex("""\{\{(.+?)\}\}""").replace(out) { m ->
            val inner = m.groupValues[1].trim()
            when {
                inner == "page" -> vars["page"] ?: "1"
                inner == "key" -> vars["key"] ?: ""
                inner == "baseURL" || inner == "baseUrl" -> vars["baseURL"] ?: ""
                else -> vars[inner] ?: m.value
            }
        }
        return out
    }

    // ── Join ─────────────────────────────────────────────────────────────────

    private fun concatJoin(results: List<Any?>): Any? {
        val nonNull = results.filterNotNull()
        return when {
            nonNull.isEmpty() -> null
            nonNull.size == 1 -> nonNull[0]
            else -> {
                val allStrings = nonNull.all { it is String || (it is List<*> && it.all { e -> e is String }) }
                if (allStrings) {
                    nonNull.flatMap { normalizeStrings(it) }
                } else {
                    nonNull.flatMap { r ->
                        when (r) {
                            is List<*> -> r.filterNotNull()
                            else -> listOf(r)
                        }
                    }
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

    private fun normalizeMixedList(content: Any?): List<Any?> = when (content) {
        null -> emptyList()
        is List<*> -> content.filterNotNull()
        else -> listOf(content)
    }

    // ── Normalization ───────────────────────────────────────────────────────────

    fun normalizeStrings(content: Any?): List<String> = when (content) {
        null -> emptyList()
        is String -> listOf(content)
        is Element -> listOf(content.wholeText())
        is Document -> listOf(content.wholeText())
        is Elements -> content.map { it.wholeText() }
        is List<*> -> content.flatMap { normalizeStrings(it) }
        is Number, is Boolean -> listOf(content.toString())
        else -> {
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

    private fun ensureDocument(content: Any?): Document = when (content) {
        is Document -> content
        is Element -> Jsoup.parse(content.outerHtml())
        is String -> Jsoup.parse(content)
        is List<*> -> content.filterIsInstance<Element>().firstOrNull()
            ?.let { Jsoup.parse(it.outerHtml()) } ?: Document("")
        is Elements -> content.firstOrNull()?.let { Jsoup.parse(it.outerHtml()) } ?: Document("")
        else -> Document("")
    }

    // ── Top-level parsing ───────────────────────────────────────────────────────

    private fun splitTopLevel(input: String, separator: String): List<String> {
        if (separator !in input) return listOf(input)
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var i = 0
        var depthSquare = 0
        var depthCurly = 0
        var depthParen = 0
        var inSingle = false
        var inDouble = false
        while (i < input.length) {
            val c = input[i]
            when {
                inSingle -> {
                    sb.append(c)
                    if (c == '\\' && i + 1 < input.length) { sb.append(input[i + 1]); i++ } else if (c == '\'') inSingle = false
                    i++
                }
                inDouble -> {
                    sb.append(c)
                    if (c == '\\' && i + 1 < input.length) { sb.append(input[i + 1]); i++ } else if (c == '"') inDouble = false
                    i++
                }
                c == '\'' -> { inSingle = true; sb.append(c); i++ }
                c == '"' -> { inDouble = true; sb.append(c); i++ }
                c == '[' -> { depthSquare++; sb.append(c); i++ }
                c == ']' -> { depthSquare = (depthSquare - 1).coerceAtLeast(0); sb.append(c); i++ }
                c == '{' -> { depthCurly++; sb.append(c); i++ }
                c == '}' -> { depthCurly = (depthCurly - 1).coerceAtLeast(0); sb.append(c); i++ }
                c == '(' -> { depthParen++; sb.append(c); i++ }
                c == ')' -> { depthParen = (depthParen - 1).coerceAtLeast(0); sb.append(c); i++ }
                depthSquare == 0 && depthCurly == 0 && depthParen == 0 && input.startsWith(separator, i) -> {
                    result += sb.toString()
                    sb.setLength(0)
                    i += separator.length
                }
                else -> { sb.append(c); i++ }
            }
        }
        result += sb.toString()
        return result
    }

    private fun splitSteps(input: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var i = 0
        var depthSquare = 0
        var depthParen = 0
        var depthCurly = 0
        var inSingle = false
        var inDouble = false
        while (i < input.length) {
            val c = input[i]
            when {
                inSingle -> {
                    sb.append(c)
                    if (c == '\\' && i + 1 < input.length) { sb.append(input[i + 1]); i++ } else if (c == '\'') inSingle = false
                    i++
                }
                inDouble -> {
                    sb.append(c)
                    if (c == '\\' && i + 1 < input.length) { sb.append(input[i + 1]); i++ } else if (c == '"') inDouble = false
                    i++
                }
                c == '\'' -> { inSingle = true; sb.append(c); i++ }
                c == '"' -> { inDouble = true; sb.append(c); i++ }
                input.startsWith("@js:", i) -> {
                    if (sb.isNotEmpty()) {
                        result += sb.toString()
                        sb.setLength(0)
                    }
                    sb.append(input.substring(i))
                    i = input.length
                }
                input.startsWith("@get:{", i) -> {
                    val end = findClosing(input, i + 5, '{', '}')
                    if (end >= 0) {
                        sb.append(input.substring(i, end + 1))
                        i = end + 1
                    } else {
                        sb.append(c); i++
                    }
                }
                input.startsWith("@css:", i) || input.startsWith("@json:", i) || input.startsWith("@xpath:", i) -> {
                    if (sb.isNotEmpty()) {
                        result += sb.toString()
                        sb.setLength(0)
                    }
                    sb.append('@')
                    i++
                }
                c == '[' -> { depthSquare++; sb.append(c); i++ }
                c == ']' -> { depthSquare = (depthSquare - 1).coerceAtLeast(0); sb.append(c); i++ }
                c == '(' -> { depthParen++; sb.append(c); i++ }
                c == ')' -> { depthParen = (depthParen - 1).coerceAtLeast(0); sb.append(c); i++ }
                c == '{' -> { depthCurly++; sb.append(c); i++ }
                c == '}' -> { depthCurly = (depthCurly - 1).coerceAtLeast(0); sb.append(c); i++ }
                c == '@' && depthSquare == 0 && depthParen == 0 && depthCurly == 0 -> {
                    result += sb.toString()
                    sb.setLength(0)
                    i++
                }
                else -> { sb.append(c); i++ }
            }
        }
        result += sb.toString()
        return result.filter { it.isNotBlank() }
    }

    private fun topLevelIndexOf(input: String, sep: String): Int {
        var i = 0
        var depthSquare = 0
        var depthCurly = 0
        var inSingle = false
        var inDouble = false
        while (i < input.length) {
            val c = input[i]
            when {
                inSingle -> { if (c == '\\') i++ else if (c == '\'') inSingle = false }
                inDouble -> { if (c == '\\') i++ else if (c == '"') inDouble = false }
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
                close -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return -1
    }

    // ── Models ──────────────────────────────────────────────────────────────────

    data class CompiledRule(val alternatives: List<Alternative>)

    data class Alternative(
        val chains: List<Chain>,
        val join: String,
        val regexOps: List<RegexOp>,
    )

    data class Chain(val steps: List<Step>)

    sealed interface Step {
        data class Css(val selector: String) : Step
        data class XPath(val expression: String) : Step
        data class JsonPath(val path: String) : Step
        data class Js(val code: String) : Step
        data class End(val name: String) : Step
        data class Template(val raw: String) : Step
    }

    data class RegexOp(
        val pattern: String,
        val replacement: String?,
        val mode: RegexMode,
    )

    enum class RegexMode { REPLACE, REPLACE_FIRST, EXTRACT_FIRST, REMOVE }

    data class IndexSpec(
        val include: List<Int> = emptyList(),
        val exclude: List<Int> = emptyList(),
        val slice: Triple<Int, Int, Int?>? = null,
    ) {
        val isEmpty: Boolean get() = include.isEmpty() && exclude.isEmpty() && slice == null

        fun apply(input: List<Element>): List<Element> {
            if (isEmpty || input.isEmpty()) return input
            val len = input.size
            if (slice != null) {
                val (s, e, step) = slice
                val start = if (s < 0) (len + s).coerceAtLeast(0) else s
                val end = if (e < 0) len + e + 1 else e
                val sub = input.subList(start.coerceAtMost(len), end.coerceIn(0, len))
                return if (step == null || step == 1) sub else sub.filterIndexed { i, _ -> i % step == 0 }
            }
            if (include.isNotEmpty()) {
                return include.mapNotNull { i -> input.getOrNull(if (i < 0) len + i else i) }
            }
            val excluded = exclude.map { i -> if (i < 0) len + i else i }.toSet()
            return input.filterIndexed { i, _ -> i !in excluded }
        }
    }

    companion object {
        private val END_RULES = setOf(
            "text", "textNodes", "ownText", "html", "all",
            "href", "src", "content", "value", "title", "alt",
            "data-src", "data-original", "data-echo", "srcset", "data-srcset",
        )

        private object LegadoClassShutter : ClassShutter {
            private val allowed = setOf(
                "org.jsoup.Jsoup",
                "org.jsoup.nodes.Document",
                "org.jsoup.nodes.Element",
            )

            override fun visibleToScripts(fullClassName: String?): Boolean {
                if (fullClassName == null) return false
                if (fullClassName in allowed) return true
                if (fullClassName.startsWith("java.lang.")) {
                    val simple = fullClassName.removePrefix("java.lang.")
                    return simple in setOf("String", "Integer", "Long", "Double", "Float", "Boolean", "Math", "Object")
                }
                return false
            }
        }
    }
}
