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
    when (userAgentChoice.getUnsafe()) {
        UserAgentChoice.DEFAULT -> WebSettings.getDefaultUserAgent(application)
        UserAgentChoice.DESKTOP -> DESKTOP_USER_AGENT
        UserAgentChoice.MOBILE -> MOBILE_USER_AGENT
        UserAgentChoice.CUSTOM -> userAgentString.getUnsafe().takeIf(String::isNotEmpty) ?: " "
    }

suspend fun UserPreferencesDataStore.userAgent(defaultUserAgent: String): String =
    when (userAgentChoice.get()) {
        UserAgentChoice.DEFAULT -> defaultUserAgent
        UserAgentChoice.DESKTOP -> DESKTOP_USER_AGENT
        UserAgentChoice.MOBILE -> MOBILE_USER_AGENT
        UserAgentChoice.CUSTOM -> userAgentString.get().takeIf(String::isNotEmpty).orEmpty()
    }

enum class UserAgentChoice(override val value: Int) : IntEnum {
    DEFAULT(1),
    DESKTOP(2),
    MOBILE(3),
    CUSTOM(4),
}
