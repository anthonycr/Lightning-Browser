package acr.browser.lightning._browser2.theme

import acr.browser.lightning.utils.ThemeUtils
import android.app.Activity

/**
 * The default theme attribute provider that delegates to the activity.
 */
class DefaultThemeProvider(private val activity: Activity) : ThemeProvider {

    override fun color(attrRes: Int): Int =
        ThemeUtils.getColor(activity, attrRes)

}
