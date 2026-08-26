package acr.browser.lightning.settings.navigation

import kotlinx.coroutines.flow.Flow

/**
 * Used to navigate within the settings screen.
 */
interface SettingsNavigator {

    /**
     * The navigation events.
     */
    val events: Flow<SettingsNavigation>

    /**
     * Navigate to the provided destination.
     */
    fun navigateTo(settingsNavigation: SettingsNavigation)
}

