package acr.browser.lightning.settings.fragment

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.BuildConfig
import acr.browser.lightning.R
import acr.browser.lightning.constant.*
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.dialog.DialogItem
import acr.browser.lightning.preference.PreferenceManager
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.search.SearchEngineProvider
import acr.browser.lightning.search.engine.BaseSearchEngine
import acr.browser.lightning.search.engine.CustomSearch
import acr.browser.lightning.utils.FileUtils
import acr.browser.lightning.utils.ProxyUtils
import acr.browser.lightning.utils.ThemeUtils
import acr.browser.lightning.utils.Utils
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.TextView
import javax.inject.Inject

/**
 * The general settings of the app.
 */
class GeneralSettingsFragment : AbstractSettingsFragment() {

    @Inject lateinit var searchEngineProvider: SearchEngineProvider
    @Inject lateinit var preferenceManager: PreferenceManager
    @Inject lateinit var userPreferences: UserPreferences

    private lateinit var proxyChoices: Array<String>

    override fun providePreferencesXmlResource() = R.xml.preference_general

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        BrowserApp.appComponent.inject(this)

        proxyChoices = resources.getStringArray(R.array.proxy_choices_array)

        clickableDynamicPreference(
                preference = SETTINGS_PROXY,
                summary = proxyChoiceToSummary(preferenceManager.proxyChoice),
                onClick = this::showProxyPicker
        )

        clickableDynamicPreference(
                preference = SETTINGS_USER_AGENT,
                summary = choiceToUserAgent(preferenceManager.userAgentChoice),
                onClick = this::showUserAgentChooserDialog
        )

        clickableDynamicPreference(
                preference = SETTINGS_DOWNLOAD,
                summary = preferenceManager.downloadDirectory,
                onClick = this::showDownloadLocationDialog
        )

        clickableDynamicPreference(
                preference = SETTINGS_HOME,
                summary = homePageUrlToDisplayTitle(preferenceManager.homepage),
                onClick = this::showHomePageDialog
        )

        clickableDynamicPreference(
                preference = SETTINGS_SEARCH_ENGINE,
                summary = getSearchEngineSummary(searchEngineProvider.getCurrentSearchEngine()),
                onClick = this::showSearchProviderDialog
        )

        clickableDynamicPreference(
                preference = SETTINGS_SUGGESTIONS,
                summary = searchSuggestionChoiceToTitle(preferenceManager.searchSuggestionChoice),
                onClick = this::showSearchSuggestionsDialog
        )

