package my.noveldokusha.extension_compat.lnreader

import io.github.dokar3.quickjs.QuickJSContext
import io.github.dokar3.quickjs.QuickJSEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * QuickJS runtime for executing LNReader plugins.
 * Each plugin runs in its own isolated context.
 */
class LNReaderRuntime {

    private val engine by lazy { QuickJSEngine() }
    private val contexts = ConcurrentHashMap<String, QuickJSContext>()
    private val pluginCodeCache = ConcurrentHashMap<String, String>()
    private val requestIdCounter = AtomicLong()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Load a plugin from JavaScript code.
     * Returns the plugin instance metadata.
     */
    suspend fun loadPlugin(pluginId: String, jsCode: String): Result<LNReaderPluginInstance> {
        return withContext(Dispatchers.IO) {
            try {
                val context = engine.newContext()
                contexts[pluginId] = context

                // Inject LNReader globals
                injectGlobals(context, pluginId)

                // Evaluate the plugin code (IIFE that exports default)
                val result = context.evaluate(jsCode)
                if (result.isException) {
                    val error = result.exception?.toString() ?: "Unknown error"
                    return@withContext Result.failure(Exception("Plugin evaluation failed: $error"))
                }

                // Get the exported default instance
                val pluginExport = context.evaluate("module.exports")
                if (pluginExport.isException || pluginExport.isNullish) {
                    return@withContext Result.failure(Exception("Plugin did not export default instance"))
                }

                // Extract plugin metadata
                val metadata = extractPluginMetadata(context, pluginExport)
                pluginCodeCache[pluginId] = jsCode

                Result.success(LNReaderPluginInstance(
                    pluginId = pluginId,
                    context = context,
                    metadata = metadata,
                    export = pluginExport,
                ))
            } catch (e: Exception) {
                Timber.e(e, "Failed to load LNReader plugin: $pluginId")
                Result.failure(e)
            }
        }
    }

    /**
     * Inject global objects and functions that LNReader plugins expect.
     */
    private fun injectGlobals(context: QuickJSContext, pluginId: String) {
        // Console
        context.evaluate("""
            var console = {
                log: function(...args) { java.lang.System.out.println('[LNReader:' + '$pluginId' + '] ' + args.join(' ')); },
                warn: function(...args) { java.lang.System.err.println('[LNReader:' + '$pluginId' + ' WARN] ' + args.join(' ')); },
                error: function(...args) { java.lang.System.err.println('[LNReader:' + '$pluginId' + ' ERROR] ' + args.join(' ')); },
            };
        """.trimIndent())

        // Global variables
        context.evaluate("""
            var key = "";
            var page = 1;
            var baseURL = "";
            var source = "";
            var defaultCover = "data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 150'><rect fill='%23ddd' width='100' height='150'/><text x='50' y='80' text-anchor='middle' fill='%23999' font-size='12'>No Cover</text></svg>";
        """.trimIndent())

        // NovelStatus enum
        context.evaluate("""
            var NovelStatus = {
                Ongoing: 'Ongoing',
                Completed: 'Completed',
                Hiatus: 'Hiatus',
                Unknown: 'Unknown',
            };
        """.trimIndent())

        // FilterTypes
        context.evaluate("""
            var FilterTypes = {
                Picker: 'Picker',
                Text: 'Text',
                Switch: 'Switch',
                Checkbox: 'Checkbox',
                XCheckbox: 'XCheckbox',
            };
        """.trimIndent())

        // HTTP client (bridged to NoveLA's NetworkClient)
        injectHttpClient(context, pluginId)

        // Cheerio-like DOM parser
        injectCheerio(context)

        // Utility functions
        injectUtilities(context)
    }

    private fun injectHttpClient(context: QuickJSContext, pluginId: String) {
        context.evaluate("""
            var fetchApi = function(url, init) {
                return javaFetch(url, init || {});
            };
            var fetchText = function(url, init, encoding) {
                return javaFetchText(url, init || {}, encoding || 'utf-8');
            };
            var fetchProto = function(protoInit, url, init) {
                return javaFetchProto(protoInit, url, init || {});
            };
        """.trimIndent())

        // The actual Java implementations are provided via the host bridge
        // We'll use a bridge interface to call back to Kotlin
    }

    private fun injectCheerio(context: QuickJSContext) {
        context.evaluate("""
            var Document = function(html) {
                this.html = function() { return html; };
                this.find = function(selector) { return new Document(''); }; // stub
                this.select = function(selector) { return new Document(''); };
                this.selectFirst = function(selector) { return { attr: function(name) { return ''; }, text: function() { return ''; } }; };
                this.text = function() { return html; };
            };
        """.trimIndent())
    }

