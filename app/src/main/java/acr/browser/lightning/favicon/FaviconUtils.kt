@file:JvmName("FaviconUtils")

package acr.browser.lightning.favicon

import android.net.Uri

/**
 * Returns a valid [Uri] or `null` if the [url] provided was invalid.
 */
fun safeUri(url: String): Uri? {
    if (url.isBlank()) {
        return null
    }

    val uri = Uri.parse(url)

    return if (uri.scheme.isBlank() || uri.host.isBlank()) {
        null
    } else {
        uri
    }
}

/**
 * Requires that a [Uri] be safe for use. The [Uri.getScheme] and [Uri.getHost] must not be
 * `null` or blank.
 */
fun requireUriSafe(uri: Uri) {
    if (uri.scheme?.isNotBlank() != true || uri.host?.isNotBlank() != true) {
        throw RuntimeException("Unsafe uri provided")
    }
}
