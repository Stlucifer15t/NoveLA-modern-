package my.noveldokusha.extension_compat.mangayomi

import io.github.dokar3.quickjs.QuickJSContext
import io.github.dokar3.quickjs.QuickJSEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * QuickJS runtime for executing Mangayomi JavaScript sources.
 * Provides MBridge utilities that Mangayomi plugins expect.
 */
class MangayomiRuntime {

    private val engine by lazy { QuickJSEngine() }
    private val contexts = ConcurrentHashMap<String, QuickJSContext>()
    private val sourceCodeCache = ConcurrentHashMap<String, String>()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Load a source from JavaScript code.
     * Returns the source instance metadata.
     */
    suspend fun loadSource(sourceId: String, jsCode: String): Result<MangayomiSourceInstance> {
        return withContext(Dispatchers.IO) {
            try {
                val context = engine.newContext()
                contexts[sourceId] = context

                // Inject Mangayomi globals
                injectGlobals(context, sourceId)

                // Evaluate the source code
                val result = context.evaluate(jsCode)
                if (result.isException) {
                    val error = result.exception?.toString() ?: "Unknown error"
                    return@withContext Result.failure(Exception("Source evaluation failed: $error"))
                }

                // Get the exported DefaultExtension instance
                val export = context.evaluate("DefaultExtension")
                if (export.isException || export.isNullish) {
                    return@withContext Result.failure(Exception("Source did not export DefaultExtension"))
                }

                // Extract source metadata
                val metadata = extractSourceMetadata(context, export)
                sourceCodeCache[sourceId] = jsCode

                Result.success(MangayomiSourceInstance(
                    sourceId = sourceId,
                    context = context,
                    metadata = metadata,
                    export = export,
                ))
            } catch (e: Exception) {
                Timber.e(e, "Failed to load Mangayomi source: $sourceId")
                Result.failure(e)
            }
        }
    }

    /**
     * Inject global objects and functions that Mangayomi sources expect.
     * Based on: lib/eval/javascript/service.dart and lib/eval/model/m_bridge.dart
     */
    private fun injectGlobals(context: QuickJSContext, sourceId: String) {
        // Console
        context.evaluate("""
            var console = {
                log: function(...args) { java.lang.System.out.println('[Mangayomi:' + '$sourceId' + '] ' + args.join(' ')); },
                warn: function(...args) { java.lang.System.err.println('[Mangayomi:' + '$sourceId' + ' WARN] ' + args.join(' ')); },
                error: function(...args) { java.lang.System.err.println('[Mangayomi:' + '$sourceId' + ' ERROR] ' + args.join(' ')); },
            };
        """.trimIndent())

        // Global variables
        context.evaluate("""
            var Client = function() {
                this.get = function(url, headers) { return javaHttpGet(url, headers || {}); };
                this.post = function(url, headers, body) { return javaHttpPost(url, headers || {}, body || ''); };
            };
        """.trimIndent())

        // Document (cheerio-like)
        context.evaluate("""
            var Document = function(html) {
                this.html = function() { return html; };
                this.select = function(selector) { return new Document(''); };
                this.selectFirst = function(selector) { return { attr: function(name) { return ''; }, text: function() { return ''; } }; };
                this.text = function() { return html; };
                this.attr = function(name) { return ''; };
            };
        """.trimIndent())

        // MBridge utilities
        injectMBridge(context)

        // String utilities
        context.evaluate("""
            String.prototype.substringAfter = function(pattern) {
                var idx = this.indexOf(pattern);
                return idx >= 0 ? this.substring(idx + pattern.length) : '';
            };
            String.prototype.substringAfterLast = function(pattern) {
                var idx = this.lastIndexOf(pattern);
                return idx >= 0 ? this.substring(idx + pattern.length) : '';
            };
            String.prototype.substringBefore = function(pattern) {
                var idx = this.indexOf(pattern);
                return idx >= 0 ? this.substring(0, idx) : this;
            };
            String.prototype.substringBeforeLast = function(pattern) {
                var idx = this.lastIndexOf(pattern);
                return idx >= 0 ? this.substring(0, idx) : this;
            };
            String.prototype.substringBetween = function(left, right) {
                var l = this.indexOf(left);
                if (l < 0) return '';
                var r = this.indexOf(right, l + left.length);
                return r >= 0 ? this.substring(l + left.length, r) : '';
            };
        """.trimIndent())
    }

