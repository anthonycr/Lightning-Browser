package acr.browser.lightning.concurrency

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Used to provide access to suspend functions that can be observed from compose.
 */
class DeferredStateProvider<T>(
    private val provideState: suspend () -> T,
    appCoroutineScope: AppCoroutineScope,
) : StateProvider<T> {

    /**
     * The state emitted by the provide function, defaults to `null` until the first value is
     * emitted.
     */
    override val state: MutableStateFlow<T?> = MutableStateFlow(null)

    init {
        appCoroutineScope.launch {
            state.emit(provideState())
        }
    }
}

interface StateProvider<T> {
    val state: StateFlow<T?>
}