    private fun injectUtilities(context: QuickJSContext) {
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

    /**
     * Extract plugin metadata from the exported instance.
     */
    private fun extractPluginMetadata(context: QuickJSContext, export: Any): LNReaderPluginMetadata {
        val id = getStringProperty(context, export, "id") ?: ""
        val name = getStringProperty(context, export, "name") ?: ""
        val icon = getStringProperty(context, export, "icon") ?: ""
        val site = getStringProperty(context, export, "site") ?: ""
        val version = getStringProperty(context, export, "version") ?: "1.0.0"

        return LNReaderPluginMetadata(
            id = id,
            name = name,
            icon = icon,
            site = site,
            version = version,
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

    /**
     * Call a plugin method with arguments.
     */
    suspend fun callPluginMethod(
        pluginId: String,
        methodName: String,
        args: Array<Any>,
    ): Result<Any> {
        return withContext(Dispatchers.IO) {
            val context = contexts[pluginId] ?: return@withContext Result.failure(Exception("Plugin not loaded: $pluginId"))
            try {
                val argJson = json.encodeToString(args)
                val js = """
                    (async function() {
                        var plugin = module.exports;
                        var args = $argJson;
                        var result = await plugin.$methodName.apply(plugin, args);
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
     * Call popularNovels(page, options)
     */
    suspend fun callPopularNovels(pluginId: String, page: Int, options: PopularNovelsOptions): Result<List<NovelItem>> {
        val args = arrayOf(page, json.encodeToString(options))
        return callPluginMethod(pluginId, "popularNovels", args).map { json.decodeFromString<List<NovelItem>>(it.toString()) }
    }

    /**
     * Call parseNovel(novelPath)
     */
    suspend fun callParseNovel(pluginId: String, novelPath: String): Result<SourceNovel> {
        val args = arrayOf(novelPath)
        return callPluginMethod(pluginId, "parseNovel", args).map { json.decodeFromString<SourceNovel>(it.toString()) }
    }

    /**
     * Call parseChapter(chapterPath)
     */
    suspend fun callParseChapter(pluginId: String, chapterPath: String): Result<String> {
        val args = arrayOf(chapterPath)
        return callPluginMethod(pluginId, "parseChapter", args).map { it.toString() }
    }

    /**
     * Call searchNovels(searchTerm, page)
     */
    suspend fun callSearchNovels(pluginId: String, searchTerm: String, page: Int): Result<List<NovelItem>> {
        val args = arrayOf(searchTerm, page)
        return callPluginMethod(pluginId, "searchNovels", args).map { json.decodeFromString<List<NovelItem>>(it.toString()) }
    }

    /**
     * Call parsePage(novelPath, page)
     */
    suspend fun callParsePage(pluginId: String, novelPath: String, page: String): Result<SourcePage> {
        val args = arrayOf(novelPath, page)
        return callPluginMethod(pluginId, "parsePage", args).map { json.decodeFromString<SourcePage>(it.toString()) }
    }

    /**
     * Call resolveUrl(path, isNovel)
     */
    suspend fun callResolveUrl(pluginId: String, path: String, isNovel: Boolean): Result<String> {
        val args = arrayOf(path, isNovel)
        return callPluginMethod(pluginId, "resolveUrl", args).map { it.toString() }
    }

    /**
     * Get filters from plugin
     */
    suspend fun getFilters(pluginId: String): Result<FilterDefinition> {
        val context = contexts[pluginId] ?: return Result.failure(Exception("Plugin not loaded"))
        try {
            val result = context.evaluate("module.exports.filters")
            if (result.isException || result.isNullish) {
                return Result.success(FilterDefinition())
            }
            val jsonStr = result.isString ? result.string : json.encodeToString(result)
            return Result.success(json.decodeFromString<FilterDefinition>(jsonStr))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get plugin settings
     */
    suspend fun getPluginSettings(pluginId: String): Result<Map<String, SettingDefinition>> {
        val context = contexts[pluginId] ?: return Result.failure(Exception("Plugin not loaded"))
        try {
            val result = context.evaluate("module.exports.pluginSettings")
            if (result.isException || result.isNullish) {
                return Result.success(emptyMap())
            }
            val jsonStr = result.isString ? result.string : json.encodeToString(result)
            return Result.success(json.decodeFromString(jsonStr))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun unloadPlugin(pluginId: String) {
        contexts.remove(pluginId)?.close()
        pluginCodeCache.remove(pluginId)
    }

    fun close() {
        contexts.values.forEach { it.close() }
        contexts.clear()
        pluginCodeCache.clear()
        engine.close()
    }

    data class LNReaderPluginInstance(
        val pluginId: String,
        val context: QuickJSContext,
        val metadata: LNReaderPluginMetadata,
        val export: Any,
    )

    data class LNReaderPluginMetadata(
        val id: String,
        val name: String,
        val icon: String,
        val site: String,
        val version: String,
    )
}