    private fun injectMBridge(context: QuickJSContext) {
        // MBridge provides native bindings for JS/Dart code
        context.evaluate("""
            var MBridge = {
                // HTML parsing with XPath support
                parsHtml: function(html, xpath) {
                    return javaParseHtml(html, xpath);
                },
                xpath: function(html, xpath) {
                    return javaXPath(html, xpath);
                },
                // JS unpacking
                unpackJs: function(code) {
                    return javaUnpackJs(code);
                },
                deobfuscateJsPassword: function(input) {
                    return javaDeobfuscateJsPassword(input);
                },
                // Crypto
                encryptAESCryptoJS: function(plainText, passphrase) {
                    return javaEncryptAES(plainText, passphrase);
                },
                decryptAESCryptoJS: function(encrypted, passphrase) {
                    return javaDecryptAES(encrypted, passphrase);
                },
                cryptoHandler: function(text, iv, secretKey, encrypt) {
                    return javaCryptoHandler(text, iv, secretKey, encrypt);
                },
                // Video extractors
                gogoCdnExtractor: function(url, referer) {
                    return javaGogoCdnExtractor(url, referer);
                },
                doodExtractor: function(url, referer) {
                    return javaDoodExtractor(url, referer);
                },
                streamWishExtractor: function(url, referer) {
                    return javaStreamWishExtractor(url, referer);
                },
                filemoonExtractor: function(url, referer) {
                    return javaFilemoonExtractor(url, referer);
                },
            };
        """.trimIndent())
    }

    /**
     * Extract source metadata from the exported instance.
     */
    private fun extractSourceMetadata(context: QuickJSContext, export: Any): MangayomiSourceMetadata {
        val id = getStringProperty(context, export, "id") ?: ""
        val name = getStringProperty(context, export, "name") ?: ""
        val baseUrl = getStringProperty(context, export, "baseUrl") ?: ""
        val apiUrl = getStringProperty(context, export, "apiUrl") ?: ""
        val lang = getStringProperty(context, export, "lang") ?: "en"
        val iconUrl = getStringProperty(context, export, "iconUrl") ?: ""
        val version = getIntProperty(context, export, "version") ?: 1
        val isManga = getBooleanProperty(context, export, "isManga") ?: true
        val isNsfw = getBooleanProperty(context, export, "isNsfw") ?: false

        return MangayomiSourceMetadata(
            id = id,
            name = name,
            baseUrl = baseUrl,
            apiUrl = apiUrl,
            lang = lang,
            iconUrl = iconUrl,
            version = version,
            isManga = isManga,
            isNsfw = isNsfw,
        )
    }

