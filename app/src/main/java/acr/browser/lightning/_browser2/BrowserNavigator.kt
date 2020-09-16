package acr.browser.lightning._browser2

import acr.browser.lightning.reading.activity.ReadingActivity
import acr.browser.lightning.settings.activity.SettingsActivity
import android.app.Activity
import android.content.Intent
import javax.inject.Inject

/**
 * Created by anthonycr on 9/15/20.
 */
class BrowserNavigator @Inject constructor(
    private val activity: Activity
) : BrowserContract.Navigator {

    override fun openSettings() {
        activity.startActivity(Intent(activity, SettingsActivity::class.java))
    }

    override fun openReaderMode(url: String) {
        ReadingActivity.launch(activity, url)
    }

}
