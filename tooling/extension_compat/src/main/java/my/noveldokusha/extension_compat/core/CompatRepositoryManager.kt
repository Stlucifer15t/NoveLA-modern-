package my.noveldokusha.extension_compat.core

import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.getRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import timber.log.Timber
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager of third-party extension repositories: subscribe → parse the index
 * → install the chosen sources into [CompatStorage] → [CompatSourceProviderImpl]
 * rebuilds the source list and they show up in the Finder.
 *
 * Adding a URL ([add]) goes like this:
 *  1. [RepoUrlResolver.plan] decides what the URL means (file / git repo / directory);
 *  2. for a GitHub repository we ask for the tree (one request) and pick the
 *     index file by [CompatRepoParsers.INDEX_FILE_NAMES] priority; with no index
 *     at all we fall back to TREE mode (every `*.json` = one source);
 *  3. the index content is sniffed ([CompatRepoParsers.sniff]) and parsed by the
 *     ecosystem parser; CloudStream additionally expands nested pluginLists;
 *  4. the raw index and the parsed rows are cached on disk, so the list can be
 *     shown offline and so installing an inline bookSource does not need the
 *     network.
 *
 * Installing stores the payload and creates a [CompatInstallRecord]; when the
 * ecosystem's runtime is not part of the build ([CompatRuntime]) the source is
 * still listed in Extensions with an explanation instead of silently missing.
 */
