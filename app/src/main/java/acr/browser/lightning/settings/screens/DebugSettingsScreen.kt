package acr.browser.lightning.settings.screens

import acr.browser.lightning.R
import acr.browser.lightning.preference.DeveloperPreferenceStore
import acr.browser.lightning.resources.ResourceProvider
import acr.browser.lightning.settings.framework.SettingsFrameworkState
import acr.browser.lightning.settings.framework.SettingsSnackBarState
import acr.browser.lightning.settings.framework.ToggleState
import javax.inject.Inject

class DebugSettingsScreen @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val developerPreferenceStore: DeveloperPreferenceStore
) {
    fun createSettingsFrameworkState(): SettingsFrameworkState = SettingsFrameworkState(
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
