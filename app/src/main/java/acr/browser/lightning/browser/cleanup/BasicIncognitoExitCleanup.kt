package acr.browser.lightning.browser.cleanup

import acr.browser.lightning.browser.activity.BrowserActivity
import acr.browser.lightning.utils.WebUtils
import android.webkit.WebView
import javax.inject.Inject

/**
 * Exit cleanup that should run on API < 28 when the incognito instance is closed. This is
 * significantly less secure than on API > 28 since we can separate WebView data from
 */
class BasicIncognitoExitCleanup @Inject constructor() : ExitCleanup {
    override fun cleanUp(webView: WebView?, context: BrowserActivity) {
        // We want to make sure incognito mode is secure as possible without also breaking existing
        // browser instances.
        WebUtils.clearWebStorage()
    }
}
