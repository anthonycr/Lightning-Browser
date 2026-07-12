package acr.browser.lightning.browser.theme

import acr.browser.lightning.AppTheme
import acr.browser.lightning.browser.di.IncognitoMode
import acr.browser.lightning.preference.UserPreferencesDataStore
import javax.inject.Inject

/**
 * The default theme attribute provider that delegates to the activity.
 */
class DefaultThemeProvider @Inject constructor(
    @IncognitoMode private val isIncognitoMode: Boolean,
    private val userPreferencesDataStore: UserPreferencesDataStore,
) : ThemeProvider {

    override suspend fun appTheme(): AppTheme = if (isIncognitoMode) {
        AppTheme.DARK
    } else {
        userPreferencesDataStore.useTheme.get()
    }

}
