package acr.browser.lightning.settings.fragment

import acr.browser.lightning.R
import acr.browser.lightning.browser.di.injector
import acr.browser.lightning.browser.tab.WebViewFactory
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.database.history.HistoryRepository
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.dialog.DialogItem
import acr.browser.lightning.extensions.snackbar
import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.preference.datastore.getUnsafe
import acr.browser.lightning.preference.datastore.setUnsafe
import acr.browser.lightning.utils.WebUtils
import android.os.Bundle
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PrivacySettingsFragment : AbstractSettingsFragment() {

    @Inject internal lateinit var historyRepository: HistoryRepository
    @Inject internal lateinit var userPreferencesDataStore: UserPreferencesDataStore
    @Inject internal lateinit var appCoroutineScope: CoroutineScope
    @Inject internal lateinit var coroutineDispatchers: CoroutineDispatchers

    override fun providePreferencesXmlResource() = R.xml.preference_privacy

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        injector.inject(this)

        clickablePreference(preference = SETTINGS_CLEARCACHE, onClick = this::clearCache)
        clickablePreference(preference = SETTINGS_CLEARHISTORY, onClick = this::clearHistoryDialog)
        clickablePreference(preference = SETTINGS_CLEARCOOKIES, onClick = this::clearCookiesDialog)
        clickablePreference(preference = SETTINGS_CLEARWEBSTORAGE, onClick = this::clearWebStorage)

        togglePreference(
            preference = SETTINGS_LOCATION,
            isChecked = userPreferencesDataStore.locationEnabled.getUnsafe(),
            onCheckChange = { userPreferencesDataStore.locationEnabled.setUnsafe(it) }
        )

        togglePreference(
            preference = SETTINGS_THIRDPCOOKIES,
            isChecked = userPreferencesDataStore.blockThirdPartyCookiesEnabled.getUnsafe(),
            onCheckChange = { userPreferencesDataStore.blockThirdPartyCookiesEnabled.setUnsafe(it) }
        )

        togglePreference(
            preference = SETTINGS_CACHEEXIT,
            isChecked = userPreferencesDataStore.clearCacheExit.getUnsafe(),
            onCheckChange = { userPreferencesDataStore.clearCacheExit.setUnsafe(it) }
        )

        togglePreference(
            preference = SETTINGS_HISTORYEXIT,
            isChecked = userPreferencesDataStore.clearHistoryExitEnabled.getUnsafe(),
            onCheckChange = { userPreferencesDataStore.clearHistoryExitEnabled.setUnsafe(it) }
        )

        togglePreference(
            preference = SETTINGS_COOKIEEXIT,
            isChecked = userPreferencesDataStore.clearCookiesExitEnabled.getUnsafe(),
            onCheckChange = { userPreferencesDataStore.clearCookiesExitEnabled.setUnsafe(it) }
        )

        togglePreference(
            preference = SETTINGS_WEBSTORAGEEXIT,
            isChecked = userPreferencesDataStore.clearWebStorageExitEnabled.getUnsafe(),
            onCheckChange = { userPreferencesDataStore.clearWebStorageExitEnabled.setUnsafe(it) }
        )

        togglePreference(
            preference = SETTINGS_DONOTTRACK,
            isChecked = userPreferencesDataStore.doNotTrackEnabled.getUnsafe(),
            onCheckChange = { userPreferencesDataStore.doNotTrackEnabled.setUnsafe(it) }
        )

        togglePreference(
            preference = SETTINGS_WEBRTC,
            isChecked = userPreferencesDataStore.webRtcEnabled.getUnsafe(),
            onCheckChange = { userPreferencesDataStore.webRtcEnabled.setUnsafe(it) }
        )

        togglePreference(
            preference = SETTINGS_IDENTIFYINGHEADERS,
            isChecked = userPreferencesDataStore.removeIdentifyingHeadersEnabled.getUnsafe(),
            summary = "${WebViewFactory.HEADER_REQUESTED_WITH}, ${WebViewFactory.HEADER_WAP_PROFILE}",
            onCheckChange = { userPreferencesDataStore.removeIdentifyingHeadersEnabled.setUnsafe(it) }
        )

    }

    private fun clearHistoryDialog() {
        BrowserDialog.showPositiveNegativeDialog(
            activity = requireActivity(),
            title = R.string.title_clear_history,
            message = R.string.dialog_history,
            positiveButton = DialogItem(title = R.string.action_yes) {
                appCoroutineScope.launch {
                    clearHistory()
                    requireActivity().snackbar(R.string.message_clear_history)
                }
            },
            negativeButton = DialogItem(title = R.string.action_no) {},
            onCancel = {}
        )
    }

    private fun clearCookiesDialog() {
        BrowserDialog.showPositiveNegativeDialog(
            activity = requireActivity(),
            title = R.string.title_clear_cookies,
            message = R.string.dialog_cookies,
            positiveButton = DialogItem(title = R.string.action_yes) {
                appCoroutineScope.launch {
                    clearCookies()
                    requireActivity().snackbar(R.string.message_cookies_cleared)
                }
            },
            negativeButton = DialogItem(title = R.string.action_no) {},
            onCancel = {}
        )
    }

    private fun clearCache() {
        WebView(requireNotNull(activity)).apply {
            clearCache(true)
            destroy()
        }
        requireActivity().snackbar(R.string.message_cache_cleared)
    }

    private suspend fun clearHistory() {
        val activity = activity
        if (activity != null) {
            // TODO: 6/9/17 clearHistory is not synchronous
            WebUtils.clearHistory(activity, historyRepository)
        } else {
            throw RuntimeException("Activity was null in clearHistory")
        }
    }

    private suspend fun clearCookies(): Unit = withContext(coroutineDispatchers.io) {
        WebUtils.clearCookies()
    }

    private fun clearWebStorage() {
        WebUtils.clearWebStorage()
        requireActivity().snackbar(R.string.message_web_storage_cleared)
    }

    companion object {
        private const val SETTINGS_LOCATION = "location"
        private const val SETTINGS_THIRDPCOOKIES = "third_party"
        private const val SETTINGS_CACHEEXIT = "clear_cache_exit"
        private const val SETTINGS_HISTORYEXIT = "clear_history_exit"
        private const val SETTINGS_COOKIEEXIT = "clear_cookies_exit"
        private const val SETTINGS_CLEARCACHE = "clear_cache"
        private const val SETTINGS_CLEARHISTORY = "clear_history"
        private const val SETTINGS_CLEARCOOKIES = "clear_cookies"
        private const val SETTINGS_CLEARWEBSTORAGE = "clear_webstorage"
        private const val SETTINGS_WEBSTORAGEEXIT = "clear_webstorage_exit"
        private const val SETTINGS_DONOTTRACK = "do_not_track"
        private const val SETTINGS_WEBRTC = "webrtc_support"
        private const val SETTINGS_IDENTIFYINGHEADERS = "remove_identifying_headers"
    }

}
