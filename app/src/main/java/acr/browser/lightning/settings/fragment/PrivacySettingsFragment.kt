package acr.browser.lightning.settings.fragment

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.R
import acr.browser.lightning.database.history.HistoryRepository
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.dialog.DialogItem
import acr.browser.lightning.extensions.snackbar
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.utils.ApiUtils
import acr.browser.lightning.utils.WebUtils
import acr.browser.lightning.view.LightningView
import android.os.Bundle
import android.webkit.WebView
import io.reactivex.Completable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject
import javax.inject.Named

class PrivacySettingsFragment : AbstractSettingsFragment() {

    @Inject internal lateinit var historyRepository: HistoryRepository
    @Inject internal lateinit var userPreferences: UserPreferences
    @Inject @field:Named("database") internal lateinit var databaseScheduler: Scheduler

    override fun providePreferencesXmlResource() = R.xml.preference_privacy

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BrowserApp.appComponent.inject(this)

        clickablePreference(preference = SETTINGS_CLEARCACHE, onClick = this::clearCache)
        clickablePreference(preference = SETTINGS_CLEARHISTORY, onClick = this::clearHistoryDialog)
        clickablePreference(preference = SETTINGS_CLEARCOOKIES, onClick = this::clearCookiesDialog)
        clickablePreference(preference = SETTINGS_CLEARWEBSTORAGE, onClick = this::clearWebStorage)

        checkBoxPreference(
                preference = SETTINGS_LOCATION,
                isChecked = userPreferences.locationEnabled,
                onCheckChange = { userPreferences.locationEnabled = it }
        )

        checkBoxPreference(
                preference = SETTINGS_THIRDPCOOKIES,
                isChecked = userPreferences.blockThirdPartyCookiesEnabled,
                isEnabled = ApiUtils.doesSupportThirdPartyCookieBlocking(),
                onCheckChange = { userPreferences.blockThirdPartyCookiesEnabled = it }
        )

        checkBoxPreference(
                preference = SETTINGS_SAVEPASSWORD,
                isChecked = userPreferences.savePasswordsEnabled,
                onCheckChange = { userPreferences.savePasswordsEnabled = it }
        )

        checkBoxPreference(
                preference = SETTINGS_CACHEEXIT,
                isChecked = userPreferences.clearCacheExit,
                onCheckChange = { userPreferences.clearCacheExit = it }
        )

        checkBoxPreference(
                preference = SETTINGS_HISTORYEXIT,
                isChecked = userPreferences.clearHistoryExitEnabled,
                onCheckChange = { userPreferences.clearHistoryExitEnabled = it }
        )

        checkBoxPreference(
                preference = SETTINGS_COOKIEEXIT,
                isChecked = userPreferences.clearCookiesExitEnabled,
                onCheckChange = { userPreferences.clearCookiesExitEnabled = it }
        )

        checkBoxPreference(
                preference = SETTINGS_WEBSTORAGEEXIT,
                isChecked = userPreferences.clearWebStorageExitEnabled,
                onCheckChange = { userPreferences.clearWebStorageExitEnabled = it }
        )

        checkBoxPreference(
                preference = SETTINGS_DONOTTRACK,
                isChecked = userPreferences.doNotTrackEnabled && ApiUtils.doesSupportWebViewHeaders(),
                isEnabled = ApiUtils.doesSupportWebViewHeaders(),
                onCheckChange = { userPreferences.doNotTrackEnabled = it }
        )

        checkBoxPreference(
                preference = SETTINGS_WEBRTC,
                isChecked = userPreferences.webRtcEnabled && ApiUtils.doesSupportWebRtc(),
                isEnabled = ApiUtils.doesSupportWebRtc(),
                onCheckChange = { userPreferences.webRtcEnabled = it }
        )

        checkBoxPreference(
                preference = SETTINGS_IDENTIFYINGHEADERS,
                isChecked = userPreferences.removeIdentifyingHeadersEnabled && ApiUtils.doesSupportWebViewHeaders(),
                isEnabled = ApiUtils.doesSupportWebViewHeaders(),
                summary = "${LightningView.HEADER_REQUESTED_WITH}, ${LightningView.HEADER_WAP_PROFILE}",
                onCheckChange = { userPreferences.removeIdentifyingHeadersEnabled = it }
        )

    }

    private fun clearHistoryDialog() {
        BrowserDialog.showPositiveNegativeDialog(
                activity = activity,
                title = R.string.title_clear_history,
                message = R.string.dialog_history,
                positiveButton = DialogItem(R.string.action_yes) {
                    clearHistory()
                            .subscribeOn(databaseScheduler)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                activity.snackbar(R.string.message_clear_history)
                            }
                },
                negativeButton = DialogItem(R.string.action_no) {},
                onCancel = {}
        )
    }

    private fun clearCookiesDialog() {
        BrowserDialog.showPositiveNegativeDialog(
                activity = activity,
                title = R.string.title_clear_cookies,
                message = R.string.dialog_cookies,
                positiveButton = DialogItem(R.string.action_yes) {
                    clearCookies()
                            .subscribeOn(databaseScheduler)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                activity.snackbar(R.string.message_cookies_cleared)
                            }
                },
                negativeButton = DialogItem(R.string.action_no) {},
                onCancel = {}
        )
    }

    private fun clearCache() {
        WebView(requireNotNull(activity)).apply {
            clearCache(true)
            destroy()
        }
        activity.snackbar(R.string.message_cache_cleared)
    }

    private fun clearHistory(): Completable = Completable.fromAction {
        val activity = activity
        if (activity != null) {
            // TODO: 6/9/17 clearHistory is not synchronous
            WebUtils.clearHistory(activity, historyRepository, databaseScheduler)
        } else {
            throw RuntimeException("Activity was null in clearHistory")
        }
    }

    private fun clearCookies(): Completable = Completable.fromAction {
        val activity = activity
        if (activity != null) {
            WebUtils.clearCookies(activity)
        } else {
            throw RuntimeException("Activity was null in clearCookies")
        }
    }

    private fun clearWebStorage() {
        WebUtils.clearWebStorage()
        activity.snackbar(R.string.message_web_storage_cleared)
    }

    companion object {
        private const val SETTINGS_LOCATION = "location"
        private const val SETTINGS_THIRDPCOOKIES = "third_party"
        private const val SETTINGS_SAVEPASSWORD = "password"
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
