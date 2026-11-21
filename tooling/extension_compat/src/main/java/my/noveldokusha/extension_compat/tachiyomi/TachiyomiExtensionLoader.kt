package my.noveldokusha.extension_compat.tachiyomi

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import my.noveldokusha.extension_compat.core.CompatEcosystem
import my.noveldokusha.extension_compat.core.CompatException
import my.noveldokusha.extension_compat.core.CompatInstallRecord
import my.noveldokusha.extension_compat.core.CompatExtensionLoader
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.scraper.SourceInterface
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.lang.reflect.Constructor
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Loads Tachiyomi/Mihon/Aniyomi extension APKs and instantiates their sources.
 *
 * Uses a child-first ClassLoader to isolate extension classes from the host app.
 * Communicates with the extension via TachiyomiHostBridge running in the extension's ClassLoader.
 */
class TachiyomiExtensionLoader @javax.inject.Inject constructor(
    private val context: Context,
    private val networkClient: NetworkClient,
) : CompatExtensionLoader {

    override val ecosystem = CompatEcosystem.TACHIYOMI

    private val manifestParser = TachiyomiManifestParser()
    private val requestIdCounter = AtomicLong()
    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<Any>>()

    override suspend fun load(record: CompatInstallRecord, payload: ByteArray): List<SourceInterface> {
        return withContext(Dispatchers.IO) {
            // Payload contains the APK file bytes
            val apkFile = writeApkToCache(record, payload)
            val packageInfo = getPackageInfo(apkFile)
            val parseResult = manifestParser.parse(context.packageManager, packageInfo)

            parseResult.error?.let { throw CompatException(it, operation = "load", ecosystem = ecosystem) }
            val metadata = parseResult.metadata!!

            // Create isolated ClassLoader for this extension
            val classLoader = createClassLoader(apkFile)

            val loadedSources = mutableListOf<TachiyomiLoadedSource>()

            // Load Source classes
            for (sourceClassName in metadata.sourceClasses) {
                try {
                    val clazz = Class.forName(sourceClassName, true, classLoader)
                    val sourceInstance = instantiateSource(clazz)
                    loadedSources += TachiyomiLoadedSource(sourceInstance, sourceClassName, metadata)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load source class $sourceClassName")
                    throw CompatException("Failed to instantiate $sourceClassName: ${e.message}", operation = "load", ecosystem = ecosystem)
                }
            }

            // Load SourceFactory if present
            metadata.sourceFactoryClass?.let { factoryClassName ->
                try {
                    val clazz = Class.forName(factoryClassName, true, classLoader)
                    val factoryInstance = clazz.getDeclaredConstructor().newInstance()
                    val createSourcesMethod = clazz.getMethod("createSources")
                    val sources = createSourcesMethod.invoke(factoryInstance) as java.util.List<*>
                    for (source in sources) {
                        loadedSources += TachiyomiLoadedSource(source, factoryClassName, metadata)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load SourceFactory $factoryClassName")
                    throw CompatException("Failed to instantiate SourceFactory: ${e.message}", operation = "load", ecosystem = ecosystem)
                }
            }

            if (loadedSources.isEmpty()) {
                throw CompatException("No sources loaded from extension ${metadata.name}", operation = "load", ecosystem = ecosystem)
            }

            // Convert to NoveLA SourceInterface adapters
            return loadedSources.map { loaded ->
                TachiyomiSourceAdapter(
                    loadedSource = loaded,
                    networkClient = networkClient,
                    extensionId = record.id,
                    extensionMetadata = metadata,
                    classLoader = classLoader,
                    hostBridge = hostBridge,
                    pendingRequests = pendingRequests,
                    requestIdCounter = requestIdCounter,
                )
            }
        }
    }

    private val hostBridge = object : TachiyomiHostBridge {
        override fun onMangaPageResult(requestId: String, mangas: List<MangaData>, hasNextPage: Boolean) {
            pendingRequests.remove(requestId)?.complete(MangaPageResult(mangas, hasNextPage))
        }

        override fun onMangaDetailResult(requestId: String, manga: MangaData?, chapters: List<ChapterData>?) {
            pendingRequests.remove(requestId)?.complete(MangaDetailResult(manga, chapters))
        }

        override fun onPageListResult(requestId: String, pages: List<PageData>) {
            pendingRequests.remove(requestId)?.complete(pages)
        }

        override fun onFilterListResult(requestId: String, filters: List<FilterData>) {
            pendingRequests.remove(requestId)?.complete(filters)
        }

        override fun onFilterDataResult(requestId: String, data: Any?) {
            pendingRequests.remove(requestId)?.complete(data)
        }

        override fun onMangaByUrlResult(requestId: String, manga: MangaData?) {
            pendingRequests.remove(requestId)?.complete(manga)
        }

        override fun onError(requestId: String, error: String) {
            pendingRequests.remove(requestId)?.completeExceptionally(CompatException(error, operation = "bridge", ecosystem = ecosystem))
        }
    }

    private fun writeApkToCache(record: CompatInstallRecord, payload: ByteArray): File {
        val cacheDir = File(context.cacheDir, "tachiyomi_extensions")
        cacheDir.mkdirs()
        val apkFile = File(cacheDir, "${record.id}.apk")
        apkFile.writeBytes(payload)
        return apkFile
    }

    private fun getPackageInfo(apkFile: File): PackageInfo {
        val flags = PackageManager.GET_META_DATA or PackageManager.GET_SIGNATURES
        val packageInfo = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags)
        if (packageInfo == null) {
            throw CompatException("Failed to parse APK: ${apkFile.name}", operation = "load", ecosystem = ecosystem)
        }
        return packageInfo
    }

    private fun createClassLoader(apkFile: File): ClassLoader {
        return dalvik.system.PathClassLoader(
            apkFile.absolutePath,
            ClassLoader.getSystemClassLoader(),
        )
    }

    private fun instantiateSource(clazz: Class<*>): Any {
        return try {
            clazz.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
        } catch (e: NoSuchMethodException) {
            val constructors = clazz.declaredConstructors
            for (ctor in constructors) {
                if (ctor.parameterTypes.isEmpty() || ctor.parameterTypes.all { it.isPrimitive || it == String::class.java }) {
                    ctor.isAccessible = true
                    return ctor.newInstance()
                }
            }
            throw CompatException("No suitable constructor for ${clazz.name}", operation = "load", ecosystem = ecosystem)
        }
    }

    override fun clearCache() {
        val cacheDir = File(context.cacheDir, "tachiyomi_extensions")
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { it.delete() }
        }
        pendingRequests.clear()
    }

    // ── Internal result data classes ────────────────────────────────────────

    data class MangaPageResult(
        val mangas: List<MangaData>,
        val hasNextPage: Boolean,
    )

    data class MangaDetailResult(
        val manga: MangaData?,
        val chapters: List<ChapterData>?,
    )
}