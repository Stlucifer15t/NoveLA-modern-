package my.noveldokusha.extension_compat.core

import my.noveldokusha.core.atomicWrite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.util.concurrent.ReentrantLock

/**
 * Персистентное хранилище compat-расширений:
 *
 * ```
 * filesDir/compat/
 *   registry.json                 — список CompatInstallRecord
 *   <ecosystem>/<payloadPath>     — сами файлы (js/json/apk/cs3)
 * ```
 *
 * Всё пишется атомарно ([atomicWrite]); чтение registry терпимо к битому JSON
 * (возвращает пустой список и логирует — не роняет приложение).
 *
 * Инжектится через `@Provides` в di/CompatModule (конструктор принимает File,
 * Dagger сам его не соберёт), поэтому у конструктора нет `@Inject` — иначе
 * была бы дублирующаяся биндинг-ошибка Dagger.
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
    private val mutex = java.util.concurrent.ReentrantLock()

    private val _records = MutableStateFlow<List<CompatInstallRecord>>(emptyList())
    val records: StateFlow<List<CompatInstallRecord>> = _records.asStateFlow()

    init {
        loadRegistry()
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
        // Удаляем payload
        _records.value.find { it.id == id }?.let { /* already filtered */ }
    }

    fun setEnabled(id: String, enabled: Boolean) {
        val current = _records.value.map {
            if (it.id == id) it.copy(enabled = enabled) else it
        }
        saveRegistry(current)
    }

    /** Файл payload'а записи; parent создаётся. */
    fun payloadFile(record: CompatInstallRecord): File {
        val file = File(File(rootDir, record.ecosystem.id), record.payloadPath)
        file.parentFile?.mkdirs()
        return file
    }

    /** Сохранить бинарный payload, вернуть относительный путь. */
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

    /** Очистка всего storage (uninstall-all / отладка). */
    fun clearAll() {
        saveRegistry(emptyList())
        rootDir.listFiles()?.forEach { dir ->
            if (dir.isDirectory) dir.deleteRecursively() else dir.delete()
        }
    }

    companion object {
        /** Имя каталога внутри filesDir. */
        const val DIR_NAME = "compat"
    }
}