        checkBoxPreference(
                preference = SETTINGS_FLASH,
                isEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT,
                summary = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    null
                } else {
                    getString(R.string.flash_not_supported)
                },
                isChecked = preferenceManager.flashSupport > 0,
                onCheckChange = { checked ->
                    if (!Utils.isFlashInstalled(activity) && checked) {
                        Utils.createInformativeDialog(activity, R.string.title_warning, R.string.dialog_adobe_not_installed)
                        preferenceManager.flashSupport = 0
                    } else {
                        if (checked) {
                            showFlashChoiceDialog()
                        } else {
                            preferenceManager.flashSupport = 0
                        }
                    }
                }
        )

        checkBoxPreference(
                preference = SETTINGS_ADS,
                isEnabled = BuildConfig.FULL_VERSION,
                summary = if (BuildConfig.FULL_VERSION) {
                    null
                } else {
                    getString(R.string.upsell_plus_version)
                },
                isChecked = BuildConfig.FULL_VERSION && userPreferences.adBlockEnabled,
                onCheckChange = { userPreferences.adBlockEnabled = it }
        )

        checkBoxPreference(
                preference = SETTINGS_IMAGES,
                isChecked = preferenceManager.blockImagesEnabled,
                onCheckChange = preferenceManager::setBlockImagesEnabled
        )

        checkBoxPreference(
                preference = SETTINGS_JAVASCRIPT,
                isChecked = preferenceManager.javaScriptEnabled,
                onCheckChange = preferenceManager::setJavaScriptEnabled
        )

        checkBoxPreference(
                preference = SETTINGS_COLOR_MODE,
                isChecked = preferenceManager.colorModeEnabled,
                onCheckChange = preferenceManager::setColorModeEnabled
        )
    }

    private fun showFlashChoiceDialog() {
        activity?.let {
            BrowserDialog.showPositiveNegativeDialog(
                    activity = it,
                    title = R.string.title_flash,
                    message = R.string.flash,
                    positiveButton = DialogItem(
                            title = R.string.action_manual,
                            onClick = { preferenceManager.flashSupport = 1 }
                    ),
                    negativeButton = DialogItem(
                            title = R.string.action_auto,
                            onClick = { preferenceManager.flashSupport = 2 }
                    ),
                    onCancel = { preferenceManager.flashSupport = 0 }
            )
        }
    }

    private fun proxyChoiceToSummary(choice: Int) = when (choice) {
        PROXY_MANUAL -> "${preferenceManager.proxyHost}:${preferenceManager.proxyPort}"
        else -> proxyChoices[choice]
    }

    private fun showProxyPicker(summaryUpdater: SummaryUpdater) {
        BrowserDialog.showCustomDialog(activity) {
            setTitle(R.string.http_proxy)
            setSingleChoiceItems(proxyChoices, preferenceManager.proxyChoice) { _, which ->
                updateProxyChoice(which, it, summaryUpdater)
            }
            setPositiveButton(R.string.action_ok, null)
        }
    }

    private fun updateProxyChoice(@Proxy choice: Int, activity: Activity, summaryUpdater: SummaryUpdater) {
        val sanitizedChoice = ProxyUtils.sanitizeProxyChoice(choice, activity)
        when (sanitizedChoice) {
            PROXY_ORBOT,
            PROXY_I2P,
            NO_PROXY -> Unit
            PROXY_MANUAL -> showManualProxyPicker(activity, summaryUpdater)
        }

        preferenceManager.proxyChoice = sanitizedChoice
        if (sanitizedChoice < proxyChoices.size) {
            summaryUpdater.updateSummary(proxyChoices[sanitizedChoice])
        }
    }

    private fun showManualProxyPicker(activity: Activity, summaryUpdater: SummaryUpdater) {
        val v = activity.layoutInflater.inflate(R.layout.dialog_manual_proxy, null)
        val eProxyHost = v.findViewById<TextView>(R.id.proxyHost)
        val eProxyPort = v.findViewById<TextView>(R.id.proxyPort)

        // Limit the number of characters since the port needs to be of type int
        // Use input filters to limit the EditText length and determine the max
        // length by using length of integer MAX_VALUE
        val maxCharacters = Integer.toString(Integer.MAX_VALUE).length
        eProxyPort.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(maxCharacters - 1))

        eProxyHost.text = preferenceManager.proxyHost
        eProxyPort.text = Integer.toString(preferenceManager.proxyPort)

        BrowserDialog.showCustomDialog(activity) {
            setTitle(R.string.manual_proxy)
            setView(v)
            setPositiveButton(R.string.action_ok) { _, _ ->
                val proxyHost = eProxyHost.text.toString()
                val proxyPort = try {
                    // Try/Catch in case the user types an empty string or a number
                    // larger than max integer
                    Integer.parseInt(eProxyPort.text.toString())
                } catch (ignored: NumberFormatException) {
                    preferenceManager.proxyPort
                }

                preferenceManager.proxyHost = proxyHost
                preferenceManager.proxyPort = proxyPort
                summaryUpdater.updateSummary("$proxyHost:$proxyPort")
            }
        }
    }

    private fun choiceToUserAgent(index: Int) = when (index) {
        1 -> resources.getString(R.string.agent_default)
        2 -> resources.getString(R.string.agent_desktop)
        3 -> resources.getString(R.string.agent_mobile)
        4 -> resources.getString(R.string.agent_custom)
        else -> resources.getString(R.string.agent_default)
    }

    private fun showUserAgentChooserDialog(summaryUpdater: SummaryUpdater) {
        BrowserDialog.showCustomDialog(activity) {
            setTitle(resources.getString(R.string.title_user_agent))
            setSingleChoiceItems(R.array.user_agent, preferenceManager.userAgentChoice - 1) { _, which ->
                preferenceManager.userAgentChoice = which + 1
                summaryUpdater.updateSummary(choiceToUserAgent(preferenceManager.userAgentChoice))
                when (which) {
                    in 0..2 -> Unit
                    3 -> {
                        summaryUpdater.updateSummary(resources.getString(R.string.agent_custom))
                        showCustomUserAgentPicker(summaryUpdater)
                    }
                }
            }
            setPositiveButton(resources.getString(R.string.action_ok), null)
        }
    }

    private fun showCustomUserAgentPicker(summaryUpdater: SummaryUpdater) {
        activity?.let {
            BrowserDialog.showEditText(it,
                    R.string.title_user_agent,
                    R.string.title_user_agent,
                    preferenceManager.getUserAgentString(""),
                    R.string.action_ok) { s ->
                preferenceManager.setUserAgentString(s)
                summaryUpdater.updateSummary(it.getString(R.string.agent_custom))
            }
        }
    }

    private fun showDownloadLocationDialog(summaryUpdater: SummaryUpdater) {
        BrowserDialog.showCustomDialog(activity) {
            setTitle(resources.getString(R.string.title_download_location))
            val n: Int = if (preferenceManager.downloadDirectory.contains(Environment.DIRECTORY_DOWNLOADS)) {
                0
            } else {
                1
            }

            setSingleChoiceItems(R.array.download_folder, n) { _, which ->
                when (which) {
                    0 -> {
                        preferenceManager.downloadDirectory = FileUtils.DEFAULT_DOWNLOAD_PATH
                        summaryUpdater.updateSummary(FileUtils.DEFAULT_DOWNLOAD_PATH)
                    }
                    1 -> {
                        showCustomDownloadLocationPicker(summaryUpdater)
                    }
                }
            }
            setPositiveButton(resources.getString(R.string.action_ok), null)
        }
    }


    private fun showCustomDownloadLocationPicker(summaryUpdater: SummaryUpdater) {
        activity?.let {
            val dialogView = LayoutInflater.from(it).inflate(R.layout.dialog_edit_text, null)
            val getDownload = dialogView.findViewById<EditText>(R.id.dialog_edit_text)

            val errorColor = ContextCompat.getColor(it, R.color.error_red)
            val regularColor = ThemeUtils.getTextColor(it)
            getDownload.setTextColor(regularColor)
            getDownload.addTextChangedListener(DownloadLocationTextWatcher(getDownload, errorColor, regularColor))
            getDownload.setText(preferenceManager.downloadDirectory)

            BrowserDialog.showCustomDialog(it) {
                setTitle(R.string.title_download_location)
                setView(dialogView)
                setPositiveButton(R.string.action_ok) { _, _ ->
                    var text = getDownload.text.toString()
                    text = FileUtils.addNecessarySlashes(text)
                    preferenceManager.downloadDirectory = text
                    summaryUpdater.updateSummary(text)
                }
            }
        }
    }

    private class DownloadLocationTextWatcher(
            private val getDownload: EditText,
            private val errorColor: Int,
            private val regularColor: Int
    ) : TextWatcher {

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable) {
            if (!FileUtils.isWriteAccessAvailable(s.toString())) {
                this.getDownload.setTextColor(this.errorColor)
            } else {
                this.getDownload.setTextColor(this.regularColor)
            }
        }
    }

    private fun homePageUrlToDisplayTitle(url: String): String = when (url) {
        SCHEME_HOMEPAGE -> resources.getString(R.string.action_homepage)
        SCHEME_BLANK -> resources.getString(R.string.action_blank)
        SCHEME_BOOKMARKS -> resources.getString(R.string.action_bookmarks)
        else -> url
    }

    private fun showHomePageDialog(summaryUpdater: SummaryUpdater) {
        BrowserDialog.showCustomDialog(activity) {
            setTitle(R.string.home)
            val n = when (preferenceManager.homepage) {
                SCHEME_HOMEPAGE -> 0
                SCHEME_BLANK -> 1
                SCHEME_BOOKMARKS -> 2
                else -> 3
            }

            setSingleChoiceItems(R.array.homepage, n) { _, which ->
                when (which) {
                    0 -> {
                        preferenceManager.homepage = SCHEME_HOMEPAGE
                        summaryUpdater.updateSummary(resources.getString(R.string.action_homepage))
                    }
                    1 -> {
                        preferenceManager.homepage = SCHEME_BLANK
                        summaryUpdater.updateSummary(resources.getString(R.string.action_blank))
                    }
                    2 -> {
                        preferenceManager.homepage = SCHEME_BOOKMARKS
                        summaryUpdater.updateSummary(resources.getString(R.string.action_bookmarks))
                    }
                    3 -> {
                        showCustomHomePagePicker(summaryUpdater)
                    }
                }
            }
            setPositiveButton(resources.getString(R.string.action_ok), null)
        }
    }

    private fun showCustomHomePagePicker(summaryUpdater: SummaryUpdater) {
        val currentHomepage: String = if (!URLUtil.isAboutUrl(preferenceManager.homepage)) {
            preferenceManager.homepage
        } else {
            "https://www.google.com"
        }

        activity?.let {
            BrowserDialog.showEditText(it,
                    R.string.title_custom_homepage,
                    R.string.title_custom_homepage,
                    currentHomepage,
                    R.string.action_ok) { url ->
                preferenceManager.homepage = url
                summaryUpdater.updateSummary(url)
            }
        }
    }

    private fun getSearchEngineSummary(baseSearchEngine: BaseSearchEngine): String {
        return if (baseSearchEngine is CustomSearch) {
            baseSearchEngine.queryUrl
        } else {
            getString(baseSearchEngine.titleRes)
        }
    }

    private fun convertSearchEngineToString(searchEngines: List<BaseSearchEngine>): Array<CharSequence> =
            searchEngines.map { getString(it.titleRes) }.toTypedArray()

    private fun showSearchProviderDialog(summaryUpdater: SummaryUpdater) {
        BrowserDialog.showCustomDialog(activity) {
            setTitle(resources.getString(R.string.title_search_engine))

            val searchEngineList = searchEngineProvider.getAllSearchEngines()

            val chars = convertSearchEngineToString(searchEngineList)

            val n = preferenceManager.searchChoice

            setSingleChoiceItems(chars, n) { _, which ->
                val searchEngine = searchEngineList[which]

                // Store the search engine preference
                val preferencesIndex = searchEngineProvider.mapSearchEngineToPreferenceIndex(searchEngine)
                preferenceManager.searchChoice = preferencesIndex

                if (searchEngine is CustomSearch) {
                    // Show the URL picker
                    showCustomSearchDialog(searchEngine, summaryUpdater)
                } else {
                    // Set the new search engine summary
                    summaryUpdater.updateSummary(getSearchEngineSummary(searchEngine))
                }
            }
            setPositiveButton(R.string.action_ok, null)
        }
    }

    private fun showCustomSearchDialog(customSearch: CustomSearch, summaryUpdater: SummaryUpdater) {
        activity?.let {
            BrowserDialog.showEditText(
                    it,
                    R.string.search_engine_custom,
                    R.string.search_engine_custom,
                    preferenceManager.searchUrl,
                    R.string.action_ok
            ) { searchUrl ->
                preferenceManager.searchUrl = searchUrl
                summaryUpdater.updateSummary(getSearchEngineSummary(customSearch))
            }

        }
    }

    private fun searchSuggestionChoiceToTitle(choice: PreferenceManager.Suggestion): String =
            when (choice) {
                PreferenceManager.Suggestion.SUGGESTION_GOOGLE -> getString(R.string.powered_by_google)
                PreferenceManager.Suggestion.SUGGESTION_DUCK -> getString(R.string.powered_by_duck)
                PreferenceManager.Suggestion.SUGGESTION_BAIDU -> getString(R.string.powered_by_baidu)
                PreferenceManager.Suggestion.SUGGESTION_NONE -> getString(R.string.search_suggestions_off)
            }

    private fun showSearchSuggestionsDialog(summaryUpdater: SummaryUpdater) {
        BrowserDialog.showCustomDialog(activity) {
            setTitle(resources.getString(R.string.search_suggestions))

            val currentChoice = when (preferenceManager.searchSuggestionChoice) {
                PreferenceManager.Suggestion.SUGGESTION_GOOGLE -> 0
                PreferenceManager.Suggestion.SUGGESTION_DUCK -> 1
                PreferenceManager.Suggestion.SUGGESTION_BAIDU -> 2
                PreferenceManager.Suggestion.SUGGESTION_NONE -> 3
            }

            setSingleChoiceItems(R.array.suggestions, currentChoice) { _, which ->
                val suggestionsProvider = when (which) {
                    0 -> PreferenceManager.Suggestion.SUGGESTION_GOOGLE
                    1 -> PreferenceManager.Suggestion.SUGGESTION_DUCK
                    2 -> PreferenceManager.Suggestion.SUGGESTION_BAIDU
                    3 -> PreferenceManager.Suggestion.SUGGESTION_NONE
                    else -> PreferenceManager.Suggestion.SUGGESTION_NONE
                }
                preferenceManager.searchSuggestionChoice = suggestionsProvider
                summaryUpdater.updateSummary(searchSuggestionChoiceToTitle(suggestionsProvider))
            }
            setPositiveButton(resources.getString(R.string.action_ok), null)
        }
    }

    companion object {
        private const val SETTINGS_PROXY = "proxy"
        private const val SETTINGS_FLASH = "cb_flash"
        private const val SETTINGS_ADS = "cb_ads"
        private const val SETTINGS_IMAGES = "cb_images"
        private const val SETTINGS_JAVASCRIPT = "cb_javascript"
        private const val SETTINGS_COLOR_MODE = "cb_colormode"
        private const val SETTINGS_USER_AGENT = "agent"
        private const val SETTINGS_DOWNLOAD = "download"
        private const val SETTINGS_HOME = "home"
        private const val SETTINGS_SEARCH_ENGINE = "search"
        private const val SETTINGS_SUGGESTIONS = "suggestions_choice"
    }
}