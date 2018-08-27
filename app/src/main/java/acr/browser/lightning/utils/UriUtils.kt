package acr.browser.lightning.utils

import android.net.Uri

/**
 * Returns the domain name represented by the [url] or `null` if it is not a valid URL.
 */
fun domainForUrl(url: String?): String? =
        Uri.parse(url ?: "")?.host.takeIf { it?.isNotBlank() == true }