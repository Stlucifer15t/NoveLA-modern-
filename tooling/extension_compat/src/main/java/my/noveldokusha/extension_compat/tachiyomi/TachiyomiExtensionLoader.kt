package my.noveldokusha.extension_compat.tachiyomi

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import my.noveldokusha.extension_compat.core.CompatEcosystem
import my.noveldokusha.extension_compat.core.CompatException
import my.noveldokusha.extension_compat.core.CompatExtensionLoader
import my.noveldokusha.extension_compat.core.CompatInstallRecord
import my.noveldokusha.extension_compat.core.CompatRuntime
import my.noveldokusha.extension_compat.core.CompatRuntimeState
import my.noveldokusha.extension_compat.core.CompatStorage
import my.noveldokusha.scraper.SourceInterface
import dalvik.system.DexClassLoader
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mihon/Tachiyomi/Aniyomi (и keiyoushi) поставляют расширения как APK с dex-байткодом,
 * написанным против API хоста: `eu.kanade.tachiyomi.source.model.*`, Injekt-преференсы,
 * RxJava-заголовки. Поэтому загрузчик делает ровно то, что честно:
 *
 *  1. читает метаданные манифеста APK (`tachiyomi.extension.class` / `.factory` / `.name`) —
 *     это доказывает, что файл действительно расширение, а не «просто apk»;
 *  2. поднимает dex в изолированном [DexClassLoader] и пробует разрешить класс источника;
 *  3. если хост не предоставляет API расширения — падает с внятным объяснением, и
 *     [CompatInstallRecord] остаётся в библиотеке: достаточно добавить shim, чтобы
 *     установленные пакеты ожили без переустановки.
 *
 * Ошибка загрузки попадает в [my.noveldokusha.extension_compat.core.CompatDiagnostics]
 * и показывается в списке расширений, а не проглатывается.
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
        // Класс виден — но без адаптера suspend-методов источника в источники NoveLA
        // загружать его не во что. Держим запись и объясняем следующий шаг.
        val runtime = CompatRuntime.of(ecosystem)
        val suffix = declaredName?.let { " (declared as '$it')" }.orEmpty()
        throw CompatException(
            "APK verified: $className$suffix. ${runtime.detail}",
            operation = "load",
        )
    }

    /**
     * Метаданные манифеста APK, который нигде не установлен. Пустая карта означает
     * «архив не читается» — вызывающий сообщит о формате, а не упадёт.
     */
    private fun readMetadata(path: String): Map<String, String?> = runCatching {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageArchiveInfo(path, PackageManager.GET_META_DATA)
            ?: return@runCatching emptyMap()
        val app: ApplicationInfo? = info.applicationInfo
        if (app != null) {
            // Без явных путей PackageManager игнорирует metaData у не-установленного архива
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

    /** Экосистема не исполняется в этой сборке — статус для UI без исключения. */
    fun availability(): CompatRuntimeState = CompatRuntime.of(ecosystem).state

    private companion object {
        const val KEY_CLASS = "tachiyomi.extension.class"
        const val KEY_NAME = "tachiyomi.extension.name"
        const val KEY_FACTORY = "tachiyomi.extension.factory"
        const val KEY_NSFW = "tachiyomi.extension.nsfw"
    }
}
