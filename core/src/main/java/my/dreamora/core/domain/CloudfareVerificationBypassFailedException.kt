package my.dreamora.core.domain

import java.io.IOException


class WebViewCookieManagerInitializationFailedException :
    IOException("Webview cookies not found for websited")

class CloudfareVerificationBypassFailedException(message: String? = null) :
    IOException(message ?: "Cloudflare verification failed")
