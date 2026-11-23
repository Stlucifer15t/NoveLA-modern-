package my.dreamora

import android.app.Application
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.work.Configuration as WorkConfiguration
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.network.CacheStrategy
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.Options
import coil3.request.crossfade
import coil3.request.allowHardware
import coil3.request.allowRgb565
import okio.Path.Companion.toPath
import dagger.hilt.EntryPoints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import dagger.hilt.android.HiltAndroidApp
import my.dreamora.core.AppCacheConfig
import my.dreamora.core.LocaleManager
import my.dreamora.core.appPreferences.AppLanguage
import my.dreamora.core.appPreferences.AppLanguageProvider
import my.dreamora.core.appPreferences.AppPreferences
import my.dreamora.di.HiltAppEntryPoint
import my.dreamora.data.DownloadManager
import my.dreamora.network.NetworkClient
import my.dreamora.network.ScraperNetworkClient
import my.dreamora.debug.MemoryDiagnostics
import my.dreamora.logging.FileTree
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import java.util.Locale


@HiltAndroidApp
class App : Application(), SingletonImageLoader.Factory, WorkConfiguration.Provider {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Inject
    lateinit var networkClient: NetworkClient

    // Eager singleton: forces DownloadManager to be created at app start, so
    // restoreTasksFromDatabase() runs immediately instead of on the first book open.
    @Inject
    lateinit var downloadManager: DownloadManager

    override fun attachBaseContext(newBase: Context?) {
        val base = newBase ?: return super.attachBaseContext(null)
        super.attachBaseContext(LocaleManager.createAppLocaleContext(base))
    }

    override fun onCreate() {
        super.onCreate()

        val appPreferences = EntryPoints.get(this, HiltAppEntryPoint::class.java).appPreferences()
        resolveAppLanguage(appPreferences)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            MemoryDiagnostics.logMemoryStats()
            applicationScope.launch {
                delay(30_000)
                while (true) {
                    MemoryDiagnostics.logMemoryStats()
                    delay(60_000)
                }
            }
        }

        // File tree for log export — all build types
        val logFile = File(filesDir, "logs/app.log")
        Timber.plant(FileTree(logFile))

        // Session header: which build this log belongs to.
        // Written straight to the file — R8 strips Timber.i from the release dex.
        runCatching {
            val buildType = if (BuildConfig.DEBUG) "debug" else "release"
            logFile.appendText(
                "=== ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())} " +
                    "app start ${BuildConfig.VERSION_NAME}-$buildType (${BuildConfig.GIT_COMMIT_HASH}) " +
                    "API${Build.VERSION.SDK_INT} ${Build.MANUFACTURER} ${Build.MODEL} ===\n"
            )
        }

        // Catch uncaught exceptions: write the crash to app.log before the process dies
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Timber.e(throwable, "FATAL uncaught on ${thread.name}")
            } catch (_: Exception) {
                // logging must not get in the way of the default handler
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    override fun newImageLoader(context: Context): ImageLoader {
        val diskCache = coil3.disk.DiskCache.Builder()
            .directory("${context.cacheDir.absolutePath}/${AppCacheConfig.IMAGE_DISK_CACHE_DIR}".toPath())
            .maxSizeBytes(AppCacheConfig.IMAGE_DISK_CACHE_BYTES)
            .build()

        val memoryCache = coil3.memory.MemoryCache.Builder()
            .maxSizePercent(context, AppCacheConfig.IMAGE_MEMORY_CACHE_PERCENT) // 25% is the norm for networked grid UIs (Mihon=20%, the Coil sample=25%)
            .build()

        val animatorDurationScale = Settings.System.getFloat(
            contentResolver, Settings.System.ANIMATOR_DURATION_SCALE, 1f
        )

        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val isLowRamDevice = activityManager.isLowRamDevice

        val sharedBuilder = ImageLoader.Builder(context)
            .fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(8))
            .decoderCoroutineContext(Dispatchers.IO.limitedParallelism(3))
            .memoryCache(memoryCache)
            .diskCache(diskCache)
            .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
            .crossfade((AppCacheConfig.IMAGE_CROSSFADE_MS * animatorDurationScale).toInt())
            .allowHardware(AppCacheConfig.IMAGE_HARDWARE_BITMAPS)
            .allowRgb565(AppCacheConfig.IMAGE_RGB565) // RGB_565: 2 bytes per pixel instead of 4 — twice as many covers in the cache

        return when (val networkClient = networkClient) {
            is ScraperNetworkClient -> sharedBuilder
                .components {
                    add(
                        OkHttpNetworkFetcherFactory(
                            callFactory = { networkClient.client },
                            cacheStrategy = { RevalidatingCacheStrategy },
                        )
                    )
                }
                .build()

            else -> sharedBuilder.build()
        }
    }

    private fun resolveAppLanguage(appPreferences: AppPreferences): AppLanguage {
        if (appPreferences.IS_FOLLOW_SYSTEM_LANGUAGE.value || !appPreferences.IS_FIRST_LAUNCH_DONE.value) {
            val systemLocale = getSystemLocale()
            val detected = AppLanguageProvider.fromLocale(systemLocale)
            if (!appPreferences.IS_FIRST_LAUNCH_DONE.value) {
                appPreferences.APP_LANGUAGE_CODE.value = detected.code
                appPreferences.IS_FIRST_LAUNCH_DONE.value = true
            }
            return detected
        }
        return AppLanguageProvider.fromCode(appPreferences.APP_LANGUAGE_CODE.value)
            ?: AppLanguageProvider.supportedLanguages.first()
    }

    private fun getSystemLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale
        }
    }

    // WorkManager — custom factory for @HiltWorker workers (LibraryUpdates, UpdatesChecker)
    override val workManagerConfiguration: WorkConfiguration by lazy {
        val appWorkerFactory = EntryPoints
            .get(this, HiltAppEntryPoint::class.java)
            .workerFactory()

        WorkConfiguration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.INFO)
            .setWorkerFactory(appWorkerFactory)
            .build()
    }
}

