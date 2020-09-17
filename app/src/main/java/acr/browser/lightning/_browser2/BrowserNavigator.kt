package acr.browser.lightning._browser2

import acr.browser.lightning.R
import acr.browser.lightning.extensions.copyToClipboard
import acr.browser.lightning.extensions.snackbar
import acr.browser.lightning.reading.activity.ReadingActivity
import acr.browser.lightning.settings.activity.SettingsActivity
import acr.browser.lightning.utils.IntentUtils
import android.app.Activity
import android.content.ClipboardManager
import android.content.Intent
import javax.inject.Inject

/**
 * Created by anthonycr on 9/15/20.
 */
class BrowserNavigator @Inject constructor(
    private val activity: Activity,
    private val clipboardManager: ClipboardManager
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

}
