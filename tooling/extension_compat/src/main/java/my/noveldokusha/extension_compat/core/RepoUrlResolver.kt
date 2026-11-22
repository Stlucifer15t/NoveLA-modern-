package my.noveldokusha.extension_compat.core

/**
 * Turns "whatever link the user pasted" into concrete URLs to download.
 *
 * The user may paste anything:
 *
 * | Input | Meaning |
 * |---|---|
 * | `https://github.com/keiyoushi/extensions` | repository → the API finds branch and index |
 * | `https://github.com/o/r/blob/main/index.json` | a file → download it |
 * | `https://github.com/o/r/tree/repo/index.min.json` | a file in branch `repo` |
 * | `https://github.com/o/r/releases/download/v1/repo.json` | a release asset |
 * | `https://raw.githubusercontent.com/o/r/branch/.dist/plugins.min.json` | a file |
 * | `https://gitlab.com/o/r` | repository without a tree API → probe known names |
 * | `https://example.com/bookSource.json` | a file |
 * | `https://example.com/` | a directory → try [CompatRepoParsers.INDEX_FILE_NAMES] |
 *
 * Every function is pure (string → plan), so it is unit-tested without network.
 */
object RepoUrlResolver {

    /** Repos holding extension SOURCE code rather than a built index. */
    private val KNOWN_REWRITES = mapOf(
        // keiyoushi/extensions-source is Kotlin source; the built APKs and index
        // live in keiyoushi/extensions (branch `repo`)
        "github.com/keiyoushi/extensions-source" to "https://github.com/keiyoushi/extensions",
        "github.com/keiyoushi/extensions-source/" to "https://github.com/keiyoushi/extensions",
        // LNReader: plugins live in lnreader-sources, the index in lnreader-plugins.
        // A raw URL rather than /tree/: the branch `plugins/v3.0.0` contains a slash,
        // so in /tree/ form it cannot be told from a path (see RepoUrlResolverTest).
        "github.com/LNReader/lnreader-sources" to
            "https://raw.githubusercontent.com/LNReader/lnreader-plugins/plugins/v3.0.0/.dist/plugins.min.json",
    )

    data class GitTarget(
        val host: GitHost,
        val owner: String,
        val repo: String,
        /** Branch/tag from the URL; null means ask default_branch (GitHub API). */
        val ref: String?,
        /** Path inside the repository (file or directory); null means the root. */
        val path: String?,
    )

    enum class GitHost {
        GITHUB,
        GITLAB,
        ;

        /** Raw file URL template: `format(owner, repo, ref, path)`. */
        fun rawUrl(owner: String, repo: String, ref: String, path: String): String = when (this) {
            GITHUB -> "https://raw.githubusercontent.com/$owner/$repo/$ref/${path.trimStart('/')}"
            GITLAB -> "https://gitlab.com/$owner/$repo/-/raw/$ref/${path.trimStart('/')}"
        }
    }

    sealed interface Plan {
        /** A single file: download it and detect the format from its content. */
        data class File(val url: String) : Plan

        /** Git repository: find the index through the API (or by probing names). */
        data class Git(val target: GitTarget) : Plan

        /** Any host: try a list of URLs in order. */
        data class Probe(val urls: List<String>) : Plan

        /** Empty or unparseable input. */
        data object Invalid : Plan
    }

    /** How many candidates to probe on a foreign host — no self-inflicted DoS. */
    const val MAX_PROBES = 8

    fun plan(input: String): Plan {
        val url = normalize(input)
        if (url.isEmpty()) return Plan.Invalid
        val lowered = url.lowercase()
        if (!lowered.startsWith("http://") && !lowered.startsWith("https://")) {
            // Allow "github.com/o/r" without a scheme
            val withScheme = if (lowered.contains("/")) "https://$url" else ""
            if (withScheme.isEmpty()) return Plan.Invalid
            return plan(withScheme)
        }

        val hostPart = url.substringAfter("://").substringBefore('/')
        val rest = url.substringAfter("://").substringAfter('/', "")
        val path = rest.substringBefore('?').substringBefore('#')

        return when {
            hostPart.equals("github.com", true) || hostPart.equals("www.github.com", true) ->
                githubPlan(path)

            hostPart.equals("raw.githubusercontent.com", true) -> rawGithubPlan(path)

            hostPart.equals("gitlab.com", true) || hostPart.endsWith(".gitlab.com", true) ->
                gitlabPlan(path)

            hostPart.equals("cdn.jsdelivr.net", true) -> jsdelivrPlan(path)

            else -> fileOrProbe(url)
        }
    }

