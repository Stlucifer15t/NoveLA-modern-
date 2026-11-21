package my.noveldokusha.extension_compat.cloudstream

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.net.URL
import java.net.URLClassLoader
import java.util.concurrent.ConcurrentHashMap

/**
 * Runtime for loading and executing CloudStream plugins.
 * CloudStream plugins are Kotlin/JVM classes packaged as JAR or .cs3 files.
 */
class CloudStreamRuntime {

    private val classLoaders = ConcurrentHashMap<String, URLClassLoader>()
    private val pluginInstances = ConcurrentHashMap<String, CloudStreamPluginInstance>()
    private val mainApis = ConcurrentHashMap<String, Any>() // MainAPI instances
    private val extractorApis = ConcurrentHashMap<String, Any>() // ExtractorApi instances

    /**
     * Load a CloudStream plugin from a JAR/.cs3 file.
     */
    suspend fun loadPlugin(
        context: Context,
        pluginId: String,
        pluginFile: File,
    ): Result<CloudStreamPluginInstance> {
        return withContext(Dispatchers.IO) {
            try {
                // Create isolated ClassLoader for this plugin
                val classLoader = createClassLoader(pluginFile)
                classLoaders[pluginId] = classLoader

                // Load the plugin class
                val pluginInstance = loadPluginClass(classLoader, pluginId)
                pluginInstances[pluginId] = pluginInstance

                // Call plugin.load(context) to register MainAPI and ExtractorApi
                callLoadMethod(pluginInstance.instance, context)

                // Extract registered MainAPI and ExtractorApi instances
                extractApis(pluginId, classLoader)

                Result.success(pluginInstance)
            } catch (e: Exception) {
                Timber.e(e, "Failed to load CloudStream plugin: $pluginId")
                Result.failure(e)
            }
        }
    }

    private fun createClassLoader(pluginFile: File): URLClassLoader {
        val urls = arrayOf(pluginFile.toURI().toURL())
        // Parent-last (child-first) classloader to isolate plugin classes
        return URLClassLoader(urls, ClassLoader.getSystemClassLoader()) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                // First try to load from plugin JAR
                return try {
                    findClass(name)
                } catch (e: ClassNotFoundException) {
                    // Fall back to parent
                    super.loadClass(name, resolve)
                }
            }
        }
    }

    private fun loadPluginClass(classLoader: URLClassLoader, pluginId: String): CloudStreamPluginInstance {
        // Find the plugin class - it should be annotated with @CloudstreamPlugin
        // or extend BasePlugin/Plugin
        val pluginClassName = findPluginClass(classLoader)
        val pluginClass = classLoader.loadClass(pluginClassName)
        val instance = pluginClass.getDeclaredConstructor().apply { isAccessible = true }.newInstance()

        return CloudStreamPluginInstance(
            pluginId = pluginId,
            classLoader = classLoader,
            pluginClass = pluginClass,
            instance = instance,
        )
    }

    private fun findPluginClass(classLoader: URLClassLoader): String {
        // In CloudStream, the plugin class name is in the manifest
        // For now, we'll scan for classes annotated with @CloudstreamPlugin
        // or extending BasePlugin
        val manifest = classLoader.getResourceAsStream("META-INF/cloudstream-plugin.json")
        if (manifest != null) {
            // Parse manifest for pluginClassName
            manifest.close()
        }

        // Fallback: scan all classes in the JAR
        // This is a simplified approach - real implementation would read the manifest
        throw UnsupportedOperationException("Plugin class discovery not fully implemented")
    }

    private fun callLoadMethod(pluginInstance: Any, context: Context) {
        try {
            val loadMethod = pluginInstance.javaClass.getMethod("load", Context::class.java)
            loadMethod.invoke(pluginInstance, context)
        } catch (e: NoSuchMethodException) {
            // Try the parameterless load() method
            try {
                val loadMethod = pluginInstance.javaClass.getMethod("load")
                loadMethod.invoke(pluginInstance)
            } catch (e2: Exception) {
                Timber.w("No load() method found in plugin")
            }
        } catch (e: Exception) {
            Timber.w(e, "Error calling load() on plugin")
        }
    }

    private fun extractApis(pluginId: String, classLoader: ClassLoader) {
        // CloudStream registers providers via static APIHolder
        // We need to extract them from the plugin's classloader
        try {
            val apiHolderClass = classLoader.loadClass("com.lagradost.cloudstream3.APIHolder")
            val allProvidersField = apiHolderClass.getDeclaredField("allProviders")
            allProvidersField.isAccessible = true
            val providers = allProvidersField.get(null) as java.util.List<*>

            val mainApisList = mutableListOf<Any>()
            val extractorApisList = mutableListOf<Any>()

            for (provider in providers) {
                val className = provider.javaClass.name
                if (className.contains("MainAPI") || className.contains("Provider")) {
                    mainApisList.add(provider)
                } else if (className.contains("ExtractorApi")) {
                    extractorApisList.add(provider)
                }
            }

            if (mainApisList.isNotEmpty()) {
                this.mainApis[pluginId] = mainApisList
            }
            if (extractorApisList.isNotEmpty()) {
                this.extractorApis[pluginId] = extractorApisList
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to extract APIs from plugin: $pluginId")
        }
    }

    /**
     * Get MainAPI instances for a plugin.
     */
    fun getMainApis(pluginId: String): List<Any> {
        return (mainApis[pluginId] as? List<*>)?.filterIsInstance<Any>() ?: emptyList()
    }

    /**
     * Get ExtractorApi instances for a plugin.
     */
    fun getExtractorApis(pluginId: String): List<Any> {
        return (extractorApis[pluginId] as? List<*>)?.filterIsInstance<Any>() ?: emptyList()
    }

    /**
     * Call a MainAPI method via reflection.
     */
    suspend fun callMainApiMethod(
        pluginId: String,
        apiIndex: Int,
        methodName: String,
        args: Array<Any>,
    ): Result<Any> {
        return withContext(Dispatchers.IO) {
            val apis = getMainApis(pluginId)
            if (apiIndex >= apis.size) {
                return@withContext Result.failure(Exception("API index out of bounds"))
            }
            val api = apis[apiIndex]
            try {
                val method = api.javaClass.getMethod(methodName, *args.map { it.javaClass }.toTypedArray())
                val result = method.invoke(api, *args)
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Call an ExtractorApi method via reflection.
     */
    suspend fun callExtractorApiMethod(
        pluginId: String,
        apiIndex: Int,
        methodName: String,
        args: Array<Any>,
    ): Result<Any> {
        return withContext(Dispatchers.IO) {
            val apis = getExtractorApis(pluginId)
            if (apiIndex >= apis.size) {
                return@withContext Result.failure(Exception("API index out of bounds"))
            }
            val api = apis[apiIndex]
            try {
                val method = api.javaClass.getMethod(methodName, *args.map { it.javaClass }.toTypedArray())
                val result = method.invoke(api, *args)
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun unloadPlugin(pluginId: String) {
        classLoaders.remove(pluginId)?.close()
        pluginInstances.remove(pluginId)
        mainApis.remove(pluginId)
        extractorApis.remove(pluginId)
    }

    fun close() {
        classLoaders.values.forEach { it.close() }
        classLoaders.clear()
        pluginInstances.clear()
        mainApis.clear()
        extractorApis.clear()
    }

    data class CloudStreamPluginInstance(
        val pluginId: String,
        val classLoader: URLClassLoader,
        val pluginClass: Class<*>,
        val instance: Any,
    )
}