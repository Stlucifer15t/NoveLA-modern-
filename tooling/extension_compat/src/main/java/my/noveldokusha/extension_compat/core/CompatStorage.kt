package my.noveldokusha.extension_compat.core

import my.noveldokusha.core.atomicWrite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.util.concurrent.locks.ReentrantLock

/**
 * Persistent storage for compat extensions:
 *
 * ```
 * filesDir/compat/
 *   registry.json                 — CompatInstallRecord list (what is installed)
 *   repos.json                    — CompatRepository list (what the user subscribed to)
 *   indexes/<repoId>.index.json   — raw subscription index (offline browsing, inline bookSource)
 *   indexes/<repoId>.entries.json — parsed subscription catalog
 *   <ecosystem>/<payloadPath>     — the payload files themselves (js/json/apk/cs3)
 * ```
 *
 * The record registry and the subscription list are separate files on purpose: a
 * subscription can be removed while its installed sources stay (see remove()).
 *
 * Everything is written atomically ([atomicWrite]); reading the registry tolerates
 * (broken JSON returns an empty list and logs instead of crashing the app).
 *
 * Injected via `@Provides` in di/CompatModule (the constructor takes a File,
 * Dagger cannot resolve it), so the constructor has no `@Inject` — adding one
 * would produce a duplicate Dagger binding error.
 */
