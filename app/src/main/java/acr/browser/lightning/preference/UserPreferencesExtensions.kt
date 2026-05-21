package acr.browser.lightning.preference

import acr.browser.lightning.constant.DESKTOP_USER_AGENT
import acr.browser.lightning.constant.MOBILE_USER_AGENT
import acr.browser.lightning.preference.datastore.getUnsafe
import android.app.Application
import android.webkit.WebSettings

/**
 * Return the user agent chosen by the user or the custom user agent entered by the user.
 */
fun UserPreferencesDataStore.userAgent(application: Application): String =
    when (val choice = userAgentChoice.getUnsafe()) {
        1 -> WebSettings.getDefaultUserAgent(application)
        2 -> DESKTOP_USER_AGENT
        3 -> MOBILE_USER_AGENT
        4 -> userAgentString.getUnsafe().takeIf(String::isNotEmpty) ?: " "
        else -> throw UnsupportedOperationException("Unknown userAgentChoice: $choice")
    }

suspend fun UserPreferencesDataStore.userAgent(defaultUserAgent: String): String =
    when (val choice = userAgentChoice.get()) {
        1 -> defaultUserAgent
        2 -> DESKTOP_USER_AGENT
        3 -> MOBILE_USER_AGENT
        4 -> userAgentString.get().takeIf(String::isNotEmpty).orEmpty()
        else -> throw UnsupportedOperationException("Unknown userAgentChoice: $choice")
    }
