package my.dreamora.extension_compat.js

import my.dreamora.network.NetworkClient
import my.dreamora.network.getRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/** One request a JS plugin made through the host (`fetch`, `Client.get`, `fetchApi`...). */
data class JsHttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String>,
    val body: String?,
)

/** Raw result of a host request. Text decoding happens in the JS host, from [body]. */
class JsHttpResponse(
    val status: Int,
    val url: String,
    val headers: Map<String, String>,
    val body: ByteArray,
)

/** The network a JS runtime talks through. Production uses [NetworkJsHttp]. */
interface JsHttp {
    suspend fun execute(request: JsHttpRequest): JsHttpResponse
}

/**
 * Runs plugin requests through the app's [NetworkClient], so Cloudflare handling, the
 * cookie jar and the DNS setup apply exactly as they do for first-party sources.
 */
class NetworkJsHttp(private val networkClient: NetworkClient) : JsHttp {

    override suspend fun execute(request: JsHttpRequest): JsHttpResponse {
        val method = request.method.uppercase()
        val builder = getRequest(request.url)
        for ((name, value) in request.headers) builder.header(name, value)
        if (method != "GET" && method != "HEAD") {
            val mediaType = request.headers.entries
                .firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }
                ?.value
                ?.toMediaTypeOrNull()
            builder.method(method, (request.body ?: "").toRequestBody(mediaType))
        }
        networkClient.call(builder, followRedirects = true).use { response ->
            val headers = response.headers.toMultimap()
                .mapValues { (_, values) -> values.joinToString(", ") }
            return JsHttpResponse(
                status = response.code,
                url = response.request.url.toString(),
                headers = headers,
                body = response.body.bytes(),
            )
        }
    }
}
