package acr.browser.lightning.utils

import android.net.Uri

/**
 * Returns a valid [Uri] or `null` if the [url] provided was invalid.
 */
fun safeUri(url: String): Uri? {
    if (url.isBlank()) {
        return null
    }

    val uri = Uri.parse(url)

    return if (uri.scheme.isNullOrBlank() || uri.host.isNullOrBlank()) {
        null
    } else {
        uri
    }
}

/**
 * Returns the domain name represented by the [url] or `null` if it is not a valid URL.
 */
fun domainForUrl(url: String?): String? =
        Uri.parse(url ?: "")?.host.takeIf { it?.isNotBlank() == true }