    /** Rewrite well-known "source code" repositories to the ones holding an index. */
    fun rewriteKnown(input: String): String {
        val trimmed = input.trim().trimEnd('/')
        val key = trimmed.lowercase().removePrefix("https://").removePrefix("http://")
        for ((from, to) in KNOWN_REWRITES) {
            if (key == from.trimEnd('/')) return to
        }
        return input.trim()
    }

    // ── GitHub ───────────────────────────────────────────────────────────────

    /**
     * GitHub paths: `owner/repo[/tree|blob|raw|releases/...]`.
     * A file is recognized by its extension and returned as [Plan.File] directly.
     */
    private fun githubPlan(path: String): Plan {
        val segments = path.split('/').filter { it.isNotBlank() }
        if (segments.size < 2) return Plan.Invalid
        val owner = segments[0]
        val repo = segments[1].removeSuffix(".git")
        when (segments.getOrNull(2)?.lowercase()) {
            "tree" -> {
                val ref = segments.getOrNull(3) ?: return Plan.Git(GitTarget(GitHost.GITHUB, owner, repo, null, null))
                val sub = segments.drop(4).joinToString("/")
                return if (sub.isBlank()) {
                    Plan.Git(GitTarget(GitHost.GITHUB, owner, repo, ref, null))
                } else if (isIndexLike(sub)) {
                    Plan.File(GitHost.GITHUB.rawUrl(owner, repo, ref, sub))
                } else {
                    Plan.Git(GitTarget(GitHost.GITHUB, owner, repo, ref, sub))
                }
            }

            "blob", "raw" -> {
                val ref = segments.getOrNull(3) ?: ""
                val sub = segments.drop(4).joinToString("/")
                if (ref.isBlank() || sub.isBlank()) {
                    return Plan.Git(GitTarget(GitHost.GITHUB, owner, repo, ref.ifBlank { null }, null))
                }
                return Plan.File(GitHost.GITHUB.rawUrl(owner, repo, ref, sub))
            }

            "releases" -> {
                // /releases/download/<tag>/<asset> — a concrete release asset
                if (segments.getOrNull(3)?.lowercase() == "download" && segments.size >= 6) {
                    val tag = segments[4]
                    val asset = segments.drop(5).joinToString("/")
                    return Plan.File("https://github.com/$owner/$repo/releases/download/$tag/$asset")
                }
                // /releases, /releases/latest — the index still lives in a branch
                return Plan.Git(GitTarget(GitHost.GITHUB, owner, repo, null, null))
            }

        }
        // "owner/repo" — a plain repository root; extra segments are ignored
        return Plan.Git(GitTarget(GitHost.GITHUB, owner, repo, null, null))
    }

    private fun rawGithubPlan(path: String): Plan {
        val segments = path.split('/').filter { it.isNotBlank() }
        if (segments.size < 4) return Plan.Invalid
        val owner = segments[0]
        val repo = segments[1]
        val ref = segments[2]
        val sub = segments.drop(3).joinToString("/")
        return if (sub.isBlank()) {
            Plan.Git(GitTarget(GitHost.GITHUB, owner, repo, ref, null))
        } else if (isIndexLike(sub)) {
            Plan.File("https://raw.githubusercontent.com/$owner/$repo/$ref/$sub")
        } else {
            // A directory on a raw host: probe known index names inside it
            Plan.Probe(
                CompatRepoParsers.INDEX_FILE_NAMES.take(MAX_PROBES)
                    .map { "https://raw.githubusercontent.com/$owner/$repo/$ref/$sub/$it" }
            )
        }
    }

    private fun gitlabPlan(path: String): Plan {
        val segments = path.split('/').filter { it.isNotBlank() }
        if (segments.size < 2) return Plan.Invalid
        val owner = segments[0]
        val repo = segments[1].removeSuffix(".git")
        val sub = segments.drop(2)
        // "-/blob/<ref>/<path>" and "-/raw/<ref>/<path>"
        val marker = sub.indexOfFirst { it == "blob" || it == "raw" }
        if (marker >= 0 && sub.size >= marker + 3) {
            val ref = sub[marker + 1]
            val file = sub.drop(marker + 2).joinToString("/")
            if (isIndexLike(file)) {
                return Plan.File(GitHost.GITLAB.rawUrl(owner, repo, ref, file))
            }
            return Plan.Git(GitTarget(GitHost.GITLAB, owner, repo, ref, file.ifBlank { null }))
        }
        return Plan.Git(GitTarget(GitHost.GITLAB, owner, repo, null, null))
    }

