package my.noveldokusha.extension_compat.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Diagnostics for compat loading and calls: per-source error isolation,
 * timeouts and a compatibility report for the UI (Extensions → diagnostics).
 */
@Singleton
class CompatDiagnostics @Inject constructor() {

    @Serializable
    data class CompatError(
        val sourceId: String,
        val ecosystem: String,
        val operation: String,
        val message: String,
        val timestamp: Long = System.currentTimeMillis(),
        val timedOut: Boolean = false,
    )

    private val _errors = MutableStateFlow<List<CompatError>>(emptyList())
    val errors: StateFlow<List<CompatError>> = _errors.asStateFlow()

    private val _compatReport = MutableStateFlow<Map<String, String>>(emptyMap())
    val compatReport: StateFlow<Map<String, String>> = _compatReport.asStateFlow()

    fun recordError(
        sourceId: String,
        ecosystem: CompatEcosystem,
        operation: String,
        error: Throwable,
        timedOut: Boolean = false,
    ) {
        val entry = CompatError(
            sourceId = sourceId,
            ecosystem = ecosystem.id,
            operation = operation,
            message = error.message ?: error::class.java.simpleName,
            timedOut = timedOut,
        )
        val current = _errors.value
        // Keep the history bounded: last 100 entries
        _errors.value = (current + entry).takeLast(100)
    }

    fun clearErrors(sourceId: String? = null) {
        _errors.value = if (sourceId == null) emptyList()
        else _errors.value.filterNot { it.sourceId == sourceId }
    }

    /** Update the report line of an ecosystem (e.g. "Legado: 12 sources, 2 errors"). */
    fun setReportLine(ecosystem: CompatEcosystem, line: String) {
        _compatReport.value = _compatReport.value + (ecosystem.id to line)
    }

    fun clearReport() {
        _compatReport.value = emptyMap()
    }
}

/**
 * Compat-layer errors, kept apart from ordinary per-source network errors.
 */
open class CompatException(
    message: String,
    cause: Throwable? = null,
    /** Operation: "load", "search", "details", "chapters", "pages", "video"... */
    val operation: String = "load",
) : Exception(message, cause)

class CompatTimeoutException(operation: String, timeoutMs: Long) :
    CompatException("Compat operation '$operation' timed out after ${timeoutMs}ms", operation = operation)

object CompatTimeouts {
    /** Source load/initialization (dex/JS engine). */
    const val LOAD_MS = 30_000L
    /** One network operation of a source (search/details/...). */
    const val OPERATION_MS = 30_000L
    /** Video extraction (can be heavy: several hosts). */
    const val VIDEO_MS = 45_000L
}
