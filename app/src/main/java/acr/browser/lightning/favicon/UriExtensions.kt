@file:JvmName("FaviconUtils")

package acr.browser.lightning.favicon

import android.net.Uri

/**
 * Returns a valid [ValidUri] or `null` if the [String] provided was an invalid [ValidUri].
 */
fun Uri.toValidUri(): ValidUri? {
    val validScheme = scheme?.takeIf(String::isNotBlank)
    val validHost = host?.takeIf(String::isNotBlank)
    return if (validScheme != null && validHost != null) {
        ValidUri(validScheme, validHost)
    } else {
        null
    }
}

/**
 * A [Uri] that has both a non-blank [scheme] and a non-blank [host].
 */
data class ValidUri(val scheme: String, val host: String)
