package acr.browser.lightning.browser

import acr.browser.lightning.IncognitoBrowserActivity
import acr.browser.lightning.browser.cleanup.ExitCleanup
import acr.browser.lightning.browser.di.IncognitoMode
import acr.browser.lightning.browser.download.PendingDownload
import acr.browser.lightning.concurrency.AppCoroutineScope
import acr.browser.lightning.download.FileDownloader
import acr.browser.lightning.extensions.copyToClipboard
import acr.browser.lightning.log.Logger
import acr.browser.lightning.settings.activity.SettingsActivity
import acr.browser.lightning.utils.IntentUtils
import acr.browser.lightning.utils.Utils
import android.app.ActivityManager
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The navigator implementation.
 */
class BrowserNavigator @Inject constructor(
    private val activity: FragmentActivity,
    private val clipboardManager: ClipboardManager,
    private val logger: Logger,
    private val exitCleanup: ExitCleanup,
    @IncognitoMode private val incognitoMode: Boolean,
    private val activityManager: ActivityManager,
    private val appCoroutineScope: AppCoroutineScope,
    private val fileDownloader: FileDownloader,
) : BrowserContract.Navigator {

    override fun openSettings() {
        activity.startActivity(Intent(activity, SettingsActivity::class.java))
    }

    override fun sharePage(url: String, title: String?) {
        IntentUtils(activity).shareUrl(url, title)
    }

    override fun copyPageLink(url: String) {
        clipboardManager.copyToClipboard(url)
    }

    override suspend fun closeBrowser() {
        exitCleanup.cleanUp()
        if (incognitoMode) {
            activityManager.appTasks
                .first { it.taskInfo?.topActivity?.className == IncognitoBrowserActivity::class.java.name }
                .finishAndRemoveTask()
        } else {
            activity.finish()
        }
    }

    override fun addToHomeScreen(url: String, title: String, favicon: Bitmap?): Boolean {
        logger.log(TAG, "Creating shortcut: $title $url")
        return Utils.createShortcut(activity, url, title, favicon)
    }

    override fun download(pendingDownload: PendingDownload) {
        appCoroutineScope.launch {
            fileDownloader.download(pendingDownload)
        }
    }

    override fun backgroundBrowser() {
        if (incognitoMode) {
            appCoroutineScope.launch {
                exitCleanup.cleanUp()
                activityManager.appTasks
                    .first { it.taskInfo?.topActivity?.className == IncognitoBrowserActivity::class.java.name }
                    .finishAndRemoveTask()
            }
        } else {
            activity.moveTaskToBack(true)
        }
    }

    override fun launchIncognito(url: String?) {
        IncognitoBrowserActivity.launch(activity, url)
    }

    companion object {
        private const val TAG = "BrowserNavigator"
    }

}
