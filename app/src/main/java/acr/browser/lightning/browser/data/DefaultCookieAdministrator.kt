package acr.browser.lightning.browser.data

import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.preference.datastore.getUnsafe
import android.webkit.CookieManager
import javax.inject.Inject

/**
 * The default cookie administrator that sets cookie preferences for the default browser instance.
 */
class DefaultCookieAdministrator @Inject constructor(
    private val userPreferencesDataStore: UserPreferencesDataStore
) : CookieAdministrator {
    override fun adjustCookieSettings() {
        CookieManager.getInstance().setAcceptCookie(userPreferencesDataStore.cookiesEnabled.getUnsafe())
    }
}
