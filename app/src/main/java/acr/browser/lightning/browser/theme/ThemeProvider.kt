package acr.browser.lightning.browser.theme

import acr.browser.lightning.AppTheme

/**
 * Provides the current app theme.
 */
interface ThemeProvider {

    /**
     * The current app theme.
     */
    suspend fun appTheme(): AppTheme

}
