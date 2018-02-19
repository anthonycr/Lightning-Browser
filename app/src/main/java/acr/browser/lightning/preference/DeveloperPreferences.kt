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
}

private const val LEAK_CANARY = "leakCanary"