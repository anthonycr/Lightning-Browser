package acr.browser.lightning.preference

import acr.browser.lightning.di.DevPrefs
import acr.browser.lightning.preference.delegates.booleanPreference
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preferences related to development debugging.
 *
 * Created by anthonycr on 2/19/18.
 */
@Singleton
class DeveloperPreferences @Inject constructor(
    @DevPrefs preferences: SharedPreferences
) {

    var useLeakCanary by preferences.booleanPreference(LEAK_CANARY, false)

    var checkedForTor by preferences.booleanPreference(INITIAL_CHECK_FOR_TOR, false)

    var checkedForI2P by preferences.booleanPreference(INITIAL_CHECK_FOR_I2P, false)
}

private const val LEAK_CANARY = "leakCanary"
private const val INITIAL_CHECK_FOR_TOR = "checkForTor"
private const val INITIAL_CHECK_FOR_I2P = "checkForI2P"
