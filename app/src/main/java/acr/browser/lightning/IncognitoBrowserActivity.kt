package acr.browser.lightning

import acr.browser.lightning.browser.BrowserActivity
import android.app.Activity
import android.content.Intent
import androidx.core.net.toUri

/**
 * The incognito browsing experience.
 */
class IncognitoBrowserActivity : BrowserActivity() {

    override fun provideThemeOverride(): Int = R.style.Theme_DarkTheme

    override fun isIncognito(): Boolean = true

    override fun menu(): Int = R.menu.incognito

    override fun homeIcon(): Int = R.drawable.incognito_mode

    companion object {
        /**
         * Creates an intent to launch the browser with an optional [url] to load.
         */
        fun intent(activity: Activity, url: String? = null): Intent =
            Intent(activity, IncognitoBrowserActivity::class.java).apply {
                data = url?.let(String::toUri)
            }

        /**
         * Launch the browser with an optional [url] to load.
         */
        fun launch(activity: Activity, url: String?) {
            activity.startActivity(intent(activity, url))
        }
    }
}
