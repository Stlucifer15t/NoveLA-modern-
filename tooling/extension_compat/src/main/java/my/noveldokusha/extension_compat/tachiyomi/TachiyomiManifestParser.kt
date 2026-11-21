package my.noveldokusha.extension_compat.tachiyomi

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.annotation.Nullable
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import timber.log.Timber

/**
 * Parses Tachiyomi/Mihon/Aniyomi extension APK manifest metadata.
 */
class TachiyomiManifestParser {

    companion object {
        private const val EXTENSION_FEATURE = "tachiyomi.extension"
        private const val METADATA_SOURCE_CLASS = "tachiyomi.extension.class"
        private const val METADATA_SOURCE_FACTORY = "tachiyomi.extension.factory"
        private const val METADATA_NSFW = "tachiyomi.extension.nsfw"
        private const val METADATA_LIB_VERSION = "tachiyomix.extensionLib"
        private const val METADATA_NAME = "tachiyomix.name"
        private const val METADATA_CONTENT_WARNING = "tachiyomix.contentWarning"

        private const val LIB_VERSION_MIN = 1.4
        private const val LIB_VERSION_MAX = 1.6

        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    }

    data class ExtensionMetadata(
        val packageName: String,
        val versionName: String,
        val versionCode: Long,
        val name: String,
        val libVersion: Double,
        val isNsfw: Boolean,
        val contentWarning: Int, // 0=Safe, 1=Mixed, 2=NSFW
        val sourceClasses: List<String>,
        val sourceFactoryClass: String?,
        val icon: android.graphics.drawable.Drawable?,
    )

    data class ParseResult(
        val metadata: ExtensionMetadata?,
        val error: String?,
    )

    /**
     * Parse extension metadata from an installed APK's PackageInfo.
     */
    fun parse(
        packageManager: PackageManager,
        packageInfo: PackageInfo,
    ): ParseResult {
        val appInfo = packageInfo.applicationInfo ?: return ParseResult(null, "No applicationInfo")

        // Check for extension feature
        if (!hasExtensionFeature(packageManager, packageInfo.packageName)) {
            return ParseResult(null, "Missing tachiyomi.extension feature")
        }

        val metaData = appInfo.metaData ?: return ParseResult(null, "No metaData in manifest")

        // Validate lib version
        val libVersion = metaData.getString(METADATA_LIB_VERSION)?.toDoubleOrNull()
        if (libVersion == null || libVersion < LIB_VERSION_MIN || libVersion > LIB_VERSION_MAX) {
            return ParseResult(null, "Lib version $libVersion not in supported range [$LIB_VERSION_MIN, $LIB_VERSION_MAX]")
        }

        // Source class(es)
        val sourceClassStr = metaData.getString(METADATA_SOURCE_CLASS) ?: return ParseResult(null, "Missing tachiyomi.extension.class")
        val sourceClasses = sourceClassStr.split(";").map { it.trim() }.filter { it.isNotBlank() }
            .map { cls ->
                if (cls.startsWith(".")) packageInfo.packageName + cls else cls
            }

        // Source factory (optional)
        val sourceFactoryClass = metaData.getString(METADATA_SOURCE_FACTORY)?.let { cls ->
            if (cls.startsWith(".")) packageInfo.packageName + cls else cls
        }

        // NSFW flag
        val isNsfw = metaData.getInt(METADATA_NSFW) == 1

        // Content warning (0=Safe, 1=Mixed, 2=NSFW)
        val contentWarning = metaData.getInt(METADATA_CONTENT_WARNING)

        // Extension name
        val label = packageManager.getApplicationLabel(appInfo).toString()
        val name = metaData.getString(METADATA_NAME)?.ifBlank { label } ?: label

        // Icon
        val icon = try {
            appInfo.loadIcon(packageManager)
        } catch (e: Exception) {
            Timber.w(e, "Failed to load extension icon")
            null
        }

        val metadata = ExtensionMetadata(
            packageName = packageInfo.packageName,
            versionName = packageInfo.versionName ?: "0",
            versionCode = android.content.pm.PackageInfoCompat.getLongVersionCode(packageInfo),
            name = name,
            libVersion = libVersion,
            isNsfw = isNsfw,
            contentWarning = contentWarning,
            sourceClasses = sourceClasses,
            sourceFactoryClass = sourceFactoryClass,
            icon = icon,
        )

        return ParseResult(metadata, null)
    }

    private fun hasExtensionFeature(packageManager: PackageManager, packageName: String): Boolean {
        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
            val features = info.applicationInfo?.metaData?.getString(EXTENSION_FEATURE)
            return features != null
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }
    }
}

/**
 * Represents a loaded Tachiyomi source instance with its metadata.
 */
data class TachiyomiLoadedSource(
    val source: Any, // eu.kanade.tachiyomi.extension.source.KeiSource or HttpSource
    val sourceClass: String,
    val extensionMetadata: TachiyomiManifestParser.ExtensionMetadata,
)