    private fun jsdelivrPlan(path: String): Plan {
        // cdn.jsdelivr.net/gh/owner/repo@ref/path
        val segments = path.split('/').filter { it.isNotBlank() }
        if (segments.size < 3 || segments[0].lowercase() != "gh") return Plan.Invalid
        val owner = segments[1]
        val repoRef = segments[2]
        val repo = repoRef.substringBefore('@')
        val ref = repoRef.substringAfter('@', "").ifBlank { null }
        val sub = segments.drop(3).joinToString("/")
        if (sub.isBlank()) return Plan.Git(GitTarget(GitHost.GITHUB, owner, repo, ref, null))
        if (isIndexLike(sub)) {
            return Plan.File("https://cdn.jsdelivr.net/gh/$owner/$repo${ref?.let { "@$it" } ?: ""}/$sub")
        }
        return Plan.Probe(
            CompatRepoParsers.INDEX_FILE_NAMES.take(MAX_PROBES)
                .map {
                    "https://cdn.jsdelivr.net/gh/$owner/$repo${ref?.let { r -> "@$r" } ?: ""}/$sub/$it"
                }
        )
    }

    /** Direct link to a file (repository's own domain) or to a directory. */
    private fun fileOrProbe(url: String): Plan {
        val clean = url.substringBefore('?').substringBefore('#')
        val last = clean.substringAfterLast('/')
        if (last.contains('.')) return Plan.File(url)
        val base = if (clean.endsWith("/")) clean else "$clean/"
        val probes = ArrayList<String>(CompatRepoParsers.INDEX_FILE_NAMES.size + 1)
        probes += url
        probes += CompatRepoParsers.INDEX_FILE_NAMES.take(MAX_PROBES).map { base + it }
        return Plan.Probe(probes)
    }

    private fun isIndexLike(path: String): Boolean {
        val name = path.substringAfterLast('/').lowercase()
        if (!name.contains('.')) return false
        val ext = name.substringAfterLast('.')
        return ext in setOf("json", "txt", "yaml", "yml", "js")
    }

    private fun normalize(input: String): String {
        var s = input.trim().trimEnd('/')
        if (s.isEmpty()) return ""
        // "github.com/o/r" without a scheme
        if (!s.contains("://") && s.contains("/")) s = "https://$s"
        return s
    }

    // ── GitHub API ───────────────────────────────────────────────────────────

    /** Default branch: `{"default_branch":"repo"}`. */
    fun apiDefaultBranchUrl(owner: String, repo: String): String =
        "https://api.github.com/repos/$owner/$repo"

    /**
     * Repository tree. `recursive=false` — root names (fast index lookup);
     * `recursive=true` — the full tree for a TREE scan.
     */
    fun apiTreeUrl(owner: String, repo: String, ref: String, recursive: Boolean): String =
        "https://api.github.com/repos/$owner/$repo/git/trees/${urlEncodePath(ref)}" +
            if (recursive) "?recursive=1" else ""

    /** A ref may contain `/` (e.g. `plugins/v3.0.0`) — the GitHub API accepts it. */
    private fun urlEncodePath(ref: String): String =
        ref.split('/').joinToString("%2F") { it.trim() }

    /** The index file name, if the repository root listing contains one. */
    fun findIndexName(fileNames: List<String>): String? {
        for (candidate in CompatRepoParsers.INDEX_FILE_NAMES) {
            val hit = fileNames.firstOrNull { it.equals(candidate, ignoreCase = true) }
            if (hit != null) return hit
        }
        return null
    }

    /** Repository files that can be sources (used by the TREE scan). */
    fun treeFiles(paths: List<Pair<String, Long>>): List<CompatRepoParsers.TreeFile> =
        paths.filter { (path, _) -> path.substringAfterLast('.', "").lowercase() in setOf("json", "js", "dart") }
            .map { (path, size) -> CompatRepoParsers.TreeFile(path, size) }
}
