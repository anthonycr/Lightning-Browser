package acr.browser.lightning._browser2

import acr.browser.lightning.R
import android.app.Activity
import android.content.Intent
import androidx.core.net.toUri

/**
 * Created by anthonycr on 7/26/22.
 */
class IncognitoBrowserActivity : BrowserActivity() {

    override fun provideThemeOverride(): Int = R.style.Theme_DarkTheme

    override fun isIncognito(): Boolean = true

    override fun menu(): Int = R.menu.incognito

    override fun homeIcon(): Int = R.drawable.incognito_mode

    companion object {
        fun intent(activity: Activity, url: String? = null): Intent =
            Intent(activity, IncognitoBrowserActivity::class.java).apply {
                data = url?.let(String::toUri)
            }

        fun launch(activity: Activity, url: String?) {
            activity.startActivity(intent(activity, url))
        }
    }
}
