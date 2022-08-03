package acr.browser.lightning.browser.theme

import acr.browser.lightning.AppTheme
import acr.browser.lightning.R
import acr.browser.lightning.extensions.color
import acr.browser.lightning.preference.UserPreferences
import android.app.Application
import javax.inject.Inject

/**
 * The theme provider that should be used until [DefaultThemeProvider] can be injected safely
 * throughout the codebase.
 */
class LegacyThemeProvider @Inject constructor(
    private val application: Application,
    private val userPreferences: UserPreferences
) : ThemeProvider {

    val theme: AppTheme
        get() = userPreferences.useTheme

    override fun color(attrRes: Int): Int = when (attrRes) {
        R.attr.colorPrimary -> when (theme) {
            AppTheme.LIGHT -> application.color(R.color.primary_color)
            AppTheme.DARK -> application.color(R.color.primary_color_dark)
            AppTheme.BLACK -> application.color(R.color.black)
        }
        R.attr.drawerBackground -> when (theme) {
            AppTheme.LIGHT -> application.color(R.color.drawer_background)
            AppTheme.DARK -> application.color(R.color.drawer_background_dark)
            AppTheme.BLACK -> application.color(R.color.black)
        }
        R.attr.autoCompleteBackgroundColor -> when (theme) {
            AppTheme.LIGHT -> application.color(R.color.white)
            AppTheme.DARK -> application.color(R.color.divider_dark)
            AppTheme.BLACK -> application.color(R.color.gray_dark)
        }
        R.attr.autoCompleteTitleColor -> when (theme) {
            AppTheme.LIGHT -> application.color(R.color.black)
            AppTheme.DARK -> application.color(R.color.white)
            AppTheme.BLACK -> application.color(R.color.white)
        }
        R.attr.autoCompleteUrlColor -> when (theme) {
            AppTheme.LIGHT -> application.color(R.color.hint_text_light_theme)
            AppTheme.DARK -> application.color(R.color.hint_text_dark_theme)
            AppTheme.BLACK -> application.color(R.color.hint_text_dark_theme)
        }
        else -> error("Unsupported color")
    }
}
