package acr.browser.lightning.browser.data

import acr.browser.lightning.preference.UserPreferences
import android.app.Activity
import android.os.Build
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import javax.inject.Inject

/**
 * The default cookie administrator that sets cookie preferences for the default browser instance.
 */
class DefaultCookieAdministrator @Inject constructor(
    private val activity: Activity,
    private val userPreferences: UserPreferences
) : CookieAdministrator {
    override fun adjustCookieSettings() {
        val cookieManager = CookieManager.getInstance()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(activity)
        }
        cookieManager.setAcceptCookie(userPreferences.cookiesEnabled)
    }
}
