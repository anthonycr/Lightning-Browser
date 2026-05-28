package acr.browser.lightning.settings.screens

import acr.browser.lightning.AppTheme
import acr.browser.lightning.R
import acr.browser.lightning.browser.ui.TabConfiguration
import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.resources.ResourceProvider
import acr.browser.lightning.settings.SettingsSnackBarState
import acr.browser.lightning.settings.framework.ClickableOnClick
import acr.browser.lightning.settings.framework.ClickableState
import acr.browser.lightning.settings.framework.SettingsFrameworkPresenter
import acr.browser.lightning.settings.framework.SettingsFrameworkScreen
import acr.browser.lightning.settings.framework.SettingsFrameworkState
import acr.browser.lightning.settings.framework.ToggleState
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DisplaySettingsScreen(
    userPreferencesDataStore: UserPreferencesDataStore,
    resourceProvider: ResourceProvider,
    onUp: () -> Unit
) {
    val presenter: SettingsFrameworkPresenter = viewModel(
        key = "display",
        factory = SettingsFrameworkPresenter.Factory(
            settingsFrameworkState = {
                SettingsFrameworkState(
                    title = resourceProvider.stringResource(R.string.settings_display),
                    content = listOf(
                        ToggleState(
                            title = resourceProvider.stringResource(R.string.swap_bookmarks_and_tabs),
                            isChecked = { userPreferencesDataStore.bookmarksAndTabsSwapped.get() },
                            onToggle = {
                                userPreferencesDataStore.bookmarksAndTabsSwapped.set(it)
                                null
                            }
                        ),
                        ToggleState(
                            title = resourceProvider.stringResource(R.string.fullScreenOption),
                            isChecked = { userPreferencesDataStore.hideStatusBarEnabled.get() },
                            onToggle = {
                                userPreferencesDataStore.hideStatusBarEnabled.set(it)
                                null
                            }
                        ),
                        ToggleState(
                            title = resourceProvider.stringResource(R.string.fullscreen),
                            isChecked = { userPreferencesDataStore.fullScreenEnabled.get() },
                            onToggle = {
                                userPreferencesDataStore.fullScreenEnabled.set(it)
                                null
                            }
                        ),
                        ToggleState(
                            title = resourceProvider.stringResource(R.string.settings_black_status_bar),
                            isChecked = { userPreferencesDataStore.useBlackStatusBar.get() },
                            onToggle = {
                                userPreferencesDataStore.useBlackStatusBar.set(it)
                                null
                            }
                        ),
                        ToggleState(
                            title = resourceProvider.stringResource(R.string.wideViewPort),
                            summary = { resourceProvider.stringResource(R.string.recommended) },
                            isChecked = { userPreferencesDataStore.useWideViewPortEnabled.get() },
                            onToggle = {
                                userPreferencesDataStore.useWideViewPortEnabled.set(it)
                                null
                            }
                        ),
                        ToggleState(
                            title = resourceProvider.stringResource(R.string.overViewMode),
                            summary = { resourceProvider.stringResource(R.string.recommended) },
                            isChecked = { userPreferencesDataStore.overviewModeEnabled.get() },
                            onToggle = {
                                userPreferencesDataStore.overviewModeEnabled.set(it)
                                null
                            }
                        ),
                        ToggleState(
                            title = resourceProvider.stringResource(R.string.reflow),
                            isChecked = { userPreferencesDataStore.textReflowEnabled.get() },
                            onToggle = {
                                userPreferencesDataStore.textReflowEnabled.set(it)
                                null
                            }
                        ),
                        ClickableState(
                            title = resourceProvider.stringResource(R.string.theme),
                            summary = {
                                userPreferencesDataStore.useTheme.get()
                                    .toDisplayString(resourceProvider)
                            },
                            onClick = ClickableOnClick.ItemSelector(
                                title = resourceProvider.stringResource(R.string.theme),
                                values = AppTheme.entries.map { it.toDisplayString(resourceProvider) },
                                selected = {
                                    AppTheme.entries.indexOf(userPreferencesDataStore.useTheme.get())
                                },
                                onSelected = {
                                    ClickableOnClick.Snackbar {
                                        userPreferencesDataStore.useTheme.set(AppTheme.entries[it])
                                        SettingsSnackBarState(
                                            message = resourceProvider.stringResource(R.string.app_restart)
                                        )
                                    }
                                }
                            )
                        ),
                        ClickableState(
                            title = resourceProvider.stringResource(R.string.tab_style_title),
                            summary = {
                                userPreferencesDataStore.tabConfiguration.get()
                                    .toDisplayString(resourceProvider)
                            },
                            onClick = ClickableOnClick.ItemSelector(
                                title = resourceProvider.stringResource(R.string.tab_style_title),
                                values = TabConfiguration.entries.map {
                                    it.toDisplayString(resourceProvider)
                                },
                                selected = {
                                    TabConfiguration.entries.indexOf(userPreferencesDataStore.tabConfiguration.get())
                                },
                                onSelected = {
                                    ClickableOnClick.Snackbar {
                                        userPreferencesDataStore.tabConfiguration.set(
                                            TabConfiguration.entries[it]
                                        )
                                        SettingsSnackBarState(
                                            message = resourceProvider.stringResource(R.string.app_restart)
                                        )
                                    }
                                }
                            )
                        ),
                        // TODO: Text size
                    )
                )
            }
        )
    )
    SettingsFrameworkScreen(presenter, onUp)
}

private fun AppTheme.toDisplayString(resourceProvider: ResourceProvider): String =
    resourceProvider.stringResource(
        when (this) {
            AppTheme.LIGHT -> R.string.light_theme
            AppTheme.DARK -> R.string.dark_theme
            AppTheme.BLACK -> R.string.black_theme
        }
    )

private fun TabConfiguration.toDisplayString(resourceProvider: ResourceProvider): String =
    resourceProvider.stringResource(
        when (this) {
            TabConfiguration.DESKTOP -> R.string.tab_style_desktop
            TabConfiguration.DRAWER_SIDE -> R.string.tab_style_side_drawer
            TabConfiguration.DRAWER_BOTTOM -> R.string.tab_style_bottom_drawer
        }
    )
