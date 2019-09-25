package acr.browser.lightning.browser.cleanup

import acr.browser.lightning.browser.activity.BrowserActivity
import android.webkit.WebView

/**
 * A command that runs as the browser instance is shutting down to clean up anything that needs to
 * be cleaned up. For instance, if the user has chosen to clear cache on exit or if incognito mode
 * is closing.
 */
interface ExitCleanup {

    /**
     * Clean up the instance of the browser with the provided [webView] and [context].
     */
    fun cleanUp(webView: WebView?, context: BrowserActivity)

}
