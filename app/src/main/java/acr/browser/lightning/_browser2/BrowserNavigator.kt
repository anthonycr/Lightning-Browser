package acr.browser.lightning._browser2

import acr.browser.lightning.R
import acr.browser.lightning._browser2.download.DownloadPermissionsHelper
import acr.browser.lightning._browser2.download.PendingDownload
import acr.browser.lightning.browser.activity.BrowserActivity
import acr.browser.lightning.database.HistoryEntry
import acr.browser.lightning.extensions.copyToClipboard
import acr.browser.lightning.extensions.snackbar
import acr.browser.lightning.log.Logger
import acr.browser.lightning.reading.activity.ReadingActivity
import acr.browser.lightning.settings.activity.SettingsActivity
import acr.browser.lightning.utils.IntentUtils
import acr.browser.lightning.utils.Utils
import acr.browser.lightning.utils.isSpecialUrl
import android.app.Activity
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import javax.inject.Inject

/**
 * Created by anthonycr on 9/15/20.
 */
class BrowserNavigator @Inject constructor(
    private val activity: Activity,
    private val clipboardManager: ClipboardManager,
    private val logger: Logger,
    private val downloadPermissionsHelper: DownloadPermissionsHelper
) : BrowserContract.Navigator {

    override fun openSettings() {
        activity.startActivity(Intent(activity, SettingsActivity::class.java))
    }

    override fun openReaderMode(url: String) {
        ReadingActivity.launch(activity, url)
    }

    override fun sharePage(url: String, title: String?) {
        IntentUtils(activity).shareUrl(url, title)
    }

    override fun copyPageLink(url: String) {
        clipboardManager.copyToClipboard(url)
        activity.snackbar(R.string.message_link_copied)
    }

    override fun closeBrowser() {
        activity.finish()
    }

    override fun addToHomeScreen(url: String, title: String, favicon: Bitmap?) {
        Utils.createShortcut(activity, url, title, favicon)
        logger.log(TAG, "Creating shortcut: $title $url")
    }

    override fun download(pendingDownload: PendingDownload) {
        downloadPermissionsHelper.download(
            activity = activity,
            url = pendingDownload.url,
            userAgent = pendingDownload.userAgent,
            contentDisposition = pendingDownload.contentDisposition,
            mimeType = pendingDownload.mimeType,
            contentLength = pendingDownload.contentLength
        )
    }

    override fun backgroundBrowser() {
        activity.moveTaskToBack(true)
    }

    companion object {
        private const val TAG = "BrowserNavigator"
    }

}
