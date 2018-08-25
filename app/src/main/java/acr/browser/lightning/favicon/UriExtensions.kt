@file:JvmName("FaviconUtils")

package acr.browser.lightning.favicon

import android.net.Uri

/**
 * Validates that a [Uri] be safe for use. The [Uri.getScheme] and [Uri.getHost] must not be `null`
 * or blank, otherwise an [IllegalArgumentException] will be thrown.
 */
fun Uri.validateUri() {
    if (!this.isValid()) {
        throw IllegalArgumentException("Unsafe uri provided")
    }
}

/**
 * Returns true if the [Uri] is valid, false otherwise.
 */
fun Uri.isValid(): Boolean = scheme?.isNotBlank() == true && host?.isNotBlank() == true

/**
 * Returns a valid [Uri] or `null` if the [String] provided was an invalid [Uri].
 */
fun String.toValidUri(): Uri? = Uri.parse(this).takeIf(Uri::isValid)