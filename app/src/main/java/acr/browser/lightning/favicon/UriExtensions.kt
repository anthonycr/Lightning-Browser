@file:JvmName("FaviconUtils")

package acr.browser.lightning.favicon

import android.net.Uri
import androidx.core.net.toUri

/**
 * Returns a valid [ValidUri] or `null` if the [String] provided was an invalid [ValidUri].
 */
fun String.toValidUri(): ValidUri? = toUri().let {
    val scheme = it.scheme
    val host = it.host
    if (scheme?.isNotBlank() == true && host?.isNotBlank() == true) {
        ValidUri(scheme, host)
    } else {
        null
    }
}

/**
 * A [Uri] that has both a non-blank [scheme] and a non-blank [host].
 */
data class ValidUri(val scheme: String, val host: String)
