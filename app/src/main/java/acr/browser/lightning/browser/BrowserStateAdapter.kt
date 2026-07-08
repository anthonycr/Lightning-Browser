package acr.browser.lightning.browser

import android.content.Intent

/**
 * An adapter between [BrowserContract.View] and the [BrowserActivity] that creates partial states
 * to render in the activity.
 */
class BrowserStateAdapter(private val browserActivity: BrowserActivity) : BrowserContract.View {

    override fun showToolbar() {
        browserActivity.showToolbar()
    }

    override fun showToolsDialog(areAdsAllowed: Boolean, shouldShowAdBlockOption: Boolean) {
        browserActivity.showToolsDialog(areAdsAllowed, shouldShowAdBlockOption)
    }

    override fun showLocalFileBlockedDialog() {
        browserActivity.showLocalFileBlockedDialog()
    }

    override fun showFileChooser(intent: Intent) {
        browserActivity.showFileChooser(intent)
    }
}
