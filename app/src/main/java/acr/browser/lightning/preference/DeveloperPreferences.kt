package acr.browser.lightning.preference

import android.app.Application
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preferences related to development debugging.
 *
 * Created by anthonycr on 2/19/18.
 */
@Singleton
class DeveloperPreferences @Inject constructor(application: Application) {

    private val preferences = application.getSharedPreferences("developer_settings", 0)

    var useLeakCanary by BooleanPreference(LEAK_CANARY, false, preferences).delegate()

    var checkedForTor by BooleanPreference(INITIAL_CHECK_FOR_TOR, false, preferences).delegate()

    var checkedForI2P by BooleanPreference(INITIAL_CHECK_FOR_I2P, false, preferences).delegate()
}

private const val LEAK_CANARY = "leakCanary"
private const val INITIAL_CHECK_FOR_TOR = "checkForTor"
private const val INITIAL_CHECK_FOR_I2P = "checkForI2P"