package acr.browser.lightning.preference

import android.app.Application
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The user's preferences.
 */
@Singleton
class UserPreferences @Inject constructor(application: Application) {

    private val preferences = application.getSharedPreferences("settings", 0)

    var webRtcEnabled by BooleanPreference(WEB_RTC, false, preferences).delegate()

    var adBlockEnabled by BooleanPreference(BLOCK_ADS, false, preferences).delegate()
}

private const val WEB_RTC = "webRtc"
private const val BLOCK_ADS = "AdBlock"
