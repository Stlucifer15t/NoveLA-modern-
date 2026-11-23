package my.dreamora.extension_compat.core

import kotlinx.serialization.Serializable

/**
 * Subscription to a user-added extension repository.
 *
 * The user pastes ONE line of text — [url]. It may be:
 *  - a direct index file (`.../index.min.json`, `.../bookSource.json`);
 *  - a whole repository (`https://github.com/owner/repo`);
 *  - a file inside a repository (`.../blob/main/sources.json`);
 *  - a release page (`.../releases/latest`).
 *
 * [RepoUrlResolver] decides what is actually downloaded: it picks [indexUrl]
 * (or [RepoKind.TREE] when there is no index and each source is its own file).
 * The index format is detected from its content ([CompatRepoParsers.sniff]) and
 * stored in [ecosystem], so later refreshes don't have to guess again.
 */
@Serializable
data class CompatRepository(
    /** Stable subscription id: URL slug + short hash (CompatRepositoryManager.repositoryId).
     * Keys the index cache (`indexes/<id>.json`) and links installed records back. */
    val id: String,
    /** The URL exactly as the user typed it. */
    val url: String,
    /** The index file actually downloaded (empty for [RepoKind.TREE]). */
    val indexUrl: String = "",
    val kind: RepoKind = RepoKind.INDEX,
    /** Ecosystem detected from the index content. */
    val ecosystem: CompatEcosystem? = null,
    /** Display name (from the repository manifest, else host/path). */
    val name: String = "",
    val enabled: Boolean = true,
    val addedAt: Long = 0,
    val lastRefreshAt: Long = 0,
    /** How many sources the index contains (used by "Install all"). */
    val entryCount: Int = 0,
    /** Last refresh failure — shown in the repository list instead of crashing the UI. */
    val lastError: String? = null,
    /** Branch/tag when it can be derived from the URL (needed for TREE and relative paths). */
    val ref: String = "",
    /** Repository signing fingerprint (Mihon v2 `signingKey`) — APK pinning. */
    val signerFingerprint: String = "",
)

/**
 * How the repository is laid out:
 *  - [INDEX] — one index file listing every source (keiyoushi `index.json`,
 *    LNReader `plugins.min.json`, Legado `bookSource.json`, Mangayomi `novel_index.json`).
 *  - [TREE] — no index at all, each source is a separate file
 *    (typical for Legado repos of `site-name.json` files). The file list comes
 *    from the repository's git tree or a directory listing.
 */
enum class RepoKind {
    INDEX,
    TREE,
}

/**
 * One installable source inside a repository — a normalized
 * row of any supported index format. [CompatRepositoryManager] can
 * install an extension from it ([install]).
 *
 * The payload is obtained one of two ways:
 *  - [payloadUrl] non-empty → download that file (APK / JS / single bookSource);
 *  - [inlineKey] non-empty → the object already lives in the cached index
 *    (Legado collections: bookSource objects are inside `bookSource.json`).
 */
@Serializable
data class CompatRepoEntry(
    /** Stable key inside the repository (pkg / id / bookSourceUrl / file path). */
    val key: String,
    val repoId: String = "",
    val name: String,
    val ecosystem: CompatEcosystem,
    val version: String = "",
    val versionCode: Long = 0,
    /** ISO 639-1, or "" when the repository does not declare a language. */
    val language: String = "",
    /** Source home URL — NoveLA matches book URLs against it. */
    val baseUrl: String = "",
    val payloadUrl: String = "",
    /** Key of the object in the cached index (Legado) when the payload is inline. */
    val inlineKey: String = "",
    val iconUrl: String? = null,
    /** "" = novel, "manga", "video", "audio". */
    val contentType: String = "",
    val nsfw: Boolean = false,
    val description: String = "",
    /** Extension of the downloaded payload — decides which loader runs. */
    val payloadKind: CompatPayloadKind = CompatPayloadKind.JSON,
    /** Fingerprint from the repository (if any) — checked when installing an APK. */
    val signerFingerprint: String = "",
    /** File size from the index, bytes (0 = unknown). */
    val size: Long = 0,
    /** Remaining index fields verbatim — for diagnostics and future adapters. */
    val extra: Map<String, String> = emptyMap(),
)

/** Kind of payload we download and hand to [CompatStorage]. */
enum class CompatPayloadKind(val fileExtension: String) {
    JSON("json"),
    JS("js"),
    DART("dart"),
    JAR("jar"),
    APK("apk"),
    CS3("cs3"),
    ;

    companion object {
        fun fromUrlOrName(url: String): CompatPayloadKind = when {
            url.endsWith(".apk", true) -> APK
            url.endsWith(".jar", true) -> JAR
            url.endsWith(".cs3", true) -> CS3
            url.endsWith(".dart", true) -> DART
            url.endsWith(".js", true) -> JS
            else -> JSON
        }
    }
}

/**
 * Result of adding/refreshing a repository — the UI shows it right in the
 * dialog ("Detected: Legado, 412 sources"), so the user can see whether
 * the app understands their URL instead of just "something loaded".
 */
data class CompatRepoResult(
    val repository: CompatRepository? = null,
    val entries: List<CompatRepoEntry> = emptyList(),
    val ok: Boolean = false,
    /** Human-readable explanation of a failure (which URLs were tried, why none fit). */
    val message: String = "",
)
