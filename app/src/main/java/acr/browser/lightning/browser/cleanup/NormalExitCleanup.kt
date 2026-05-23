package acr.browser.lightning.browser.cleanup

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
    private val activity: Activity,
    private val webUtils: WebUtils,
) : ExitCleanup {
    override suspend fun cleanUp() {
        if (userPreferencesDataStore.clearCacheExit.get()) {
            webUtils.clearCache(activity)
            logger.log(TAG, "Cache Cleared")
        }
        if (userPreferencesDataStore.clearHistoryExitEnabled.get()) {
            webUtils.clearHistory()
            logger.log(TAG, "History Cleared")
        }
        if (userPreferencesDataStore.clearCookiesExitEnabled.get()) {
            webUtils.clearCookies()
            logger.log(TAG, "Cookies Cleared")
        }
        if (userPreferencesDataStore.clearWebStorageExitEnabled.get()) {
            webUtils.clearWebStorage()
            logger.log(TAG, "WebStorage Cleared")
        }
    }

    companion object {
        const val TAG = "NormalExitCleanup"
    }
}
