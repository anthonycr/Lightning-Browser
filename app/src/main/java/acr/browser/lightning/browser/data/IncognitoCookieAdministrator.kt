package acr.browser.lightning.browser.data

import acr.browser.lightning.preference.UserPreferencesDataStore
import android.webkit.CookieManager
import javax.inject.Inject

/**
 * The cookie administrator used to set cookie preferences for the incognito instance.
 */
class IncognitoCookieAdministrator @Inject constructor(
    private val userPreferencesDataStore: UserPreferencesDataStore,
) : CookieAdministrator {
    override suspend fun adjustCookieSettings() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(userPreferencesDataStore.cookiesEnabled.get())
    }
}
