package acr.browser.lightning.browser.cleanup

import acr.browser.lightning.log.Logger
import acr.browser.lightning.utils.WebUtils
import javax.inject.Inject

/**
 * Exit cleanup that should be run when the incognito process is exited on API >= 28. This cleanup
 * clears cookies and all web data, which can be done without affecting
 */
class EnhancedIncognitoExitCleanup @Inject constructor(
    private val logger: Logger,
    private val webUtils: WebUtils,
) : ExitCleanup {
    override suspend fun cleanUp() {
        webUtils.clearCache()
        logger.log(TAG, "Cache Cleared")
        webUtils.clearCookies()
        logger.log(TAG, "Cookies Cleared")
        webUtils.clearWebStorage()
        logger.log(TAG, "WebStorage Cleared")
    }

    companion object {
        private const val TAG = "EnhancedIncognitoExitCleanup"
    }
}
