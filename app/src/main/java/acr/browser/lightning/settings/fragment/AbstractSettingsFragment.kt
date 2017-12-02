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

    /**
     * Creates a [CheckBoxPreference] with the provided options and listener.
     *
     * @param preference the preference to create.
     * @param isChecked true if it should be initialized as checked, false otherwise.
     * @param isEnabled true if the preference should be enabled, false otherwise. Defaults to true.
     * @param summary the summary to display. Defaults to null, which results in no summary.
     * @param onCheckChange the function that should be called when the check box is toggled.
     */
    protected fun checkBoxPreference(
            preference: String,
            isChecked: Boolean,
            isEnabled: Boolean = true,
            summary: String? = null,
            onCheckChange: (Boolean) -> Unit
    ) {
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

    /**
     * Creates a simple [Preference] which reacts to clicks with the provided options and listener.
     *
     * @param preference the preference to create.
     * @param isEnabled true if the preference should be enabled, false otherwise. Defaults to true.
     * @param summary the summary to display. Defaults to null, which results in no summary.
     * @param onClick the function that should be called when the preference is clicked.
     */
    protected fun clickablePreference(
            preference: String,
            isEnabled: Boolean = true,
            summary: String? = null,
            onClick: () -> Unit
    ) {
        clickableDynamicPreference(
                preference = preference,
                isEnabled = isEnabled,
                summary = summary,
                onClick = object : Function1<SummaryUpdater, Unit> {
                    override fun invoke(p1: SummaryUpdater) = onClick()
                }
        )
    }

    /**
     * Creates a simple [Preference] which reacts to clicks with the provided options and listener.
     * It also allows its summary to be updated when clicked.
     *
     * @param preference the preference to create.
     * @param isEnabled true if the preference should be enabled, false otherwise. Defaults to true.
     * @param summary the summary to display. Defaults to null, which results in no summary.
     * @param onClick the function that should be called when the preference is clicked. The
     * function is supplied with a [SummaryUpdater] object so that it can update the summary if
     * desired.
     */
    protected fun clickableDynamicPreference(
            preference: String,
            isEnabled: Boolean = true,
            summary: String? = null,
            onClick: (SummaryUpdater) -> Unit
    ) {
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

    /**
     * Creates a [SwitchPreference] with the provided options and listener.
     *
     * @param preference the preference to create.
     * @param isChecked true if it should be initialized as checked, false otherwise.
     * @param isEnabled true if the preference should be enabled, false otherwise. Defaults to true.
     * @param onCheckChange the function that should be called when the toggle is toggled.
     */
    protected fun togglePreference(
            preference: String,
            isChecked: Boolean,
            isEnabled: Boolean = true,
            onCheckChange: (Boolean) -> Unit
    ) {
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