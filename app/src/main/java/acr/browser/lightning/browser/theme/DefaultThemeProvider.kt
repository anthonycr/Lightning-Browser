package acr.browser.lightning.browser.theme

import acr.browser.lightning.AppTheme
import acr.browser.lightning.compose.asColorScheme
import acr.browser.lightning.di.IncognitoMode
import acr.browser.lightning.preference.UserPreferencesDataStore
import android.app.Application
import android.content.res.Configuration
import androidx.compose.material3.ColorScheme
import javax.inject.Inject

/**
 * The default theme attribute provider that delegates to the activity.
 */
class DefaultThemeProvider @Inject constructor(
    @IncognitoMode private val isIncognitoMode: Boolean,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val application: Application,
) : ThemeProvider {

    override suspend fun appTheme(): AppTheme = if (isIncognitoMode) {
        AppTheme.DARK
    } else {
        userPreferencesDataStore.useTheme.get()
    }

    override suspend fun colorScheme(): ColorScheme {
        val appTheme = appTheme()

        return appTheme.asColorScheme(
            application.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        )
    }

}
