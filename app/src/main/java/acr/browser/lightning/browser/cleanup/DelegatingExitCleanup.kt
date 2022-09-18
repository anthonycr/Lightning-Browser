package acr.browser.lightning.browser.cleanup

import acr.browser.lightning.Capabilities
import acr.browser.lightning.browser.BrowserActivity
import acr.browser.lightning.DefaultBrowserActivity
import acr.browser.lightning.isSupported
import android.app.Activity
import android.webkit.WebView
import javax.inject.Inject

/**
 * Exit cleanup that determines which sort of cleanup to do at runtime. It determines which cleanup
 * to perform based on the API version and whether we are in incognito mode or normal mode.
 */
class DelegatingExitCleanup @Inject constructor(
    private val basicIncognitoExitCleanup: BasicIncognitoExitCleanup,
    private val enhancedIncognitoExitCleanup: EnhancedIncognitoExitCleanup,
    private val normalExitCleanup: NormalExitCleanup,
    private val activity: Activity
) : ExitCleanup {
    override fun cleanUp() {
        when {
            activity is DefaultBrowserActivity -> normalExitCleanup.cleanUp()
            Capabilities.FULL_INCOGNITO.isSupported -> enhancedIncognitoExitCleanup.cleanUp()
            else -> basicIncognitoExitCleanup.cleanUp()
        }
    }
}