    private fun getStringProperty(context: QuickJSContext, obj: Any, prop: String): String? {
        return try {
            val result = context.evaluate("(function(o, p) { return o[p]; })", obj, prop)
            if (result.isString) result.string else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getIntProperty(context: QuickJSContext, obj: Any, prop: String): Int? {
        return try {
            val result = context.evaluate("(function(o, p) { return o[p]; })", obj, prop)
            if (result.isNumber) result.number?.toInt() else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getBooleanProperty(context: QuickJSContext, obj: Any, prop: String): Boolean? {
        return try {
            val result = context.evaluate("(function(o, p) { return o[p]; })", obj, prop)
            if (result.isBoolean) result.boolean else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Call a source method with arguments.
     */
    suspend fun callSourceMethod(
        sourceId: String,
        methodName: String,
        args: Array<Any>,
    ): Result<Any> {
        return withContext(Dispatchers.IO) {
            val context = contexts[sourceId] ?: return@withContext Result.failure(Exception("Source not loaded: $sourceId"))
            try {
                val argJson = json.encodeToString(args)
                val js = """
                    (async function() {
                        var source = DefaultExtension;
                        var args = $argJson;
                        var result = await source.$methodName.apply(source, args);
                        return JSON.stringify(result);
                    })()
                """.trimIndent()
                val result = context.evaluate(js)
                if (result.isException) {
                    val error = result.exception?.toString() ?: "Unknown error"
                    return@withContext Result.failure(Exception("Method $methodName failed: $error"))
                }
                if (result.isString) {
                    return@withContext Result.success(json.decodeFromString(result.string))
                }
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Call getPopular(page)
     */
    suspend fun callGetPopular(sourceId: String, page: Int): Result<MPages> {
        val args = arrayOf(page)
        return callSourceMethod(sourceId, "getPopular", args).map { json.decodeFromString<MPages>(it.toString()) }
    }

    /**
     * Call getLatestUpdates(page)
     */
    suspend fun callGetLatestUpdates(sourceId: String, page: Int): Result<MPages> {
        val args = arrayOf(page)
        return callSourceMethod(sourceId, "getLatestUpdates", args).map { json.decodeFromString<MPages>(it.toString()) }
    }

    /**
     * Call search(query, page, filters)
     */
    suspend fun callSearch(sourceId: String, query: String, page: Int, filters: List<Any>): Result<MPages> {
        val args = arrayOf(query, page, filters)
        return callSourceMethod(sourceId, "search", args).map { json.decodeFromString<MPages>(it.toString()) }
    }

    /**
     * Call getDetail(url)
     */
    suspend fun callGetDetail(sourceId: String, url: String): Result<MManga> {
        val args = arrayOf(url)
        return callSourceMethod(sourceId, "getDetail", args).map { json.decodeFromString<MManga>(it.toString()) }
    }

    /**
     * Call getPageList(url)
     */
    suspend fun callGetPageList(sourceId: String, url: String): Result<List<PageUrl>> {
        val args = arrayOf(url)
        return callSourceMethod(sourceId, "getPageList", args).map { json.decodeFromString<List<PageUrl>>(it.toString()) }
    }

    /**
     * Call getVideoList(url)
     */
    suspend fun callGetVideoList(sourceId: String, url: String): Result<List<Video>> {
        val args = arrayOf(url)
        return callSourceMethod(sourceId, "getVideoList", args).map { json.decodeFromString<List<Video>>(it.toString()) }
    }

    /**
     * Call getHtmlContent(name, url)
     */
    suspend fun callGetHtmlContent(sourceId: String, name: String, url: String): Result<String> {
        val args = arrayOf(name, url)
        return callSourceMethod(sourceId, "getHtmlContent", args).map { it.toString() }
    }

    /**
     * Call cleanHtmlContent(html)
     */
    suspend fun callCleanHtmlContent(sourceId: String, html: String): Result<String> {
        val args = arrayOf(html)
        return callSourceMethod(sourceId, "cleanHtmlContent", args).map { it.toString() }
    }

    /**
     * Call getFilterList()
     */
    suspend fun callGetFilterList(sourceId: String): Result<FilterList> {
        return callSourceMethod(sourceId, "getFilterList", emptyArray()).map { json.decodeFromString<FilterList>(it.toString()) }
    }

    /**
     * Call getSourcePreferences()
     */
    suspend fun callGetSourcePreferences(sourceId: String): Result<List<SourcePreference>> {
        return callSourceMethod(sourceId, "getSourcePreferences", emptyArray()).map { json.decodeFromString<List<SourcePreference>>(it.toString()) }
    }

    /**
     * Get source properties
     */
    suspend fun getSourceProperties(sourceId: String): Result<MangayomiSourceMetadata> {
        return withContext(Dispatchers.IO) {
            val context = contexts[sourceId] ?: return@withContext Result.failure(Exception("Source not loaded: $sourceId"))
            try {
                val export = context.evaluate("DefaultExtension")
                val metadata = extractSourceMetadata(context, export)
                Result.success(metadata)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun unloadSource(sourceId: String) {
        contexts.remove(sourceId)?.close()
        sourceCodeCache.remove(sourceId)
    }

    fun close() {
        contexts.values.forEach { it.close() }
        contexts.clear()
        sourceCodeCache.clear()
        engine.close()
    }

    data class MangayomiSourceInstance(
        val sourceId: String,
        val context: QuickJSContext,
        val metadata: MangayomiSourceMetadata,
        val export: Any,
    )

    data class MangayomiSourceMetadata(
        val id: String,
        val name: String,
        val baseUrl: String,
        val apiUrl: String,
        val lang: String,
        val iconUrl: String,
        val version: Int,
        val isManga: Boolean,
        val isNsfw: Boolean,
    )
}