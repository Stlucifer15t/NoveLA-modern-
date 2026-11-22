package my.noveldokusha.extension_compat.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * A record of one installed compat extension. Stored in registry.json
 * under filesDir/compat/. It describes ONE installed artifact (file/package)
 * that can back one or several sources.
 */
@Serializable
data class CompatInstallRecord(
    /** Unique id: "<ecosystem>:<source id or file name>". */
    val id: String,
    val ecosystem: CompatEcosystem,
    /** Display name (from the manifest/index/JSON). */
    val name: String,
    val version: String = "",
    /** ISO 639-1 language code, or a special value ("zh", "en", "MULTILANGUAGE"...). */
    val language: String = "",
    /** Source base URL when known (used to match book URLs). */
    val baseUrl: String = "",
    /** Content type: "" (novel), "manga", "video". */
    val contentType: String = "",
    /** Relative path of the file inside compat storage. */
    val payloadPath: String,
    /** URL it was installed from (used for updates). */
    val sourceUrl: String = "",
    /** Icon URL, if any. */
    val iconUrl: String = "",
    /** Whether the source is enabled. */
    val enabled: Boolean = true,
    /** SHA-256 of the APK signature (Tachiyomi/Aniyomi) — pinned on update. */
    val signerSha256: String? = null,
    /** Version code/name, used for update and downgrade checks. */
    val versionCode: Long = 0,
    /** Repository metadata (nsfw, groups, package name...) as provided. */
    val extra: JsonObject? = null,
    /** Install timestamp (unix millis). */
    val installedAt: Long = 0,
    /** Repository the source came from; "" means the file was imported by hand. */
    val repositoryId: String = "",
    /** Key of this row in the repository index: versions are compared through it. */
    val entryKey: String = "",
    /** Marked 18+ by the repository (Mihon nsfw/contentWarning, Legado group). */
    val nsfw: Boolean = false,
    /** SHA-256 of the payload: integrity check on update and for diagnostics. */
    val sha256: String = "",
)

/**
 * Description of a loaded source for the UI (the list in ExtensionsScreen).
 */
data class CompatSourceInfo(
    val record: CompatInstallRecord,
    /** How many sources this record produced (a SourceFactory may yield several). */
    val loadedSources: Int,
    /** Load error, when the source failed to come up. */
    val loadError: String? = null,
    /** Declared capabilities. */
    val capabilities: Set<CompatCapability> = emptySet(),
)
