package acr.browser.lightning.settings.fragment

import acr.browser.lightning.Capabilities
import acr.browser.lightning.R
import acr.browser.lightning.browser.search.SearchBoxDisplayChoice
import acr.browser.lightning.constant.TEXT_ENCODINGS
import acr.browser.lightning.browser.di.injector
import acr.browser.lightning.extensions.resizeAndShow
import acr.browser.lightning.extensions.withSingleChoiceItems
import acr.browser.lightning.isSupported
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.browser.view.RenderingMode
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import javax.inject.Inject

/**
 * The advanced settings of the app.
 */
class AdvancedSettingsFragment : AbstractSettingsFragment() {

    @Inject internal lateinit var userPreferences: UserPreferences

    override fun providePreferencesXmlResource() = R.xml.preference_advanced

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        injector.inject(this)

        clickableDynamicPreference(
            preference = SETTINGS_RENDERING_MODE,
            summary = userPreferences.renderingMode.toDisplayString(),
            onClick = this::showRenderingDialogPicker
        )

        clickableDynamicPreference(
            preference = SETTINGS_TEXT_ENCODING,
            summary = userPreferences.textEncoding,
            onClick = this::showTextEncodingDialogPicker
        )

        clickableDynamicPreference(
            preference = SETTINGS_URL_CONTENT,
            summary = userPreferences.urlBoxContentChoice.toDisplayString(),
            onClick = this::showUrlBoxDialogPicker
        )

        checkBoxPreference(
            preference = SETTINGS_NEW_WINDOW,
            isChecked = userPreferences.popupsEnabled,
            onCheckChange = { userPreferences.popupsEnabled = it }
        )

        val incognitoCheckboxPreference = checkBoxPreference(
            preference = SETTINGS_COOKIES_INCOGNITO,
            isEnabled = !Capabilities.FULL_INCOGNITO.isSupported,
            isChecked = if (Capabilities.FULL_INCOGNITO.isSupported) {
                userPreferences.cookiesEnabled
            } else {
                userPreferences.incognitoCookiesEnabled
            },
            summary = if (Capabilities.FULL_INCOGNITO.isSupported) {
                getString(R.string.incognito_cookies_pie)
            } else {
                null
            },
            onCheckChange = { userPreferences.incognitoCookiesEnabled = it }
        )

        checkBoxPreference(
            preference = SETTINGS_ENABLE_COOKIES,
            isChecked = userPreferences.cookiesEnabled,
            onCheckChange = {
                userPreferences.cookiesEnabled = it
                if (Capabilities.FULL_INCOGNITO.isSupported) {
                    incognitoCheckboxPreference.isChecked = it
                }
            }
        )

        checkBoxPreference(
            preference = SETTINGS_RESTORE_TABS,
            isChecked = userPreferences.restoreLostTabsEnabled,
            onCheckChange = { userPreferences.restoreLostTabsEnabled = it }
        )
    }

    /**
     * Shows the dialog which allows the user to choose the browser's rendering method.
     *
     * @param summaryUpdater the command which allows the summary to be updated.
     */
    private fun showRenderingDialogPicker(summaryUpdater: SummaryUpdater) {
        activity?.let(AlertDialog::Builder)?.apply {
            setTitle(resources.getString(R.string.rendering_mode))

            val values = RenderingMode.values().map { Pair(it, it.toDisplayString()) }
            withSingleChoiceItems(values, userPreferences.renderingMode) {
                userPreferences.renderingMode = it
                summaryUpdater.updateSummary(it.toDisplayString())

            }
            setPositiveButton(resources.getString(R.string.action_ok), null)
        }?.resizeAndShow()

    }

    /**
     * Shows the dialog which allows the user to choose the browser's text encoding.
     *
     * @param summaryUpdater the command which allows the summary to be updated.
     */
    private fun showTextEncodingDialogPicker(summaryUpdater: SummaryUpdater) {
        activity?.let {
            AlertDialog.Builder(it).apply {
                setTitle(resources.getString(R.string.text_encoding))

                val currentChoice = TEXT_ENCODINGS.indexOf(userPreferences.textEncoding)

                setSingleChoiceItems(TEXT_ENCODINGS, currentChoice) { _, which ->
                    userPreferences.textEncoding = TEXT_ENCODINGS[which]
                    summaryUpdater.updateSummary(TEXT_ENCODINGS[which])
                }
                setPositiveButton(resources.getString(R.string.action_ok), null)
            }.resizeAndShow()
        }
    }

    /**
     * Shows the dialog which allows the user to choose the browser's URL box display options.
     *
     * @param summaryUpdater the command which allows the summary to be updated.
     */
    private fun showUrlBoxDialogPicker(summaryUpdater: SummaryUpdater) {
        activity?.let(AlertDialog::Builder)?.apply {
            setTitle(resources.getString(R.string.url_contents))

            val items = SearchBoxDisplayChoice.values().map { Pair(it, it.toDisplayString()) }

            withSingleChoiceItems(items, userPreferences.urlBoxContentChoice) {
                userPreferences.urlBoxContentChoice = it
                summaryUpdater.updateSummary(it.toDisplayString())
            }
            setPositiveButton(resources.getString(R.string.action_ok), null)
        }?.resizeAndShow()
    }

    private fun SearchBoxDisplayChoice.toDisplayString(): String {
        val stringArray = resources.getStringArray(R.array.url_content_array)
        return when (this) {
            SearchBoxDisplayChoice.DOMAIN -> stringArray[0]
            SearchBoxDisplayChoice.URL -> stringArray[1]
            SearchBoxDisplayChoice.TITLE -> stringArray[2]
        }
    }

    private fun RenderingMode.toDisplayString(): String = getString(
        when (this) {
            RenderingMode.NORMAL -> R.string.name_normal
            RenderingMode.INVERTED -> R.string.name_inverted
            RenderingMode.GRAYSCALE -> R.string.name_grayscale
            RenderingMode.INVERTED_GRAYSCALE -> R.string.name_inverted_grayscale
            RenderingMode.INCREASE_CONTRAST -> R.string.name_increase_contrast
        }
    )

    companion object {
        private const val SETTINGS_NEW_WINDOW = "allow_new_window"
        private const val SETTINGS_ENABLE_COOKIES = "allow_cookies"
        private const val SETTINGS_COOKIES_INCOGNITO = "incognito_cookies"
        private const val SETTINGS_RESTORE_TABS = "restore_tabs"
        private const val SETTINGS_RENDERING_MODE = "rendering_mode"
        private const val SETTINGS_URL_CONTENT = "url_contents"
        private const val SETTINGS_TEXT_ENCODING = "text_encoding"
    }

}
