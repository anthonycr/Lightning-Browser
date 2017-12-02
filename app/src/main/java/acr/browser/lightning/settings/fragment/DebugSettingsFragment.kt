package acr.browser.lightning.settings.fragment

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.R
import acr.browser.lightning.preference.PreferenceManager
import acr.browser.lightning.utils.Utils
import android.os.Bundle
import javax.inject.Inject

class DebugSettingsFragment : AbstractSettingsFragment() {

    @Inject internal lateinit var preferenceManager: PreferenceManager

    override fun providePreferencesXmlResource() = R.xml.preference_debug

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BrowserApp.appComponent.inject(this)
        addPreferencesFromResource(R.xml.preference_debug)

        togglePreference(
                preference = LEAK_CANARY,
                isChecked = preferenceManager.useLeakCanary,
                onCheckChange = {
                    activity?.let {
                        Utils.showSnackbar(it, R.string.app_restart)
                    }
                    preferenceManager.useLeakCanary = it
                }
        )
    }

    companion object {
        private const val LEAK_CANARY = "leak_canary_enabled"
    }
}
