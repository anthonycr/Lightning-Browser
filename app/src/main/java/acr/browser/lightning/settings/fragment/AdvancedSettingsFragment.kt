package acr.browser.lightning.settings.fragment

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.R
import acr.browser.lightning.constant.TEXT_ENCODINGS
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.preference.PreferenceManager
import acr.browser.lightning.preference.UserPreferences
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v7.app.AlertDialog
import javax.inject.Inject

/**
 * The advanced settings of the app.
 */
class AdvancedSettingsFragment : AbstractSettingsFragment() {

    private val SETTINGS_NEW_WINDOW = "allow_new_window"
    private val SETTINGS_ENABLE_COOKIES = "allow_cookies"
    private val SETTINGS_COOKIES_INCOGNITO = "incognito_cookies"
    private val SETTINGS_RESTORE_TABS = "restore_tabs"
    private val SETTINGS_RENDERING_MODE = "rendering_mode"
    private val SETTINGS_URL_CONTENT = "url_contents"
    private val SETTINGS_TEXT_ENCODING = "text_encoding"

    @Inject internal lateinit var userPreferences: UserPreferences
    @Inject internal lateinit var preferenceManager: PreferenceManager

    override fun providePreferencesXmlResource() = R.xml.preference_advanced

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        BrowserApp.appComponent.inject(this)

        clickableDynamicPreference(
                preference = SETTINGS_RENDERING_MODE,
                summary = getString(renderingModePreferenceToString(preferenceManager.renderingMode)),
                onClick = this::showRenderingDialogPicker
        )

        clickableDynamicPreference(
                preference = SETTINGS_TEXT_ENCODING,
                summary = preferenceManager.textEncoding,
                onClick = this::showTextEncodingDialogPicker
        )

        clickableDynamicPreference(
                preference = SETTINGS_URL_CONTENT,
                summary = urlBoxPreferenceToString(preferenceManager.urlBoxContentChoice),
                onClick = this::showUrlBoxDialogPicker
        )

        checkBoxPreference(
                preference = SETTINGS_NEW_WINDOW,
                isChecked = preferenceManager.popupsEnabled,
                onCheckChange = preferenceManager::setPopupsEnabled
        )

        checkBoxPreference(
                preference = SETTINGS_ENABLE_COOKIES,
                isChecked = userPreferences.cookiesEnabled,
                onCheckChange = { userPreferences.cookiesEnabled = it }
        )

        checkBoxPreference(
                preference = SETTINGS_COOKIES_INCOGNITO,
                isChecked = userPreferences.incognitoCookiesEnabled,
                onCheckChange = { userPreferences.incognitoCookiesEnabled = it }
        )

        checkBoxPreference(
                preference = SETTINGS_RESTORE_TABS,
                isChecked = preferenceManager.restoreLostTabsEnabled,
                onCheckChange = preferenceManager::setRestoreLostTabsEnabled
        )
    }

    /**
     * Shows the dialog which allows the user to choose the browser's rendering method.
     *
     * @param summaryUpdater the command which allows the summary to be updated.
     */
    private fun showRenderingDialogPicker(summaryUpdater: SummaryUpdater) {
        activity?.let {

            val dialog = AlertDialog.Builder(it).apply {
                setTitle(resources.getString(R.string.rendering_mode))

                val choices = arrayOf(
                        it.getString(R.string.name_normal),
                        it.getString(R.string.name_inverted),
                        it.getString(R.string.name_grayscale),
                        it.getString(R.string.name_inverted_grayscale),
                        it.getString(R.string.name_increase_contrast)
                )

                setSingleChoiceItems(choices, preferenceManager.renderingMode) { _, which ->
                    preferenceManager.renderingMode = which
                    summaryUpdater.updateSummary(getString(renderingModePreferenceToString(which)))
                }
                setPositiveButton(resources.getString(R.string.action_ok), null)
            }.show()

            BrowserDialog.setDialogSize(it, dialog)
        }

    }

    /**
     * Shows the dialog which allows the user to choose the browser's text encoding.
     *
     * @param summaryUpdater the command which allows the summary to be updated.
     */
    private fun showTextEncodingDialogPicker(summaryUpdater: SummaryUpdater) {
        activity?.let {
            val dialog = AlertDialog.Builder(it).apply {
                setTitle(resources.getString(R.string.text_encoding))

                val currentChoice = TEXT_ENCODINGS.indexOf(preferenceManager.textEncoding)

                setSingleChoiceItems(TEXT_ENCODINGS, currentChoice, { _, which ->
                    preferenceManager.textEncoding = TEXT_ENCODINGS[which]
                    summaryUpdater.updateSummary(TEXT_ENCODINGS[which])
                })
                setPositiveButton(resources.getString(R.string.action_ok), null)
            }.show()

            BrowserDialog.setDialogSize(it, dialog)
        }
    }

    /**
     * Shows the dialog which allows the user to choose the browser's URL box display options.
     *
     * @param summaryUpdater the command which allows the summary to be updated.
     */
    private fun showUrlBoxDialogPicker(summaryUpdater: SummaryUpdater) {
        activity?.let {
            val dialog = AlertDialog.Builder(it).apply {
                setTitle(resources.getString(R.string.url_contents))

                val array = resources.getStringArray(R.array.url_content_array)

                setSingleChoiceItems(array, preferenceManager.urlBoxContentChoice) { _, which ->
                    preferenceManager.urlBoxContentChoice = which
                    summaryUpdater.updateSummary(urlBoxPreferenceToString(which))
                }
                setPositiveButton(resources.getString(R.string.action_ok), null)
            }.show()
            BrowserDialog.setDialogSize(it, dialog)
        }
    }

    /**
     * Convert an integer to the [StringRes] representation which can be displayed to the user for
     * the rendering mode preference.
     */
    @StringRes
    private fun renderingModePreferenceToString(preference: Int): Int = when (preference) {
        0 -> R.string.name_normal
        1 -> R.string.name_inverted
        2 -> R.string.name_grayscale
        3 -> R.string.name_inverted_grayscale
        4 -> R.string.name_increase_contrast
        else -> throw IllegalArgumentException("Unknown rendering mode preference $preference")
    }

    /**
     * Convert an integer to the [String] representation which can be displayed to the user for the
     * URL box preference.
     */
    private fun urlBoxPreferenceToString(preference: Int): String {
        val stringArray = resources.getStringArray(R.array.url_content_array)

        return stringArray[preference]
    }

}