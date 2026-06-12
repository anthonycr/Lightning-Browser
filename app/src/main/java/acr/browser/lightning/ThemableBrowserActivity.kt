package acr.browser.lightning

import acr.browser.lightning.browser.di.injector
import acr.browser.lightning.browser.ui.TabConfiguration
import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.preference.datastore.getUnsafe
import acr.browser.lightning.utils.ThemeUtils
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import javax.inject.Inject

/**
 * A theme aware activity that updates its theme based on the user preferences.
 */
abstract class ThemableBrowserActivity : AppCompatActivity() {

    @Inject
    internal lateinit var userPreferencesDataStore: UserPreferencesDataStore

    private var themeId: AppTheme = AppTheme.LIGHT
    private var tabConfiguration: TabConfiguration = TabConfiguration.DRAWER_BOTTOM
    private var shouldRunOnResumeActions = false

    /**
     * Override this to provide an alternate theme that should be set for every instance of this
     * activity regardless of the user's preference.
     */
    @StyleRes
    protected open fun provideThemeOverride(): Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        injector.inject(this)
        themeId = userPreferencesDataStore.useTheme.getUnsafe()
        tabConfiguration = userPreferencesDataStore.tabConfiguration.getUnsafe()

        // set the theme
        setTheme(
            provideThemeOverride() ?: when (userPreferencesDataStore.useTheme.getUnsafe()) {
                AppTheme.LIGHT -> R.style.Theme_LightTheme
                AppTheme.DARK -> R.style.Theme_DarkTheme
                AppTheme.BLACK -> R.style.Theme_BlackTheme
            }
        )
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        resetPreferences()
    }

    private fun resetPreferences() {
        // TODO: Fix
        if (userPreferencesDataStore.useBlackStatusBar.getUnsafe() ||
            userPreferencesDataStore.tabConfiguration.getUnsafe() == TabConfiguration.DESKTOP
        ) {
            window.statusBarColor = Color.BLACK
        } else {
            window.statusBarColor = ThemeUtils.getStatusBarColor(this)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && shouldRunOnResumeActions) {
            shouldRunOnResumeActions = false
            onWindowVisibleToUserAfterResume()
        }
    }

    /**
     * Called after the activity is resumed
     * and the UI becomes visible to the user.
     * Called by onWindowFocusChanged only if
     * onResume has been called.
     */
    protected open fun onWindowVisibleToUserAfterResume() = Unit

    override fun onResume() {
        super.onResume()
        resetPreferences()
        shouldRunOnResumeActions = true
        val nextTabConfiguration = userPreferencesDataStore.tabConfiguration.getUnsafe()
        if (themeId != userPreferencesDataStore.useTheme.getUnsafe() || tabConfiguration != nextTabConfiguration) {
            restart()
        }
    }

    protected fun restart() {
        finish()
        startActivity(Intent(this, javaClass))
    }
}
