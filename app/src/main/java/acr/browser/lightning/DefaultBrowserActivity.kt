package acr.browser.lightning

import acr.browser.lightning.browser.BrowserActivity

/**
 * The default browsing experience.
 */
class DefaultBrowserActivity : BrowserActivity() {
    override fun isIncognito(): Boolean = false
}
