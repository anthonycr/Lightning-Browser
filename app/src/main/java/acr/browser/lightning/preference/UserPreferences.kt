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

    var webRtcEnabled by BooleanPreference(PreferenceManager.Name.WEB_RTC, false, preferences).delegate()
}