@Singleton
class CompatRepositoryManager @Inject constructor(
    private val storage: CompatStorage,
    private val networkClient: NetworkClient,
    private val provider: CompatSourceProviderImpl,
    private val diagnostics: CompatDiagnostics,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    val repositories: StateFlow<List<CompatRepository>> = storage.repositories
    val installedRecords: StateFlow<List<CompatInstallRecord>> = storage.records

    private val _entries = MutableStateFlow<Map<String, List<CompatRepoEntry>>>(emptyMap())
    /** Parsed repository catalogs: repoId → installable sources. */
    val entries: StateFlow<Map<String, List<CompatRepoEntry>>> = _entries.asStateFlow()

    private val _busy = MutableStateFlow<Set<String>>(emptySet())
    /** repoId of repositories being refreshed right now (UI: row spinner). */
    val busy: StateFlow<Set<String>> = _busy.asStateFlow()

    /** One rebuild at a time: parallel refreshes must not fight over the cache. */
    private val refreshMutex = Mutex()

    // ── Subscriptions ───────────────────────────────────────────────────────────

    suspend fun add(inputUrl: String): CompatRepoResult {
        val url = RepoUrlResolver.rewriteKnown(inputUrl)
        if (url.isBlank()) {
            return CompatRepoResult(message = "Enter a repository URL (repo, index file, or release).")
        }
        val id = repositoryId(url)
        val existing = storage.repositories.value.firstOrNull { it.id == id }
        if (existing != null) {
            return CompatRepoResult(
                repository = existing,
                entries = entriesOf(existing.id),
                ok = true,
                message = "Already added: ${existing.displayName()}",
            )
        }
        val repo = CompatRepository(
            id = id,
            url = url,
            addedAt = System.currentTimeMillis(),
        )
        storage.upsertRepository(repo)
        val result = refreshInternal(repo)
        if (!result.ok) {
            // Nothing to keep: an empty subscription only confuses the list
            storage.removeRepository(id)
            return result.copy(repository = null)
        }
        return result
    }

    suspend fun refresh(repoId: String): CompatRepoResult {
        val repo = storage.repositories.value.firstOrNull { it.id == repoId }
            ?: return CompatRepoResult(message = "Repository not found")
        return refreshInternal(repo)
    }

    /**
     * Remove a subscription. Sources installed from it stay by default —
     * the user may already be reading from them; [removeInstalled] clears those too.
     */
    suspend fun remove(repoId: String, removeInstalled: Boolean = false) {
        if (removeInstalled) {
            storage.records.value
                .filter { it.repositoryId == repoId }
                .forEach { provider.uninstall(it.id) }
        }
        storage.removeRepository(repoId)
        _entries.value = _entries.value - repoId
    }

    fun setRepositoryEnabled(repoId: String, enabled: Boolean) {
        storage.setRepositoryEnabled(repoId, enabled)
    }

    // ── Repository source catalog ──────────────────────────────────────────────

    suspend fun entriesOf(repoId: String): List<CompatRepoEntry> =
        _entries.value[repoId] ?: withContext(Dispatchers.IO) {
            val cached = storage.readIndex(repoId, "entries") ?: return@withContext emptyList()
            val parsed = runCatching {
                json.decodeFromString(ListSerializer(CompatRepoEntry.serializer()), cached)
            }.getOrNull().orEmpty()
            if (parsed.isNotEmpty()) _entries.value = _entries.value + (repoId to parsed)
            parsed
        }

    /**
     * Load every subscription's catalog from the disk cache. After a process restart
     * memory is empty while "installed / not installed" has to be right away —
     * otherwise the catalog would look empty until the first refresh.
     */
    suspend fun primeFromCache() {
        storage.repositories.value.forEach { entriesOf(it.id) }
    }

    // ── Installing / removing sources ─────────────────────────────────────────

    data class InstallOutcome(
        val ok: Boolean,
        val recordId: String = "",
        val message: String = "",
        /** true = usable in the Finder right away; false = stored in the library. */
        val runnable: Boolean = false,
    )

    suspend fun install(entry: CompatRepoEntry): InstallOutcome {
        val payload = payloadFor(entry)
            ?: return InstallOutcome(
                ok = false,
                message = when {
                    entry.inlineKey.isNotBlank() && entry.payloadUrl.isBlank() ->
                        "Source is missing from the cached index — refresh the repository first."

                    else -> "Cannot download ${entry.payloadUrl.ifBlank { "(no payload URL in index)" }}"
                },
            )
        if (payload.isEmpty) return InstallOutcome(ok = false, message = "Empty payload for ${entry.name}")

        val record = buildRecord(entry, payload)
        val previous = provider.findRecord(entry.ecosystem, entry.key)
        // APK/JAR without a supported runtime is still stored: otherwise, once the
        // runtime exists the user would have to reinstall everything by hand.
        provider.install(record, payload)
        val runtime = CompatRuntime.of(entry.ecosystem)
        return InstallOutcome(
            ok = true,
            recordId = record.id,
            runnable = runtime.state == CompatRuntimeState.READY,
            message = when (runtime.state) {
                CompatRuntimeState.READY -> "${entry.name} is installed and available in Finder"
                else -> "${entry.name} is stored${if (previous == null) "" else " (updated)"} — ${runtime.badge}"
            },
        )
    }

    /** Install everything not installed yet (cached index, no extra network). */
    suspend fun installAll(repoId: String, limit: Int = 100): List<InstallOutcome> {
        val installed = storage.records.value.mapTo(HashSet()) { it.id }
        val todo = entriesOf(repoId)
            .filter { CompatInstallIds.recordId(it.ecosystem, it.key) !in installed }
            .take(limit)
        val out = ArrayList<InstallOutcome>(todo.size)
        for (entry in todo) {
            out += install(entry)
        }
        Timber.i("Compat: installAll(%s) installed %d/%d", repoId, out.count { it.ok }, todo.size)
        return out
    }

    fun uninstall(recordId: String) {
        provider.uninstall(recordId)
    }

    fun setSourceEnabled(recordId: String, enabled: Boolean) {
        provider.setEnabled(recordId, enabled)
    }

    enum class InstallState { NOT_INSTALLED, INSTALLED, DISABLED, UPDATE_AVAILABLE }

    fun installState(entry: CompatRepoEntry): InstallState {
        val record = provider.findRecord(entry.ecosystem, entry.key) ?: return InstallState.NOT_INSTALLED
        return when {
            !record.enabled -> InstallState.DISABLED
            hasUpdate(entry, record) -> InstallState.UPDATE_AVAILABLE
            else -> InstallState.INSTALLED
        }
    }

    private fun hasUpdate(entry: CompatRepoEntry, record: CompatInstallRecord): Boolean =
        entry.versionCode > record.versionCode ||
            (entry.versionCode == record.versionCode &&
                entry.version.isNotBlank() && entry.version != record.version)

    /** Update every outdated install of a repository; returns the number updated. */
    suspend fun updateAll(repoId: String): Int {
        val records = storage.records.value.filter { it.repositoryId == repoId }
        if (records.isEmpty()) return 0
        val byKey = records.associateBy { it.entryKey }
        var updated = 0
        for (entry in entriesOf(repoId)) {
            val record = byKey[entry.key] ?: continue
            if (!hasUpdate(entry, record)) continue
            if (install(entry).ok) updated++
        }
        return updated
    }

    // ── Internals ──────────────────────────────────────────────────────────────

    private suspend fun refreshInternal(repo: CompatRepository): CompatRepoResult =
        refreshMutex.withLock {
            _busy.value = _busy.value + repo.id
            try {
                doRefresh(repo)
            } catch (e: Exception) {
                Timber.e(e, "Compat: refresh of %s failed", repo.url)
                val failed = repo.copy(
                    lastRefreshAt = System.currentTimeMillis(),
                    lastError = e.message ?: e::class.java.simpleName,
                )
                storage.upsertRepository(failed)
                CompatRepoResult(repository = failed, ok = false, message = "Failed: ${failed.lastError}")
            } finally {
                _busy.value = _busy.value - repo.id
            }
        }

    private suspend fun doRefresh(repo: CompatRepository): CompatRepoResult {
        val plan = RepoUrlResolver.plan(repo.url)
        val resolved: ResolvedIndex? = when (plan) {
            is RepoUrlResolver.Plan.File -> resolveFile(plan.url, repo)
            is RepoUrlResolver.Plan.Git -> resolveGit(plan.target, repo)
            is RepoUrlResolver.Plan.Probe -> resolveProbe(plan.urls, repo)
            RepoUrlResolver.Plan.Invalid -> null
        }

        if (resolved == null) {
            val message = "No supported index found at ${repo.url}. " +
                "Tried: ${RepoUrlResolver.MAX_PROBES} standard file names. " +
                "For Mihon-style repos use the repo that holds index.json " +
                "(e.g. https://github.com/keiyoushi/extensions for keiyoushi/extensions-source)."
            diagnostics.setReportLine(
                repo.ecosystem ?: CompatEcosystem.LEGADO,
                "repository ${repo.url}: ${message.take(160)}",
            )
            val failed = repo.copy(
                lastRefreshAt = System.currentTimeMillis(),
                lastError = message,
            )
            storage.upsertRepository(failed)
            return CompatRepoResult(repository = failed, ok = false, message = message)
        }

        var entries = resolved.parsed.entries
        var nested = 0
        for (indexUrl in resolved.parsed.nestedIndexUrls) {
            val text = fetchText(indexUrl) ?: continue
            val inner = CompatRepoParsers.parseCloudStreamList(repo.id, indexUrl, text)
            if (inner.isNotEmpty()) {
                entries = entries + inner
                nested++
            }
        }

        val updated = repo.copy(
            indexUrl = resolved.indexUrl,
            kind = if (resolved.treeMode) RepoKind.TREE else RepoKind.INDEX,
            ecosystem = resolved.parsed.ecosystem,
            name = resolved.parsed.repoName.ifBlank { repo.url.displayNameFromUrl() },
            lastRefreshAt = System.currentTimeMillis(),
            entryCount = entries.size,
            lastError = null,
            ref = resolved.ref,
            signerFingerprint = resolved.parsed.signerFingerprint,
        )
        _entries.value = _entries.value + (repo.id to entries)
        storage.writeIndex(
            repo.id,
            "entries",
            json.encodeToString(ListSerializer(CompatRepoEntry.serializer()), entries),
        )
        storage.upsertRepository(updated)
        diagnostics.setReportLine(
            updated.ecosystem ?: CompatEcosystem.LEGADO,
            "${updated.ecosystem?.displayName}: ${entries.size} sources, " +
                "$nested nested lists, ${CompatRuntime.of(updated.ecosystem ?: CompatEcosystem.LEGADO).badge}",
        )
        Timber.i("Compat: %s → %s, %d sources", repo.url, updated.ecosystem, entries.size)
        return CompatRepoResult(
            repository = updated,
            entries = entries,
            ok = entries.isNotEmpty(),
            message = if (entries.isEmpty()) {
                "Index parsed but no usable source inside"
            } else {
                "${updated.ecosystem?.displayName}: ${entries.size} sources (${entries.count { CompatRuntime.isRunnable(it.ecosystem) }} runnable now)"
            },
        )
    }

    private data class ResolvedIndex(
        val indexUrl: String,
        val ref: String,
        val parsed: CompatRepoParsers.ParsedIndex,
        val treeMode: Boolean,
    )

    /** The URL names a concrete file: download it and trust its content. */
    private suspend fun resolveFile(url: String, repo: CompatRepository): ResolvedIndex? {
        val text = fetchText(url) ?: return null
        val parsed = parseWithPointer(repo.id, url, text) ?: return null
        if (parsed.entries.isEmpty() && parsed.nestedIndexUrls.isEmpty()) return null
        storage.writeIndex(repo.id, "index", text)
        return ResolvedIndex(url, refOf(url), parsed, treeMode = false)
    }

    /** Branch taken from a raw URL (`…/{owner}/{repo}/{ref}/…`); empty for other hosts. */
    private fun refOf(url: String): String =
        if (url.contains("raw.githubusercontent.com")) {
            url.substringAfter("raw.githubusercontent.com/", "").split('/').getOrNull(2).orEmpty()
        } else {
            ""
        }

    /**
     * Second-generation Mihon repositories put a `repo.json` in the root with an
     * `index_v2` pointer at `.pb` (protobuf). The JSON variant of the same file
     * sits next to it — we switch to that instead of parsing protobuf.
     */
    private suspend fun parseWithPointer(repoId: String, indexUrl: String, text: String): CompatRepoParsers.ParsedIndex? {
        val pointer = CompatRepoParsers.parseRepoPointer(text)
        if (pointer == null) return CompatRepoParsers.parse(repoId, indexUrl, text)
        for (candidate in pointer.indexUrls) {
            val url = CompatRepoParsers.absolutize(indexUrl, candidate)
            if (url.isBlank() || url == indexUrl) continue
            val inner = fetchText(url) ?: continue
            val parsed = CompatRepoParsers.parse(repoId, url, inner)
            if (parsed != null && parsed.entries.isNotEmpty()) {
                return parsed.copy(
                    repoName = parsed.repoName.ifBlank { pointer.repoName },
                    signerFingerprint = parsed.signerFingerprint.ifBlank { pointer.signerFingerprint },
                )
            }
        }
        val parsed = CompatRepoParsers.parse(repoId, indexUrl, text) ?: return null
        return parsed.copy(
            repoName = parsed.repoName.ifBlank { pointer.repoName },
            signerFingerprint = parsed.signerFingerprint.ifBlank { pointer.signerFingerprint },
        )
    }

    /**
     * Git repository: ask for the tree, look for an index, otherwise TREE scan.
     * For GitLab there is no API — only known file names under `HEAD`.
     */
    private suspend fun resolveGit(
        target: RepoUrlResolver.GitTarget,
        repo: CompatRepository,
    ): ResolvedIndex? {
        val host = target.host
        val owner = target.owner
        val name = target.repo
        val baseRef = target.ref ?: fetchDefaultBranch(host, owner, name) ?: "main"
        val dir = target.path.orEmpty().trimEnd('/')
        // A branch name may contain a slash (`plugins/v3.0.0`), and in a /tree/ URL
        // that is indistinguishable from a path. Try as-is first, then with the first
        // path segment folded into the branch name.
        val variants = buildList {
            add(baseRef to dir)
            if (target.ref != null && dir.contains('/')) {
                add("$baseRef/${dir.substringBefore('/')}" to dir.substringAfter('/'))
            }
        }
        for ((ref, subDir) in variants) {
            val resolved = resolveGitAt(host, owner, name, ref, subDir, repo)
            if (resolved != null) return resolved
        }
        return null
    }

    /** One attempt: a specific branch plus a directory inside it. */
    private suspend fun resolveGitAt(
        host: RepoUrlResolver.GitHost,
        owner: String,
        name: String,
        ref: String,
        dir: String,
        repo: CompatRepository,
    ): ResolvedIndex? {
        if (host == RepoUrlResolver.GitHost.GITHUB) {
            val tree = fetchTree(owner, name, ref, recursive = true)
                .let { if (it == null || it.truncated || it.files.isEmpty()) fetchTree(owner, name, ref, recursive = false) else it }
            // Files that look like an index first, in INDEX_FILE_NAMES priority order
            for (path in indexCandidates(tree, dir)) {
                val url = host.rawUrl(owner, name, ref, path)
                val text = fetchText(url) ?: continue
                val parsed = parseWithPointer(repo.id, url, text) ?: continue
                if (parsed.entries.isEmpty() && parsed.nestedIndexUrls.isEmpty()) continue
                storage.writeIndex(repo.id, "index", text)
                return ResolvedIndex(url, ref, parsed, treeMode = false)
            }
            // No index: every *.json in the repository may be a bookSource of its own
            if (tree != null) {
                val usable = RepoUrlResolver.treeFiles(tree.files.map { it.path to it.size })
                val entries = CompatRepoParsers.parseTree(
                    repoId = repo.id,
                    rawBase = host.rawUrl(owner, name, ref, dir),
                    files = usable,
                    ecosystem = repo.ecosystem,
                )
                if (entries.isNotEmpty()) {
                    val parsed = CompatRepoParsers.ParsedIndex(
                        ecosystem = entries.first().ecosystem,
                        repoName = "$owner/$name",
                        entries = entries,
                    )
                    return ResolvedIndex(
                        indexUrl = host.rawUrl(owner, name, ref, dir),
                        ref = ref,
                        parsed = parsed,
                        treeMode = true,
                    )
                }
            }
        }

        // GitLab and others: probe known file names in the branch (HEAD is valid on GitLab)
        val refOrDefault = if (host == RepoUrlResolver.GitHost.GITLAB) ref.ifBlank { "HEAD" } else ref
        val base = host.rawUrl(owner, name, refOrDefault, dir).trimEnd('/')
        for (fileName in CompatRepoParsers.INDEX_FILE_NAMES.take(RepoUrlResolver.MAX_PROBES)) {
            val url = "$base/$fileName"
            val text = fetchText(url) ?: continue
            val parsed = parseWithPointer(repo.id, url, text) ?: continue
            if (parsed.entries.isEmpty() && parsed.nestedIndexUrls.isEmpty()) continue
            storage.writeIndex(repo.id, "index", text)
            return ResolvedIndex(url, refOrDefault, parsed, treeMode = false)
        }
        return null
    }

    /**
     * Tree files that can act as the index: name matches [CompatRepoParsers.INDEX_FILE_NAMES]
     * (in priority order) and, when a directory is given, only inside it.
     * Shallower wins: a root `index.json` beats `sub/dir/index.json`.
     */
    private fun indexCandidates(tree: Tree?, dir: String): List<String> {
        if (tree == null) return emptyList()
        val byName = tree.files.groupBy { it.path.substringAfterLast('/').lowercase() }
        val out = ArrayList<String>()
        for (wanted in CompatRepoParsers.INDEX_FILE_NAMES) {
            val hits = byName[wanted.lowercase()] ?: continue
            val scoped = if (dir.isBlank()) hits else hits.filter { it.path.startsWith("$dir/") }
            val pool = scoped.ifEmpty { if (dir.isBlank()) hits else emptyList() }
            out += pool.sortedBy { it.path.count { c -> c == '/' } }.map { it.path }
        }
        return out.distinct()
    }

    /** Any host: try the URL and known names, the first one that parses wins. */
    private suspend fun resolveProbe(urls: List<String>, repo: CompatRepository): ResolvedIndex? {
        for (url in urls) {
            val text = fetchText(url) ?: continue
            val parsed = parseWithPointer(repo.id, url, text) ?: continue
            if (parsed.entries.isEmpty() && parsed.nestedIndexUrls.isEmpty()) continue
            storage.writeIndex(repo.id, "index", text)
            return ResolvedIndex(url, refOf(url), parsed, treeMode = false)
        }
        return null
    }

    /** Source payload: an inline object from the index cache, or a downloaded file. */
    private suspend fun payloadFor(entry: CompatRepoEntry): ByteArray? {
        if (entry.inlineKey.isNotBlank() && entry.payloadUrl.isBlank()) {
            val indexText = storage.readIndex(entry.repoId, "index")
                ?: refreshIndexText(entry.repoId)
                ?: return null
            val source = CompatRepoParsers.extractSourceJson(indexText, entry.inlineKey)
                ?: return null
            return source.toByteArray(Charsets.UTF_8)
        }
        if (entry.payloadUrl.isNotBlank()) return fetchBytes(entry.payloadUrl)
        return null
    }

    private suspend fun refreshIndexText(repoId: String): String? {
        val repo = storage.repositories.value.firstOrNull { it.id == repoId } ?: return null
        val url = repo.indexUrl.takeIf { it.isNotBlank() } ?: return null
        val text = fetchText(url) ?: return null
        storage.writeIndex(repoId, "index", text)
        return text
    }

    private fun buildRecord(entry: CompatRepoEntry, payload: ByteArray): CompatInstallRecord {
        val repo = storage.repositories.value.firstOrNull { it.id == entry.repoId }
        val fileName = "${storage.safeFileName(entry.key)}.${entry.payloadKind.fileExtension}"
        return CompatInstallRecord(
            id = CompatInstallIds.recordId(entry.ecosystem, entry.key),
            ecosystem = entry.ecosystem,
            name = entry.name,
            version = entry.version,
            language = entry.language,
            baseUrl = entry.baseUrl,
            contentType = entry.contentType,
            payloadPath = fileName,
            sourceUrl = entry.payloadUrl.ifBlank { repo?.indexUrl ?: entry.repoId },
            iconUrl = entry.iconUrl,
            enabled = true,
            versionCode = entry.versionCode,
            repositoryId = entry.repoId,
            entryKey = entry.key,
            nsfw = entry.nsfw,
            sha256 = sha256(payload),
        )
    }

    // ── Networking ─────────────────────────────────────────────────────────────

    private data class Tree(val files: List<CompatRepoParsers.TreeFile>, val truncated: Boolean)

    private suspend fun fetchText(url: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            networkClient.callWithTimeout(getRequest(url), CompatTimeouts.OPERATION_MS, true)
                .use { resp ->
                    if (!resp.isSuccessful) return@use null
                    resp.body.string()
                }
        }.onFailure {
            // A network failure is reported on the subscription itself (CompatRepository.lastError),
            // not in per-source diagnostics: no source failed here.
            Timber.d(it, "Compat: fetch %s failed", url)
        }.getOrNull()
    }

    private suspend fun fetchBytes(url: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            networkClient.callWithTimeout(getRequest(url), CompatTimeouts.OPERATION_MS * 2, true)
                .use { resp ->
                    if (!resp.isSuccessful) return@use null
                    resp.body.bytes()
                }
        }.getOrNull()
    }

    private suspend fun fetchDefaultBranch(
        host: RepoUrlResolver.GitHost,
        owner: String,
        repo: String,
    ): String? {
        if (host != RepoUrlResolver.GitHost.GITHUB) return null
        val text = fetchText(RepoUrlResolver.apiDefaultBranchUrl(owner, repo)) ?: return null
        val root = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: return null
        return (root["default_branch"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
    }

    private suspend fun fetchTree(
        owner: String,
        repo: String,
        ref: String,
        recursive: Boolean,
    ): Tree? {
        val text = fetchText(RepoUrlResolver.apiTreeUrl(owner, repo, ref, recursive)) ?: return null
        val root = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: return null
        val tree = root["tree"] as? JsonArray ?: return null
        val truncated = (root["truncated"] as? JsonPrimitive)?.contentOrNull?.toBoolean() == true
        val files = tree.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val path = (obj["path"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            val type = (obj["type"] as? JsonPrimitive)?.contentOrNull ?: "blob"
            if (type != "blob") return@mapNotNull null
            val size = obj["size"]?.let { (it as? JsonPrimitive)?.longOrNull } ?: 0L
            CompatRepoParsers.TreeFile(path, size)
        }
        return Tree(files, truncated)
    }

    private fun sha256(bytes: ByteArray): String = runCatching {
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }.getOrDefault("")

    companion object {
        /** Stable subscription id: host/path + short hash, so two `o/r` cannot collide. */
        fun repositoryId(url: String): String {
            val readable = url.removePrefix("https://").removePrefix("http://")
                .trimEnd('/')
                .let { if (it.length > 40) it.take(40) else it }
                .map { if (it.isLetterOrDigit() || it == '.' || it == '-' || it == '/') it else '_' }
                .joinToString("")
                .trim('_')
            val hash = MessageDigest.getInstance("SHA-256")
                .digest(url.trimEnd('/').toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
                .take(10)
            return "${readable.ifBlank { "repo" }}-$hash"
        }
    }
}

/** Install record id: one function for the manager and the UI ("is it installed?"). */
object CompatInstallIds {
    fun recordId(ecosystem: CompatEcosystem, key: String): String = "${ecosystem.id}:$key"
}

internal fun String.displayNameFromUrl(): String {
    val stripped = removePrefix("https://").removePrefix("http://").trimEnd('/')
    if (stripped.isBlank()) return this
    return stripUrlPrefix(stripped)
}

/** `github.com/o/r` → `o/r`; long paths are cut at the tail. */
private fun stripUrlPrefix(s: String): String {
    val body = s.removePrefix("github.com/").removePrefix("raw.githubusercontent.com/")
        .removePrefix("gitlab.com/")
    return if (body.length > 60) body.take(60) + "…" else body.ifBlank { s }
}

private fun CompatRepository.displayName(): String =
    name.ifBlank { url.displayNameFromUrl() }
