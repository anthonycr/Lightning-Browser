package acr.browser.lightning.settings.fragment

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.R
import acr.browser.lightning.database.history.HistoryRepository
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.dialog.DialogItem
import acr.browser.lightning.preference.PreferenceManager
import acr.browser.lightning.utils.ApiUtils
import acr.browser.lightning.utils.Utils
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

    private val SETTINGS_LOCATION = "location"
    private val SETTINGS_THIRDPCOOKIES = "third_party"
    private val SETTINGS_SAVEPASSWORD = "password"
    private val SETTINGS_CACHEEXIT = "clear_cache_exit"
    private val SETTINGS_HISTORYEXIT = "clear_history_exit"
    private val SETTINGS_COOKIEEXIT = "clear_cookies_exit"
    private val SETTINGS_CLEARCACHE = "clear_cache"
    private val SETTINGS_CLEARHISTORY = "clear_history"
    private val SETTINGS_CLEARCOOKIES = "clear_cookies"
    private val SETTINGS_CLEARWEBSTORAGE = "clear_webstorage"
    private val SETTINGS_WEBSTORAGEEXIT = "clear_webstorage_exit"
    private val SETTINGS_DONOTTRACK = "do_not_track"
    private val SETTINGS_WEBRTC = "webrtc_support"
    private val SETTINGS_IDENTIFYINGHEADERS = "remove_identifying_headers"

    @Inject internal lateinit var historyRepository: HistoryRepository
    @Inject internal lateinit var preferenceManager: PreferenceManager
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
                isChecked = preferenceManager.locationEnabled,
                onCheckChange = preferenceManager::setLocationEnabled
        )

        checkBoxPreference(
                preference = SETTINGS_THIRDPCOOKIES,
                isChecked = preferenceManager.blockThirdPartyCookiesEnabled,
                isEnabled = ApiUtils.doesSupportThirdPartyCookieBlocking(),
                onCheckChange = preferenceManager::setBlockThirdPartyCookiesEnabled
        )

        checkBoxPreference(
                preference = SETTINGS_SAVEPASSWORD,
                isChecked = preferenceManager.savePasswordsEnabled,
                onCheckChange = preferenceManager::setSavePasswordsEnabled
        )

        checkBoxPreference(
                preference = SETTINGS_CACHEEXIT,
                isChecked = preferenceManager.clearCacheExit,
                onCheckChange = preferenceManager::setClearCacheExit
        )

        checkBoxPreference(
                preference = SETTINGS_HISTORYEXIT,
                isChecked = preferenceManager.clearHistoryExitEnabled,
                onCheckChange = preferenceManager::setClearHistoryExitEnabled
        )

        checkBoxPreference(
                preference = SETTINGS_COOKIEEXIT,
                isChecked = preferenceManager.clearCookiesExitEnabled,
                onCheckChange = preferenceManager::setClearCookiesExitEnabled
        )

        checkBoxPreference(
                preference = SETTINGS_WEBSTORAGEEXIT,
                isChecked = preferenceManager.clearWebStorageExitEnabled,
                onCheckChange = preferenceManager::setClearWebStorageExitEnabled
        )

        checkBoxPreference(
                preference = SETTINGS_DONOTTRACK,
                isChecked = preferenceManager.doNotTrackEnabled && ApiUtils.doesSupportWebViewHeaders(),
                isEnabled = ApiUtils.doesSupportWebViewHeaders(),
                onCheckChange = preferenceManager::setDoNotTrackEnabled
        )

        checkBoxPreference(
                preference = SETTINGS_WEBRTC,
                isChecked = preferenceManager.webRtcEnabled && ApiUtils.doesSupportWebRtc(),
                isEnabled = ApiUtils.doesSupportWebRtc(),
                onCheckChange = preferenceManager::setWebRtcEnabled
        )

        checkBoxPreference(
                preference = SETTINGS_IDENTIFYINGHEADERS,
                isChecked = preferenceManager.removeIdentifyingHeadersEnabled && ApiUtils.doesSupportWebViewHeaders(),
                isEnabled = ApiUtils.doesSupportWebViewHeaders(),
                summary = "${LightningView.HEADER_REQUESTED_WITH}, ${LightningView.HEADER_WAP_PROFILE}",
                onCheckChange = preferenceManager::setRemoveIdentifyingHeadersEnabled
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
                                Utils.showSnackbar(activity, R.string.message_clear_history)
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
                                Utils.showSnackbar(activity, R.string.message_cookies_cleared)
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
        Utils.showSnackbar(activity, R.string.message_cache_cleared)
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
        Utils.showSnackbar(activity, R.string.message_web_storage_cleared)
    }

}
