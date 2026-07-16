package acr.browser.lightning.browser.theme

import acr.browser.lightning.AppTheme
import androidx.compose.material3.ColorScheme

/**
 * Provides the current app theme.
 */
interface ThemeProvider {

    /**
     * The current app theme.
     */
    suspend fun appTheme(): AppTheme

    /**
     * The current app color scheme.
     */
    suspend fun colorScheme(): ColorScheme

}
