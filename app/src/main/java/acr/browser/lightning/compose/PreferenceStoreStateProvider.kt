package acr.browser.lightning.compose

import acr.browser.lightning.concurrency.AppCoroutineScope
import acr.browser.lightning.preference.datastore.PreferenceStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Used to provide access to certain preferences that can be observed from compose.
 */
class PreferenceStoreStateProvider<T>(
    private val preference: PreferenceStore<T>,
    appCoroutineScope: AppCoroutineScope,
) : StateProvider<T> {

    /**
     * The state emitted by the preference, defaults to `null` until the first value is emitted.
     */
    override val state: MutableStateFlow<T?> = MutableStateFlow(null)

    init {
        appCoroutineScope.launch {
            state.emit(preference.get())
        }
    }
}

interface StateProvider<T> {
    val state: StateFlow<T?>
}
