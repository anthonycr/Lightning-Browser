package acr.browser.lightning.browser.cleanup

import acr.browser.lightning.database.history.HistoryDatabase
import acr.browser.lightning.log.Logger
import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.utils.WebUtils
import android.app.Activity
import javax.inject.Inject

/**
 * Exit cleanup that should run whenever the main browser process is exiting.
 */
class NormalExitCleanup @Inject constructor(
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val logger: Logger,
    private val historyDatabase: HistoryDatabase,
    private val activity: Activity,
) : ExitCleanup {
    override suspend fun cleanUp() {
        if (userPreferencesDataStore.clearCacheExit.get()) {
            WebUtils.clearCache(activity)
            logger.log(TAG, "Cache Cleared")
        }
        if (userPreferencesDataStore.clearHistoryExitEnabled.get()) {
            WebUtils.clearHistory(activity, historyDatabase)
            logger.log(TAG, "History Cleared")
        }
        if (userPreferencesDataStore.clearCookiesExitEnabled.get()) {
            WebUtils.clearCookies()
            logger.log(TAG, "Cookies Cleared")
        }
        if (userPreferencesDataStore.clearWebStorageExitEnabled.get()) {
            WebUtils.clearWebStorage()
            logger.log(TAG, "WebStorage Cleared")
        }
    }

    companion object {
        const val TAG = "NormalExitCleanup"
    }
}
