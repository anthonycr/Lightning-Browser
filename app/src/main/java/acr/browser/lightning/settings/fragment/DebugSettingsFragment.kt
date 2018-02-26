package acr.browser.lightning.settings.fragment

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.R
import acr.browser.lightning.preference.DeveloperPreferences
import acr.browser.lightning.utils.Utils
import android.os.Bundle
import javax.inject.Inject

class DebugSettingsFragment : AbstractSettingsFragment() {

    @Inject internal lateinit var developerPreferences: DeveloperPreferences

    override fun providePreferencesXmlResource() = R.xml.preference_debug

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BrowserApp.appComponent.inject(this)

        togglePreference(
                preference = LEAK_CANARY,
                isChecked = developerPreferences.useLeakCanary,
                onCheckChange = {
                    activity?.let {
                        Utils.showSnackbar(it, R.string.app_restart)
                    }
                    developerPreferences.useLeakCanary = it
                }
        )
    }

    companion object {
        private const val LEAK_CANARY = "leak_canary_enabled"
    }
}
