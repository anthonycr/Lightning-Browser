package acr.browser.lightning.settings.screens

import acr.browser.lightning.R
import acr.browser.lightning.device.BuildInfo
import acr.browser.lightning.device.BuildType
import acr.browser.lightning.resources.ResourceProvider
import acr.browser.lightning.settings.framework.ClickableOnClick
import acr.browser.lightning.settings.framework.ClickableState
import acr.browser.lightning.settings.framework.SettingsFrameworkState
import acr.browser.lightning.settings.navigation.SettingsNavigation
import javax.inject.Inject

class RootSettingsScreen @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val buildInfo: BuildInfo,
) {
    fun createSettingsFrameworkState(): SettingsFrameworkState = SettingsFrameworkState(
        title = resourceProvider.stringResource(R.string.settings),
        content = listOf(
            ClickableState(
                title = resourceProvider.stringResource(R.string.settings_adblock),
                onClick = ClickableOnClick.Navigate(SettingsNavigation.ADBLOCK),
            ),
            ClickableState(
                title = resourceProvider.stringResource(R.string.settings_general),
                onClick = ClickableOnClick.Navigate(SettingsNavigation.GENERAL),
            ),
            ClickableState(
                title = resourceProvider.stringResource(R.string.bookmark_settings),
                onClick = ClickableOnClick.Navigate(SettingsNavigation.BOOKMARK),
            ),
            ClickableState(
                title = resourceProvider.stringResource(R.string.settings_display),
                onClick = ClickableOnClick.Navigate(SettingsNavigation.DISPLAY),
            ),
            ClickableState(
                title = resourceProvider.stringResource(R.string.settings_privacy),
                onClick = ClickableOnClick.Navigate(SettingsNavigation.PRIVACY),
            ),
            ClickableState(
                title = resourceProvider.stringResource(R.string.settings_advanced),
                onClick = ClickableOnClick.Navigate(SettingsNavigation.ADVANCED),
            ),
            ClickableState(
                title = resourceProvider.stringResource(R.string.settings_about),
                summary = { resourceProvider.stringResource(R.string.settings_about_explain) },
                onClick = ClickableOnClick.Navigate(SettingsNavigation.ABOUT),
            ),
            ClickableState(
                title = resourceProvider.stringResource(R.string.faq),
                summary = { resourceProvider.stringResource(R.string.faq_description) },
                onClick = ClickableOnClick.WebLink("http://acrdevelopment.org/lightning/faq"),
            ),
        ) + if (buildInfo.buildType == BuildType.DEBUG) {
            listOf(
                ClickableState(
                    title = resourceProvider.stringResource(R.string.debug_title),
                    onClick = ClickableOnClick.Navigate(SettingsNavigation.DEBUG),
                )
            )
        } else {
            emptyList()
        }
    )
}
