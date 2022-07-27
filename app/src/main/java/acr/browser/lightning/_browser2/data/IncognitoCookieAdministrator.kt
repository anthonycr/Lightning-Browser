package acr.browser.lightning._browser2.data

import acr.browser.lightning.Capabilities
import acr.browser.lightning.isSupported
import acr.browser.lightning.preference.UserPreferences
import android.app.Activity
import android.os.Build
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import javax.inject.Inject

/**
 * The cookie administrator used to set cookie preferences for the incognito instance.
 */
class IncognitoCookieAdministrator @Inject constructor(
    private val activity: Activity,
    private val userPreferences: UserPreferences
) : CookieAdministrator {
    override fun adjustCookieSettings() {
        val cookieManager = CookieManager.getInstance()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(activity)
        }
        if (Capabilities.FULL_INCOGNITO.isSupported) {
            cookieManager.setAcceptCookie(userPreferences.cookiesEnabled)
        } else {
            cookieManager.setAcceptCookie(userPreferences.incognitoCookiesEnabled)
        }
    }
}
