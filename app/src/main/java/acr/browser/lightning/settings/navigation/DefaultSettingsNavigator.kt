package acr.browser.lightning.settings.navigation

import acr.browser.lightning.concurrency.AppCoroutineScope
import acr.browser.lightning.settings.SettingsNavigation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Default implementation of [SettingsNavigator] that simply exposes the events triggered by
 * [navigateTo] as a [MutableSharedFlow].
 */
class DefaultSettingsNavigator @Inject constructor(
    private val appCoroutineScope: AppCoroutineScope
) : SettingsNavigator {

    override val events: MutableSharedFlow<SettingsNavigation> = MutableSharedFlow()

    override fun navigateTo(settingsNavigation: SettingsNavigation) {
        appCoroutineScope.launch {
            events.emit(settingsNavigation)
        }
    }
}
