package acr.browser.lightning.settings

import acr.browser.lightning.settings.framework.SettingsFrameworkState
import acr.browser.lightning.settings.screens.AboutSettingsScreen
import acr.browser.lightning.settings.screens.AdBlockSettingsScreen
import acr.browser.lightning.settings.screens.AdvancedSettingsScreen
import acr.browser.lightning.settings.screens.BookmarkSettingsScreen
import acr.browser.lightning.settings.screens.DebugSettingsScreen
import acr.browser.lightning.settings.screens.DisplaySettingsScreen
import acr.browser.lightning.settings.screens.GeneralSettingsScreen
import acr.browser.lightning.settings.screens.PrivacySettingsScreen
import javax.inject.Inject

class SettingsScreenStateProvider @Inject constructor(
    private val aboutSettingsScreen: AboutSettingsScreen,
    private val adBlockSettingsScreen: AdBlockSettingsScreen,
    private val advancedSettingsScreen: AdvancedSettingsScreen,
    private val bookmarkSettingsScreen: BookmarkSettingsScreen,
    private val debugSettingsScreen: DebugSettingsScreen,
    private val displaySettingsScreen: DisplaySettingsScreen,
    private val generalSettingsScreen: GeneralSettingsScreen,
    private val privacySettingsScreen: PrivacySettingsScreen,
) {

    fun provideState(
        settingsNavigation: SettingsNavigation
    ): SettingsFrameworkState = when (settingsNavigation) {
        SettingsNavigation.ROOT -> error("Unsupported")
        SettingsNavigation.ADBLOCK -> adBlockSettingsScreen.createSettingsFrameworkState()
        SettingsNavigation.GENERAL -> generalSettingsScreen.createSettingsFrameworkState()
        SettingsNavigation.BOOKMARK -> bookmarkSettingsScreen.createSettingsFrameworkState()
        SettingsNavigation.DISPLAY -> displaySettingsScreen.createSettingsFrameworkState()
        SettingsNavigation.PRIVACY -> privacySettingsScreen.createSettingsFrameworkState()
        SettingsNavigation.ADVANCED -> advancedSettingsScreen.createSettingsFrameworkState()
        SettingsNavigation.ABOUT -> aboutSettingsScreen.createSettingsFrameworkState()
        SettingsNavigation.FAQ -> error("Unsupported")
        SettingsNavigation.LICENSES -> error("Unsupported")
        SettingsNavigation.DEBUG -> debugSettingsScreen.createSettingsFrameworkState()
    }
}
