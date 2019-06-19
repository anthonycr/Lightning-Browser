package acr.browser.lightning.settings.activity

import acr.browser.lightning.R
import acr.browser.lightning.di.injector
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.utils.ThemeUtils
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import javax.inject.Inject

abstract class ThemableSettingsActivity : AppCompatPreferenceActivity() {

    private var themeId: Int = 0

    @Inject internal lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        injector.inject(this)
        themeId = userPreferences.useTheme

        // set the theme
        when (themeId) {
            0 -> {
                setTheme(R.style.Theme_SettingsTheme)
                this.window.setBackgroundDrawable(ColorDrawable(ThemeUtils.getPrimaryColor(this)))
            }
            1 -> {
                setTheme(R.style.Theme_SettingsTheme_Dark)
                this.window.setBackgroundDrawable(ColorDrawable(ThemeUtils.getPrimaryColorDark(this)))
            }
            2 -> {
                setTheme(R.style.Theme_SettingsTheme_Black)
                this.window.setBackgroundDrawable(ColorDrawable(ThemeUtils.getPrimaryColorDark(this)))
            }
        }
        super.onCreate(savedInstanceState)

        resetPreferences()
    }

    private fun resetPreferences() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (userPreferences.useBlackStatusBar) {
                window.statusBarColor = Color.BLACK
            } else {
                window.statusBarColor = ThemeUtils.getStatusBarColor(this)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resetPreferences()
        if (userPreferences.useTheme != themeId) {
            recreate()
        }
    }

}
