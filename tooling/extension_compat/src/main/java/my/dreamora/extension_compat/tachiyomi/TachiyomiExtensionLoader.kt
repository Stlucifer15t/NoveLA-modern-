package my.dreamora.extension_compat.tachiyomi

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import my.dreamora.extension_compat.core.CompatEcosystem
import my.dreamora.extension_compat.core.CompatException
import my.dreamora.extension_compat.core.CompatExtensionLoader
import my.dreamora.extension_compat.core.CompatInstallRecord
import my.dreamora.extension_compat.core.CompatRuntime
import my.dreamora.extension_compat.core.CompatRuntimeState
import my.dreamora.extension_compat.core.CompatStorage
import my.dreamora.scraper.SourceInterface
import dalvik.system.DexClassLoader
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mihon/Tachiyomi/Aniyomi (and keiyoushi) ship extensions as APKs with dex bytecode
 * written against host APIs: `eu.kanade.tachiyomi.source.model.*`, Injekt preferences,
 * RxJava headers. So the loader does exactly what is honest:
 *
 *  1. reads the APK manifest metadata (`tachiyomi.extension.class` / `.factory` / `.name`)
 *     which proves the file really is an extension and not "just an apk";
 *  2. loads the dex in an isolated [DexClassLoader] and tries to resolve the source class;
 *  3. when the host provides no extension API it fails with a clear explanation, and
 *     the [CompatInstallRecord] stays in the library: adding the shim is enough to
 *     bring installed packages to life without reinstalling.
 *
 * The load error goes to [my.dreamora.extension_compat.core.CompatDiagnostics]
 * and is shown in the extension list rather than swallowed.
 */
@Singleton
class TachiyomiExtensionLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: CompatStorage,
) : CompatExtensionLoader {

    override val ecosystem: CompatEcosystem = CompatEcosystem.TACHIYOMI

    override suspend fun load(record: CompatInstallRecord, payload: ByteArray): List<SourceInterface> {
        val apk = payloadFile(record, payload)
        val metadata = readMetadata(apk.absolutePath)

        val sourceClass = metadata[KEY_CLASS]?.takeIf { it.isNotBlank() }
        val factoryClass = metadata[KEY_FACTORY]?.takeIf { it.isNotBlank() }
        val declaredName = metadata[KEY_NAME]?.takeIf { it.isNotBlank() }
        val className = sourceClass ?: factoryClass
        if (className.isNullOrBlank()) {
            throw CompatException(
                buildString {
                    append("Not a Mihon/Tachiyomi extension: ")
                    append(apk.name)
                    append(" has no ")
                    append(KEY_CLASS)
                    append(" manifest metadata")
                    if (metadata.isEmpty()) append(" (manifest unreadable — corrupt or unsupported APK)")
                },
                operation = "load",
            )
        }

        val dexCache = File(context.cacheDir, "compat-dex").apply { mkdirs() }
        val classLoader = DexClassLoader(apk.absolutePath, dexCache.absolutePath, null, javaClass.classLoader)
        val failure = runCatching { classLoader.loadClass(className) }.exceptionOrNull()
        if (failure != null) {
            throw CompatException(
                "Extension class '$className' cannot be loaded: ${missingHint(failure)} " +
                    "This build does not bundle the Tachiyomi source API (see EXTENSION_COMPATIBILITY_MATRIX.md).",
                cause = failure,
                operation = "load",
            )
        }
        // The class resolves — but without an adapter turning its suspend methods into
        // NoveLA sources there is nothing to load it into. Keep the record, explain next.
        val runtime = CompatRuntime.of(ecosystem)
        val suffix = declaredName?.let { " (declared as '$it')" }.orEmpty()
        throw CompatException(
            "APK verified: $className$suffix. ${runtime.detail}",
            operation = "load",
        )
    }

    /**
     * Manifest metadata of an APK that is not installed anywhere. An empty map means
     * "the archive cannot be read" — the caller then reports a format problem.
     */
    private fun readMetadata(path: String): Map<String, String?> = runCatching {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageArchiveInfo(path, PackageManager.GET_META_DATA)
            ?: return@runCatching emptyMap()
        val app: ApplicationInfo? = info.applicationInfo
        if (app != null) {
            // Without explicit paths PackageManager ignores metaData of an uninstalled archive
            app.sourceDir = path
            app.publicSourceDir = path
        }
        val bundle = app?.metaData ?: return@runCatching emptyMap()
        listOf(KEY_CLASS, KEY_NAME, KEY_FACTORY, KEY_NSFW).associateWith { key ->
            runCatching {
                bundle.getString(key)
            }.getOrNull()
        }
    }.getOrElse { e ->
        Timber.d(e, "Compat: cannot read manifest of %s", path)
        emptyMap()
    }

    private fun missingHint(error: Throwable): String {
        val cause = generateSequence(error) { it.cause }
            .firstOrNull { it is NoClassDefFoundError || it is ClassNotFoundException }
            ?: error
        val name = when (cause) {
            is NoClassDefFoundError -> cause.message ?: "NoClassDefFoundError"
            is ClassNotFoundException -> cause.message ?: "ClassNotFoundException"
            else -> cause::class.java.simpleName
        }
        return "missing host class $name."
    }

    private fun payloadFile(record: CompatInstallRecord, payload: ByteArray): File {
        val file = storage.payloadFile(record)
        if (!file.exists() || file.length() == 0L) {
            file.parentFile?.mkdirs()
            file.writeBytes(payload)
        }
        return file
    }

    /** This ecosystem is not executed by this build — a UI status, not an exception. */
    fun availability(): CompatRuntimeState = CompatRuntime.of(ecosystem).state

    private companion object {
        const val KEY_CLASS = "tachiyomi.extension.class"
        const val KEY_NAME = "tachiyomi.extension.name"
        const val KEY_FACTORY = "tachiyomi.extension.factory"
        const val KEY_NSFW = "tachiyomi.extension.nsfw"
    }
}
