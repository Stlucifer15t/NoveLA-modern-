package my.noveldokusha.extension_compat.core

/**
 * Превращает «ссылку, которую вставил пользователь» в конкретные URL для скачивания.
 *
 * Пользователь может вставить что угодно:
 *
 * | Ввод | Что понимаем |
 * |---|---|
 * | `https://github.com/keiyoushi/extensions` | репозиторий → API находит ветку и индекс |
 * | `https://github.com/o/r/blob/main/index.json` | файл → качаем его |
 * | `https://github.com/o/r/tree/repo/index.min.json` | файл в ветке `repo` |
 * | `https://github.com/o/r/releases/download/v1/repo.json` | файл релиза |
 * | `https://raw.githubusercontent.com/o/r/branch/.dist/plugins.min.json` | файл |
 * | `https://gitlab.com/o/r` | репозиторий без API-дерева → пробуем имена |
 * | `https://example.com/bookSource.json` | файл |
 * | `https://example.com/` | каталог → пробуем [CompatRepoParsers.INDEX_FILE_NAMES] |
 *
 * Все функции чистые (строка → план), поэтому покрыты юнит-тестом без сети.
 */
object RepoUrlResolver {

    /** Репо, где лежит ИСХОДНИК расширений, а не собранный индекс. */
    private val KNOWN_REWRITES = mapOf(
        // keiyoushi/extensions-source — Kotlin-исходники; собранные APK и индекс
        // живут в keiyoushi/extensions (ветка `repo`)
        "github.com/keiyoushi/extensions-source" to "https://github.com/keiyoushi/extensions",
        "github.com/keiyoushi/extensions-source/" to "https://github.com/keiyoushi/extensions",
        // LNReader: плагины в lnreader-sources, а индекс — в lnreader-plugins.
        // raw-URL, а не /tree/: ветка `plugins/v3.0.0` содержит слэш, и в форме
        // /tree/ её не отличить от пути (см. RepoUrlResolverTest).
        "github.com/LNReader/lnreader-sources" to
            "https://raw.githubusercontent.com/LNReader/lnreader-plugins/plugins/v3.0.0/.dist/plugins.min.json",
    )

    data class GitTarget(
        val host: GitHost,
        val owner: String,
        val repo: String,
        /** Ветка/тег из URL; null — надо спросить default_branch (GitHub API). */
        val ref: String?,
        /** Путь внутри репозитория (файл или каталог); null — корень. */
        val path: String?,
    )

    enum class GitHost {
        GITHUB,
        GITLAB,
        ;

        /** Шаблон raw-файла: `format(owner, repo, ref, path)`. */
        fun rawUrl(owner: String, repo: String, ref: String, path: String): String = when (this) {
            GITHUB -> "https://raw.githubusercontent.com/$owner/$repo/$ref/${path.trimStart('/')}"
            GITLAB -> "https://gitlab.com/$owner/$repo/-/raw/$ref/${path.trimStart('/')}"
        }
    }

    sealed interface Plan {
        /** Один файл: скачать и определить формат по содержимому. */
        data class File(val url: String) : Plan

        /** Git-репозиторий: индекс ищем через API (или перебором имён). */
        data class Git(val target: GitTarget) : Plan

        /** Произвольный хост: пробуем список URL по порядку. */
        data class Probe(val urls: List<String>) : Plan

        /** Пустой/непонятный ввод. */
        data object Invalid : Plan
    }

    /** Сколько кандидатов перебирать на чужом хосте — не устраиваем DoS. */
    const val MAX_PROBES = 8

    fun plan(input: String): Plan {
        val url = normalize(input)
        if (url.isEmpty()) return Plan.Invalid
        val lowered = url.lowercase()
        if (!lowered.startsWith("http://") && !lowered.startsWith("https://")) {
            // Допускаем «github.com/o/r» без схемы
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

    /** Подстановка известных «исходных» репозиториев на репозитории с индексом. */
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
     * Пути GitHub: `owner/repo[/tree|blob|raw|releases/...]`.
     * Файл узнаём по расширению — его отдаём как [Plan.File] сразу.
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
                // /releases/download/<tag>/<asset> — конкретный файл релиза
                if (segments.getOrNull(3)?.lowercase() == "download" && segments.size >= 6) {
                    val tag = segments[4]
                    val asset = segments.drop(5).joinToString("/")
                    return Plan.File("https://github.com/$owner/$repo/releases/download/$tag/$asset")
                }
                // /releases, /releases/latest — индекс всё равно в ветке
                return Plan.Git(GitTarget(GitHost.GITHUB, owner, repo, null, null))
            }

        }
        // «owner/repo» — обычный корень репозитория; лишние сегменты игнорируем
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
            // Каталог в raw-домене: пробуем известные имена индексов внутри него
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
        // «-/blob/<ref>/<path>» и «-/raw/<ref>/<path>»
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

    /** Прямая ссылка на файл (свой домен репозитория) или на каталог. */
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
        // «github.com/o/r» без схемы
        if (!s.contains("://") && s.contains("/")) s = "https://$s"
        return s
    }

    // ── GitHub API ───────────────────────────────────────────────────────────

    /** Ветка по умолчанию: `{"default_branch":"repo"}`. */
    fun apiDefaultBranchUrl(owner: String, repo: String): String =
        "https://api.github.com/repos/$owner/$repo"

    /**
     * Дерево репозитория. `recursive=false` — имена в корне (быстрый поиск индекса);
     * `recursive=true` — полное дерево для TREE-скана.
     */
    fun apiTreeUrl(owner: String, repo: String, ref: String, recursive: Boolean): String =
        "https://api.github.com/repos/$owner/$repo/git/trees/${urlEncodePath(ref)}" +
            if (recursive) "?recursive=1" else ""

    /** ref может содержать `/` (например `plugins/v3.0.0`) — GitHub API это любит. */
    private fun urlEncodePath(ref: String): String =
        ref.split('/').joinToString("%2F") { it.trim() }

    /** Имя индекса, если он лежит в списке файлов корня репозитория. */
    fun findIndexName(fileNames: List<String>): String? {
        for (candidate in CompatRepoParsers.INDEX_FILE_NAMES) {
            val hit = fileNames.firstOrNull { it.equals(candidate, ignoreCase = true) }
            if (hit != null) return hit
        }
        return null
    }

    /** Файлы репозитория, годные как источники (для TREE-скана). */
    fun treeFiles(paths: List<Pair<String, Long>>): List<CompatRepoParsers.TreeFile> =
        paths.filter { (path, _) -> path.substringAfterLast('.', "").lowercase() in setOf("json", "js", "dart") }
            .map { (path, size) -> CompatRepoParsers.TreeFile(path, size) }
}
