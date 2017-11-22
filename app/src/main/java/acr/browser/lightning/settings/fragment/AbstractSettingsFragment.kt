package acr.browser.lightning.settings.fragment

import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.SwitchPreference
import android.support.annotation.XmlRes

/**
 * An abstract settings fragment which performs wiring for an instance of [PreferenceFragment].
 */
abstract class AbstractSettingsFragment : PreferenceFragment() {

    /**
     * Provide the XML resource which holds the preferences.
     */
    @XmlRes
    protected abstract fun providePreferencesResource(): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load the preferences from an XML resource
        addPreferencesFromResource(providePreferencesResource())
    }

    protected fun checkBoxPreference(preference: String,
                                     isChecked: Boolean,
                                     isEnabled: Boolean = true,
                                     summary: String? = null,
                                     onCheckChange: (Boolean) -> Unit) {
        (findPreference(preference) as CheckBoxPreference).apply {
            this.isChecked = isChecked
            this.isEnabled = isEnabled
            summary?.let {
                this.summary = summary
            }
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, any: Any ->
                onCheckChange(any as Boolean)
                true
            }
        }
    }

    protected fun clickablePreference(preference: String,
                                      isEnabled: Boolean = true,
                                      summary: String? = null,
                                      onClick: () -> Unit) {
        clickablePreference(
                preference = preference,
                isEnabled = isEnabled,
                summary = summary,
                onClick = object : Function1<SummaryUpdater, Unit> {
                    override fun invoke(p1: SummaryUpdater) = onClick()
                }
        )
    }

    protected fun clickablePreference(preference: String,
                                      isEnabled: Boolean = true,
                                      summary: String? = null,
                                      onClick: (SummaryUpdater) -> Unit) {
        findPreference(preference).apply {
            this.isEnabled = isEnabled
            summary?.let {
                this.summary = summary
            }
            val summaryUpdate = SummaryUpdater(this)
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                onClick(summaryUpdate)
                true
            }
        }
    }

    protected fun togglePreference(preference: String,
                                   isChecked: Boolean,
                                   isEnabled: Boolean = true,
                                   onCheckChange: (Boolean) -> Unit) {
        (findPreference(preference) as SwitchPreference).apply {
            this.isChecked = isChecked
            this.isEnabled = isEnabled
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, any: Any ->
                onCheckChange(any as Boolean)
                true
            }
        }
    }

}