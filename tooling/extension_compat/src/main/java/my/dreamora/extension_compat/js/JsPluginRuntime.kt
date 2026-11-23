package my.dreamora.extension_compat.js

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.asyncFunction
import com.dokar.quickjs.binding.function
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import my.dreamora.extension_compat.js.obj
import my.dreamora.extension_compat.js.str
import my.dreamora.extension_compat.js.stringOrNull
import timber.log.Timber
import java.nio.charset.Charset
import java.util.Base64

/**
 * One QuickJS runtime hosting one JS plugin (an LNReader or Mangayomi source).
 *
 * [create] installs the natives the host scripts call (`__native_*`), then runs the
 * bundled libraries, the common globals and the ecosystem host. After that the plugin
 * code is loaded with [evaluate] and its methods are reached through the host's
 * dispatcher functions (`__lnDispatch`, `__mgDispatch`).
 *
 * QuickJS is single-threaded, so every call goes through one mutex. A call that runs
 * longer than [CALL_TIMEOUT_MS] fails with a timeout.
 */
class JsPluginRuntime private constructor(
    private val quickJs: QuickJs,
) {
    private val mutex = Mutex()
    private var closed = false

    /** Evaluates [code] as a script and returns the completion value as text (promises are awaited). */
    suspend fun evaluate(code: String, filename: String = "plugin.js"): String? = mutex.withLock {
        check(!closed) { "JS runtime is closed" }
        quickJs.evaluate<String?>(code, filename)
    }

    /** Calls a host dispatcher such as `__lnDispatch(method, argsJson)` and returns its JSON text. */
    suspend fun call(dispatcher: String, method: String, argsJson: String): String? =
        withTimeout(CALL_TIMEOUT_MS) {
            evaluate("$dispatcher(${jsString(method)}, ${jsString(argsJson)})", "$dispatcher.js")
        }

    suspend fun close() {
        mutex.withLock {
            if (closed) return
            closed = true
            runCatching { quickJs.close() }
        }
    }

    companion object {
        const val CALL_TIMEOUT_MS = 60_000L

        /**
         * Creates a runtime for the ecosystem described by [hostScript] (the plugin contract,
         * `scripts.lnReader` or `scripts.mangayomi`). [hostName] only labels log lines.
         */
        suspend fun create(
            http: JsHttp,
            storage: JsStorage,
            scripts: JsHostScripts,
            hostScript: String,
            hostName: String,
        ): JsPluginRuntime {
            val quickJs = QuickJs.create(Dispatchers.IO)
            try {
                installNatives(quickJs, http, storage)
                quickJs.evaluate<Any?>(scripts.libs, "js-libs.js")
                quickJs.evaluate<Any?>(scripts.common, "host-common.js")
                quickJs.evaluate<Any?>(hostScript, "$hostName-host.js")
            } catch (e: Throwable) {
                runCatching { quickJs.close() }
                throw e
            }
            return JsPluginRuntime(quickJs)
        }

        private fun installNatives(quickJs: QuickJs, http: JsHttp, storage: JsStorage) {
            quickJs.function("__native_log") { args ->
                Timber.d("JS %s: %s", args.getOrNull(0), args.getOrNull(1))
                null
            }
            quickJs.function("__native_storage_get") { args ->
                storage.get(args[0] as String)
            }
            quickJs.function("__native_storage_set") { args ->
                storage.set(args[0] as String, args[1] as String?)
                null
            }
            quickJs.function("__native_storage_clear") { _ ->
                storage.clear()
                null
            }
            quickJs.function("__native_decode") { args ->
                decodeBase64(args[0] as String, args[1] as String)
            }
            quickJs.asyncFunction("__native_sleep") { args ->
                delay((args[0] as Number).toLong().coerceIn(0L, 60_000L))
                null
            }
            quickJs.asyncFunction("__native_fetch") { args ->
                performFetch(http, args[0] as String)
            }
        }

        /** Runs one request for the JS host and returns the response as JSON text. */
        private suspend fun performFetch(http: JsHttp, requestJson: String): String {
            val request = Json.parseToJsonElement(requestJson).jsonObject
            val url = request.str("url") ?: throw IllegalArgumentException("fetch: url is missing")
            val headers = request.obj("headers")
                ?.mapValues { (_, value) -> value.stringOrNull().orEmpty() }
                .orEmpty()
            val response = http.execute(
                JsHttpRequest(
                    method = request.str("method") ?: "GET",
                    url = url,
                    headers = headers,
                    body = request.str("body"),
                ),
            )
            val text = runCatching { String(response.body, charsetOf(response.headers)) }
                .getOrElse { String(response.body, Charsets.UTF_8) }
            return buildJsonObject {
                put("status", response.status)
                put("url", response.url)
                put("body", text)
                put("base64", Base64.getEncoder().encodeToString(response.body))
                putJsonObject("headers") {
                    response.headers.forEach { (name, value) -> put(name, JsonPrimitive(value)) }
                }
            }.toString()
        }

        private fun charsetOf(headers: Map<String, String>): Charset {
            val contentType = headers.entries
                .firstOrNull { it.key.equals("content-type", ignoreCase = true) }
                ?.value
                ?: return Charsets.UTF_8
            val name = CHARSET_PATTERN.find(contentType)?.groupValues?.get(1) ?: return Charsets.UTF_8
            return runCatching { Charset.forName(name) }.getOrDefault(Charsets.UTF_8)
        }

        private fun decodeBase64(base64: String, charset: String): String =
            String(Base64.getDecoder().decode(base64), Charset.forName(charset))

        private val CHARSET_PATTERN = Regex("charset=\"?([^;\"'\\s]+)", RegexOption.IGNORE_CASE)
    }
}
