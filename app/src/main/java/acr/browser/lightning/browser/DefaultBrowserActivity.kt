package acr.browser.lightning.browser

import acr.browser.lightning.R

/**
 * Created by anthonycr on 7/26/22.
 */
class DefaultBrowserActivity : BrowserActivity() {
    override fun isIncognito(): Boolean = false

    override fun menu(): Int = R.menu.main

    override fun homeIcon(): Int = R.drawable.ic_action_home
}