// Coil's disk-cache policy: never serve an error from the cache, never cache one.
// The default policy caches 404 because the RFC allows it, and always reads
// the cache without a freshness check — a poisoned entry lives until LRU evicts
// it, so a retry is pointless. AppCacheConfig.isCacheableStatus decides
// what is cacheable. Verified on a device: a 404 from 30.09 in image_cache.
@OptIn(ExperimentalCoilApi::class)
private object RevalidatingCacheStrategy : CacheStrategy {

    // A body that cannot be decoded as an image and therefore poisons the cache:
    // 1) text/* — a CDN or blocking-page placeholder (text/html, text/plain, …);
    // 2) image/avif — the CDN only serves AVIF when Accept asks for it, and on device
    //    HeifDecoderImpl cannot decode it ("videoFrame is a nullptr"); a retry reads
    //    back the same immutable entry and fails forever.
    private fun NetworkResponse.isUnusableBody(): Boolean {
        val contentType = headers["content-type"]?.substringBefore(';')?.trim() ?: return false
        return contentType.startsWith("text/", ignoreCase = true) ||
            contentType.equals("image/avif", ignoreCase = true)
    }

    override suspend fun read(
        cacheResponse: NetworkResponse,
        networkRequest: NetworkRequest,
        options: Options,
    ): CacheStrategy.ReadResult {
        // A cacheable response is served as is; an error or an unusable body always
        // goes to the network (which also heals entries poisoned before this fix).
        return if (
            AppCacheConfig.isCacheableStatus(cacheResponse.code) && !cacheResponse.isUnusableBody()
        ) {
            CacheStrategy.ReadResult(cacheResponse)
        } else {
            CacheStrategy.ReadResult(networkRequest)
        }
    }

    override suspend fun write(
        cacheResponse: NetworkResponse?,
        networkRequest: NetworkRequest,
        networkResponse: NetworkResponse,
        options: Options,
    ): CacheStrategy.WriteResult {
        // Cache only cacheable status codes and a usable body: "404: Not Found",
        // text placeholders and AVIF do not decode, they only poison the cache.
        if (
            AppCacheConfig.isCacheableStatus(networkResponse.code) &&
            !networkResponse.isUnusableBody()
        ) {
            return CacheStrategy.WriteResult(networkResponse)
        }
        // DISABLED on an open snapshot (cacheResponse != null) leaks file descriptors:
        // NetworkFetcher does not close the snapshot when response == null. An entry with
        // no body goes through closeAndOpenEditor instead (snapshot closed, nothing
        // written); the old entry is unusable and would be refetched anyway.
        return if (cacheResponse != null) {
            CacheStrategy.WriteResult(networkResponse.copy(body = null))
        } else {
            CacheStrategy.WriteResult.DISABLED
        }
    }
}
