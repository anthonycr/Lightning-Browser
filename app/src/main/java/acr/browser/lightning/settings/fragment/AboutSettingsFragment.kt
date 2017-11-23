/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.settings.fragment

import acr.browser.lightning.BuildConfig
import acr.browser.lightning.R
import android.os.Bundle

class AboutSettingsFragment : AbstractSettingsFragment() {

    override fun providePreferencesResource() = R.xml.preference_about

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        clickablePreference(
                preference = SETTINGS_VERSION,
                summary = BuildConfig.VERSION_NAME,
                onClick = { _ -> }
        )
    }

    companion object {
        private val SETTINGS_VERSION = "pref_version"
    }
}