class CompatStorage(
    private val rootDir: File,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val registryFile = File(rootDir, "registry.json")
    private val reposFile = File(rootDir, "repos.json")

    /**
     * Index cache directory: raw index + parsed entries per repository.
     * Declared before `init`: initializers and init blocks run in declaration order.
     */
    private val indexesDir = File(rootDir, "indexes")
    private val mutex = java.util.concurrent.locks.ReentrantLock()

    private val _records = MutableStateFlow<List<CompatInstallRecord>>(emptyList())
    val records: StateFlow<List<CompatInstallRecord>> = _records.asStateFlow()

    /** Subscriptions to third-party repositories (see [CompatRepository]). */
    private val _repositories = MutableStateFlow<List<CompatRepository>>(emptyList())
    val repositories: StateFlow<List<CompatRepository>> = _repositories.asStateFlow()

    init {
        loadRegistry()
        loadRepositories()
    }

    // ── Repositories ───────────────────────────────────────────────────────────

    fun loadRepositories(): List<CompatRepository> {
        mutex.lock()
        try {
            if (!reposFile.exists()) {
                _repositories.value = emptyList()
                return emptyList()
            }
            val parsed = json.decodeFromString(
                ListSerializer(CompatRepository.serializer()),
                reposFile.readText(),
            )
            _repositories.value = parsed
            return parsed
        } catch (e: Exception) {
            Timber.e(e, "CompatStorage: failed to read repos.json")
            _repositories.value = emptyList()
            return emptyList()
        } finally {
            mutex.unlock()
        }
    }

    private fun saveRepositories(repos: List<CompatRepository>) {
        mutex.lock()
        try {
            rootDir.mkdirs()
            val text = json.encodeToString(ListSerializer(CompatRepository.serializer()), repos)
            atomicWrite(reposFile, text.toByteArray(Charsets.UTF_8))
            _repositories.value = repos
        } catch (e: Exception) {
            Timber.e(e, "CompatStorage: failed to save repos.json")
        } finally {
            mutex.unlock()
        }
    }

    fun upsertRepository(repo: CompatRepository) {
        val current = _repositories.value.toMutableList()
        val idx = current.indexOfFirst { it.id == repo.id }
        if (idx >= 0) current[idx] = repo else current.add(repo)
        saveRepositories(current)
    }

    /** Remove a subscription and its cache; payload files stay until the caller removes them. */
    fun removeRepository(id: String) {
        saveRepositories(_repositories.value.filterNot { it.id == id })
        indexFile(id, "index").delete()
        indexFile(id, "entries").delete()
    }

    fun setRepositoryEnabled(id: String, enabled: Boolean) {
        saveRepositories(_repositories.value.map {
            if (it.id == id) it.copy(enabled = enabled) else it
        })
    }

    /**
     * Repository cache file. `suffix` = "index" (raw index text — inline bookSource
     * objects are extracted from it at install time) or "entries" (parsed list).
     * The extension is always .json: the content may be a .txt subscription, it does not matter.
     */
    fun indexFile(repoId: String, suffix: String): File {
        indexesDir.mkdirs()
        return File(indexesDir, "${safeFileName(repoId)}.$suffix.json")
    }

    fun writeIndex(repoId: String, suffix: String, text: String) {
        runCatching { atomicWrite(indexFile(repoId, suffix), text.toByteArray(Charsets.UTF_8)) }
            .onFailure { Timber.e(it, "CompatStorage: failed to write index cache $suffix for $repoId") }
    }

    fun readIndex(repoId: String, suffix: String): String? =
        runCatching {
            val file = indexFile(repoId, suffix)
            if (file.exists()) file.readText() else null
        }.getOrNull()

    /** Payload and cache file names: safe characters only. */
    fun safeFileName(raw: String): String = buildString(raw.length.coerceAtMost(120)) {
        for (c in raw) {
            when {
                c.isLetterOrDigit() || c == '.' || c == '-' || c == '_' -> append(c)
                c == '/' || c == '\\' -> append('_')
                else -> Unit
            }
            if (length >= 120) break
        }
        if (length == 0) append("source")
    }

    fun loadRegistry(): List<CompatInstallRecord> {
        mutex.lock()
        try {
            if (!registryFile.exists()) {
                _records.value = emptyList()
                return emptyList()
            }
            val text = registryFile.readText()
            val parsed = json.decodeFromString(ListSerializer(CompatInstallRecord.serializer()), text)
            _records.value = parsed
            return parsed
        } catch (e: Exception) {
            Timber.e(e, "CompatStorage: failed to read registry")
            _records.value = emptyList()
            return emptyList()
        } finally {
            mutex.unlock()
        }
    }

    private fun saveRegistry(records: List<CompatInstallRecord>) {
        mutex.lock()
        try {
            rootDir.mkdirs()
            val text = json.encodeToString(ListSerializer(CompatInstallRecord.serializer()), records)
            atomicWrite(registryFile, text.toByteArray(Charsets.UTF_8))
            _records.value = records
        } catch (e: Exception) {
            Timber.e(e, "CompatStorage: failed to save registry")
        } finally {
            mutex.unlock()
        }
    }

    fun getRecord(id: String): CompatInstallRecord? =
        _records.value.find { it.id == id }

    fun upsert(record: CompatInstallRecord) {
        val current = _records.value.toMutableList()
        val idx = current.indexOfFirst { it.id == record.id }
        if (idx >= 0) current[idx] = record else current.add(record)
        saveRegistry(current)
    }

    fun remove(id: String) {
        val current = _records.value.filterNot { it.id == id }
        saveRegistry(current)
        // Delete the payload
        _records.value.find { it.id == id }?.let { /* already filtered */ }
    }

    fun setEnabled(id: String, enabled: Boolean) {
        val current = _records.value.map {
            if (it.id == id) it.copy(enabled = enabled) else it
        }
        saveRegistry(current)
    }

    /** Payload file of a record; the parent directory is created. */
    fun payloadFile(record: CompatInstallRecord): File {
        val file = File(File(rootDir, record.ecosystem.id), record.payloadPath)
        file.parentFile?.mkdirs()
        return file
    }

    /** Store a binary payload and return its relative path. */
    fun writePayload(ecosystem: CompatEcosystem, relativePath: String, bytes: ByteArray): String {
        val file = File(File(rootDir, ecosystem.id), relativePath)
        file.parentFile?.mkdirs()
        atomicWrite(file, bytes)
        return relativePath
    }

    fun readPayload(record: CompatInstallRecord): ByteArray? {
        val file = payloadFile(record)
        return if (file.exists()) file.readBytes() else null
    }

    fun deletePayload(record: CompatInstallRecord) {
        payloadFile(record).delete()
    }

    /** Wipe all storage (uninstall-all / debugging). */
    fun clearAll() {
        saveRegistry(emptyList())
        saveRepositories(emptyList())
        rootDir.listFiles()?.forEach { dir ->
            if (dir.isDirectory) dir.deleteRecursively() else dir.delete()
        }
    }

    companion object {
        /** Directory name inside filesDir. */
        const val DIR_NAME = "compat"
    }
}
