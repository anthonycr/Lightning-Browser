package acr.browser.lightning.browser

import android.content.Intent

/**
 * An adapter between [BrowserContract.View] and the [BrowserActivity] that creates partial states
 * to render in the activity.
 */
class BrowserStateAdapter(private val browserActivity: BrowserActivity) : BrowserContract.View {

    override fun showAddBookmarkDialog(title: String, url: String, folders: List<String>) {
        browserActivity.showAddBookmarkDialog(title, url, folders)
    }

    override fun showEditBookmarkDialog(
        title: String,
        url: String,
        folder: String,
        folders: List<String>
    ) {
        browserActivity.showEditBookmarkDialog(title, url, folder, folders)
    }

    override fun showEditFolderDialog(title: String) {
        browserActivity.showEditFolderDialog(title)
    }

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
