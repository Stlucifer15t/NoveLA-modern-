package my.dreamora.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Extra request headers for image URLs that an extension handed to the app.
 *
 * Extensions declare these themselves: LNReader plugins through `imageRequestInit.headers`
 * (usually `Referer: site`), Mangayomi sources through `getHeaders(url)`. Many CDNs refuse an
 * image without the site as Referer, so the app must send them for the image URL, not only
 * for the extension's own page requests.
 *
 * Headers are keyed by the exact (normalized) image URL. Only requests for a URL that was
 * registered get them, so nothing else on the same host is affected.
 */
object ImageRequestHeaders {

    private const val MAX_URLS = 4096

    private val byUrl = object : LinkedHashMap<String, Map<String, String>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Map<String, String>>?): Boolean =
            size > MAX_URLS
    }

    /** Remembers [headers] for [url]. Empty header sets are not stored. */
    fun remember(url: String, headers: Map<String, String>) {
        if (headers.isEmpty()) return
        val key = normalize(url) ?: return
        synchronized(byUrl) { byUrl[key] = headers }
    }

    /** The headers registered for [url], or null when there are none. */
    fun headersFor(url: String): Map<String, String>? {
        val key = normalize(url) ?: return null
        return synchronized(byUrl) { byUrl[key] }
    }

    private fun normalize(url: String): String? = url.toHttpUrlOrNull()?.toString()
}

/** Adds the headers registered in [ImageRequestHeaders] to matching requests. */
internal class ImageRequestHeadersInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val extra = ImageRequestHeaders.headersFor(request.url.toString())
            ?: return chain.proceed(request)
        val builder = request.newBuilder()
        for ((name, value) in extra) builder.header(name, value)
        return chain.proceed(builder.build())
    }
}
