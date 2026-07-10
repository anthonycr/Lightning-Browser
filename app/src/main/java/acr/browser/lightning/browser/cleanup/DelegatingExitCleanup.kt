package acr.browser.lightning.browser.cleanup

import acr.browser.lightning.DefaultBrowserActivity
import android.app.Activity
import javax.inject.Inject

/**
 * Exit cleanup that determines which sort of cleanup to do at runtime. It determines which cleanup
 * to perform based on the API version and whether we are in incognito mode or normal mode.
 */
class DelegatingExitCleanup @Inject constructor(
    private val enhancedIncognitoExitCleanup: EnhancedIncognitoExitCleanup,
    private val normalExitCleanup: NormalExitCleanup,
    private val activity: Activity
) : ExitCleanup {
    override suspend fun cleanUp() {
        when {
            activity is DefaultBrowserActivity -> normalExitCleanup.cleanUp()
            else -> enhancedIncognitoExitCleanup.cleanUp()
        }
    }
}
