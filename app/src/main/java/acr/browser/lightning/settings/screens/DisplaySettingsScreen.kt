package acr.browser.lightning.settings.screens

import acr.browser.lightning.AppTheme
import acr.browser.lightning.R
import acr.browser.lightning.browser.ui.TabConfiguration
import acr.browser.lightning.compose.StateProvider
import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.resources.ResourceProvider
import acr.browser.lightning.settings.SettingsBottomSheetChooserState
import acr.browser.lightning.settings.SettingsSnackBarState
import acr.browser.lightning.settings.framework.ClickableOnClick
import acr.browser.lightning.settings.framework.ClickableState
import acr.browser.lightning.settings.framework.SettingsFrameworkPresenter
import acr.browser.lightning.settings.framework.SettingsFrameworkScreen
import acr.browser.lightning.settings.framework.SettingsFrameworkState
import acr.browser.lightning.settings.framework.ToggleState
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.webkit.WebViewFeature
import javax.inject.Inject

class DisplaySettingsScreen @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val userPreferencesDataStore: UserPreferencesDataStore
) {
    val key = "display"

    fun createSettingsFrameworkState(): SettingsFrameworkState = SettingsFrameworkState(
        title = resourceProvider.stringResource(R.string.settings_display),
        content = listOf(
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
            ToggleState(
                enabled = {
                    WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING) ||
                        WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)
                },
                title = resourceProvider.stringResource(R.string.algorithmic_darkening_title),
                summary = { resourceProvider.stringResource(R.string.algorithmic_darkening_summary) },
                isChecked = { userPreferencesDataStore.algorithmicDarkeningEnabled.get() },
                onToggle = {
                    userPreferencesDataStore.algorithmicDarkeningEnabled.set(it)
                    null
                }
            ),
            ClickableState(
                title = resourceProvider.stringResource(R.string.theme),
                summary = {
                    userPreferencesDataStore.useTheme.get().toDisplayString(resourceProvider)
                },
                onClick = ClickableOnClick.ItemSelector(
                    produceState = {
                        SettingsBottomSheetChooserState(
                            title = resourceProvider.stringResource(R.string.theme),
                            values = AppTheme.entries.map { it.toDisplayString(resourceProvider) },
                            selected = AppTheme.entries.indexOf(userPreferencesDataStore.useTheme.get()),
                        )
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
                    produceState = {
                        SettingsBottomSheetChooserState(
                            title = resourceProvider.stringResource(R.string.tab_style_title),
                            values = TabConfiguration.entries.map {
                                it.toDisplayString(resourceProvider)
                            },
                            selected = TabConfiguration.entries.indexOf(
                                userPreferencesDataStore.tabConfiguration.get()
                            )
                        )
                    },
                    onSelected = {
                        ClickableOnClick.Snackbar {
                            userPreferencesDataStore.tabConfiguration.set(TabConfiguration.entries[it])
                            SettingsSnackBarState(
                                message = resourceProvider.stringResource(R.string.app_restart)
                            )
                        }
                    }
                )
            ),
            ClickableState(
                title = resourceProvider.stringResource(R.string.title_text_size),
                summary = {
                    when (userPreferencesDataStore.textSize.get()) {
                        0 -> resourceProvider.stringResource(R.string.size_largest)
                        1 -> resourceProvider.stringResource(R.string.size_large)
                        2 -> resourceProvider.stringResource(R.string.size_normal)
                        3 -> resourceProvider.stringResource(R.string.size_normal)
                        4 -> resourceProvider.stringResource(R.string.size_small)
                        5 -> resourceProvider.stringResource(R.string.size_smallest)
                        else -> throw IllegalArgumentException("Unsupported text size")
                    }
                },
                onClick = ClickableOnClick.TextSize(
                    produceTextSize = { userPreferencesDataStore.textSize.get() },
                    onSelected = {
                        ClickableOnClick.Action {
                            userPreferencesDataStore.textSize.set(it)
                        }
                    }
                )
            )
        )
    )
}

@Composable
fun DisplaySettingsScreen(
    blackStatusStateProvider: StateProvider<Boolean>,
    displaySettingsScreen: DisplaySettingsScreen,
    onUp: () -> Unit
) {
    SettingsFrameworkScreen(
        blackStatusStateProvider,
        viewModel(
            key = displaySettingsScreen.key,
            factory = SettingsFrameworkPresenter.Factory(
                settingsFrameworkState = {
                    displaySettingsScreen.createSettingsFrameworkState()
                }
            )
        ),
        onUp
    )
}

private fun AppTheme.toDisplayString(resourceProvider: ResourceProvider): String =
    resourceProvider.stringResource(
        when (this) {
            AppTheme.LIGHT -> R.string.light_theme
            AppTheme.DARK -> R.string.dark_theme
            AppTheme.BLACK -> R.string.black_theme
            AppTheme.SYSTEM -> R.string.system_theme
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
