package my.noveldokusha.extension_compat.core

import android.content.Context
import timber.log.Timber

/**
 * Security and isolation configuration for the compat layer.
 * Enforces timeouts, resource limits, and sandboxing for third-party extensions.
 */
class CompatSecurity @javax.inject.Inject constructor(
    private val context: Context,
) {

    // ── Timeouts ──────────────────────────────────────────────────────────────

    /** Maximum time to load an extension (including ClassLoader init, JS eval, etc.) */
    const val LOAD_TIMEOUT_MS = 10_000L

    /** Maximum time for a single source method call (search, getDetail, etc.) */
    const val METHOD_TIMEOUT_MS = 15_000L

    /** Maximum time for HTTP requests made by extensions */
    const val HTTP_TIMEOUT_MS = 10_000L

    /** Maximum time for JavaScript execution (per rule/eval) */
    const val JS_TIMEOUT_MS = 8_000L

    // ── Resource Limits ───────────────────────────────────────────────────────

    /** Maximum heap memory for QuickJS runtime (bytes) */
    const val QUICKJS_MAX_HEAP = 16 * 1024 * 1024 // 16 MB

    /** Maximum stack size for QuickJS (bytes) */
    const val QUICKJS_MAX_STACK = 4 * 1024 * 1024 // 4 MB

    /** Maximum number of concurrent extension method calls */
    const val MAX_CONCURRENT_CALLS = 10

    /** Maximum response body size for extension HTTP requests (bytes) */
    const val MAX_RESPONSE_SIZE = 10 * 1024 * 1024 // 10 MB

    // ── Network Restrictions ──────────────────────────────────────────────────

    /** Allowed URL schemes for extension requests */
    val allowedSchemes = setOf("http", "https")

    /** Blocked IP ranges (private/local) */
    val blockedIpRanges = listOf(
        "10.0.0.0/8",
        "172.16.0.0/12",
        "192.168.0.0/16",
        "127.0.0.0/8",
        "169.254.0.0/16",
        "::1/128",
        "fe80::/10",
    )

    /** Whether to follow redirects */
    val followRedirects = true

    /** Maximum redirects to follow */
    val maxRedirects = 5

    // ── ClassLoader Isolation ─────────────────────────────────────────────────

    /** Packages that extensions are allowed to access via reflection */
    val allowedReflectionPackages = setOf(
        "java.lang",
        "java.util",
        "java.io",
        "java.net",
        "java.text",
        "java.time",
        "kotlin",
        "kotlinx.coroutines",
        "kotlinx.serialization",
        "okhttp3",
        "org.jsoup",
        "javax.xml.xpath",
        "org.w3c.dom",
        "com.jayway.jsonpath",
        "org.mozilla.javascript",
        "io.github.dokar3.quickjs",
    )

    /** Packages that are explicitly blocked */
    val blockedReflectionPackages = setOf(
        "android.",
        "dalvik.",
        "java.lang.reflect",
        "java.security",
        "javax.crypto",
        "sun.",
        "com.android.",
        "my.noveldokusha.", // Prevent access to app internals
    )

    // ── File System Isolation ─────────────────────────────────────────────────

    /** Extension-specific cache directory */
    fun getExtensionCacheDir(extensionId: String): java.io.File {
        val baseDir = java.io.File(context.cacheDir, "compat_extensions")
        baseDir.mkdirs()
        return java.io.File(baseDir, extensionId.replace("/", "_").replace("#", "_"))
    }

    /** Extension-specific data directory (private) */
    fun getExtensionDataDir(extensionId: String): java.io.File {
        val baseDir = java.io.File(context.filesDir, "compat_extensions")
        baseDir.mkdirs()
        return java.io.File(baseDir, extensionId.replace("/", "_").replace("#", "_"))
    }

    // ── Validation ────────────────────────────────────────────────────────────

    /**
     * Validate a URL before allowing an extension to request it.
     */
    fun validateUrl(url: String): Boolean {
        return try {
            val parsed = java.net.URL(url)
            // Check scheme
            if (parsed.protocol !in allowedSchemes) return false
            // Check host is not blocked IP range
            val host = parsed.host
            if (isBlockedHost(host)) return false
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isBlockedHost(host: String): Boolean {
        // Simple check - in production would use proper CIDR matching
        val ip = try {
            java.net.InetAddress.getByName(host)
        } catch (e: Exception) {
            return false // Hostname, not IP - allow
        }
        val ipString = ip.hostAddress
        return blockedIpRanges.any { range ->
            // Simplified check - real implementation would use SubnetUtils
            ipString.startsWith(range.substringBefore("/"))
        }
    }

    /**
     * Validate a class name for reflection access.
     */
    fun validateClassName(className: String): Boolean {
        // Check blocked packages first
        if (blockedReflectionPackages.any { className.startsWith(it) }) {
            return false
        }
        // Check allowed packages
        return allowedReflectionPackages.any { className.startsWith(it) }
    }

    /**
     * Sanitize extension-provided headers.
     */
    fun sanitizeHeaders(headers: Map<String, String>): Map<String, String> {
        val blockedHeaders = setOf(
            "host",
            "content-length",
            "transfer-encoding",
            "connection",
            "upgrade",
            "cookie",
            "set-cookie",
            "authorization",
            "proxy-authorization",
        )
        return headers.filterKeys { key ->
            !blockedHeaders.contains(key.lowercase())
        }
    }

    /**
     * Log security event for audit trail.
     */
    fun logSecurityEvent(extensionId: String, event: String, details: String = "") {
        Timber.tag("CompatSecurity").w("[$extensionId] $event ${if (details.isNotEmpty()) ": $details" else ""}")
    }
}

/**
 * Security policy for a specific ecosystem.
 */
sealed class EcosystemSecurityPolicy {
    data class Tachiyomi(
        override val loadTimeoutMs: Long = CompatSecurity.LOAD_TIMEOUT_MS,
        override val methodTimeoutMs: Long = CompatSecurity.METHOD_TIMEOUT_MS,
        override val verifySignatures: Boolean = true,
    ) : EcosystemSecurityPolicy()

    data class Legado(
        override val loadTimeoutMs: Long = CompatSecurity.LOAD_TIMEOUT_MS,
        override val methodTimeoutMs: Long = CompatSecurity.METHOD_TIMEOUT_MS,
        override val allowJavaScript: Boolean = true,
    ) : EcosystemSecurityPolicy()

    data class LNReader(
        override val loadTimeoutMs: Long = CompatSecurity.LOAD_TIMEOUT_MS,
        override val methodTimeoutMs: Long = CompatSecurity.METHOD_TIMEOUT_MS,
        override val quickJsMaxHeap: Long = CompatSecurity.QUICKJS_MAX_HEAP,
        override val quickJsMaxStack: Long = CompatSecurity.QUICKJS_MAX_STACK,
    ) : EcosystemSecurityPolicy()

    data class Mangayomi(
        override val loadTimeoutMs: Long = CompatSecurity.LOAD_TIMEOUT_MS,
        override val methodTimeoutMs: Long = CompatSecurity.METHOD_TIMEOUT_MS,
        override val quickJsMaxHeap: Long = CompatSecurity.QUICKJS_MAX_HEAP,
        override val quickJsMaxStack: Long = CompatSecurity.QUICKJS_MAX_STACK,
    ) : EcosystemSecurityPolicy()

    data class CloudStream(
        override val loadTimeoutMs: Long = CompatSecurity.LOAD_TIMEOUT_MS,
        override val methodTimeoutMs: Long = CompatSecurity.METHOD_TIMEOUT_MS,
        override val verifySignatures: Boolean = false, // CloudStream plugins are not typically signed
    ) : EcosystemSecurityPolicy()

    abstract val loadTimeoutMs: Long
    abstract val methodTimeoutMs: Long
}

/**
 * Security context for an extension execution.
 */
data class ExtensionSecurityContext(
    val extensionId: String,
    val ecosystem: CompatEcosystem,
    val startTime: Long = System.currentTimeMillis(),
    val callCount: Int = 0,
    val totalCpuTimeMs: Long = 0,
) {
    fun checkTimeouts(policy: EcosystemSecurityPolicy): Boolean {
        val elapsed = System.currentTimeMillis() - startTime
        return elapsed < policy.loadTimeoutMs && callCount < CompatSecurity.MAX_CONCURRENT_CALLS
    }

    fun incrementCallCount(): ExtensionSecurityContext = copy(callCount = callCount + 1)
}