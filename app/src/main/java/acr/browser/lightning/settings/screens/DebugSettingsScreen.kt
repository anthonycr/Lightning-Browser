package acr.browser.lightning.settings.screens

import acr.browser.lightning.R
import acr.browser.lightning.preference.DeveloperPreferenceStore
import acr.browser.lightning.resources.ResourceProvider
import acr.browser.lightning.settings.SettingsSnackBarState
import acr.browser.lightning.settings.framework.SettingsFrameworkPresenter
import acr.browser.lightning.settings.framework.SettingsFrameworkScreen
import acr.browser.lightning.settings.framework.SettingsFrameworkState
import acr.browser.lightning.settings.framework.ToggleState
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DebugSettingsScreen(
    resourceProvider: ResourceProvider,
    developerPreferenceStore: DeveloperPreferenceStore,
    onUp: () -> Unit
) {
    val presenter: SettingsFrameworkPresenter = viewModel(
        key = "debug",
        factory = SettingsFrameworkPresenter.Factory(
            settingsFrameworkState = {
                SettingsFrameworkState(
                    title = resourceProvider.stringResource(R.string.debug_title),
                    content = listOf(
                        ToggleState(
                            title = resourceProvider.stringResource(R.string.debug_leak_canary),
                            isChecked = { developerPreferenceStore.useLeakCanary.get() },
                            onToggle = {
                                developerPreferenceStore.useLeakCanary.set(it)
                                SettingsSnackBarState(
                                    resourceProvider.stringResource(R.string.app_restart)
                                )
                            }
                        )
                    )
                )
            }
        )
    )
    SettingsFrameworkScreen(presenter, onUp)
}
