package acr.browser.lightning.browser.data

import acr.browser.lightning.Capabilities
import acr.browser.lightning.isSupported
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.preference.datastore.getUnsafe
import android.webkit.CookieManager
import javax.inject.Inject

/**
 * The cookie administrator used to set cookie preferences for the incognito instance.
 */
class IncognitoCookieAdministrator @Inject constructor(
    private val userPreferences: UserPreferences,
    private val userPreferencesDataStore: UserPreferencesDataStore,
) : CookieAdministrator {
    override fun adjustCookieSettings() {
        val cookieManager = CookieManager.getInstance()
        if (Capabilities.FULL_INCOGNITO.isSupported) {
            cookieManager.setAcceptCookie(userPreferencesDataStore.cookiesEnabled.getUnsafe())
        } else {
            cookieManager.setAcceptCookie(userPreferences.incognitoCookiesEnabled)
        }
    }
}
