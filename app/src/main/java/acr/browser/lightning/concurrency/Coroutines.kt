package acr.browser.lightning.concurrency

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Common dispatchers used throughout the app.
 */
interface CoroutineDispatchers {

    /**
     * The main thread dispatcher.
     */
    val main: CoroutineDispatcher

    /**
     * The disk thread dispatcher.
     */
    val io: CoroutineDispatcher

    /**
     * The default dispatcher.
     */
    val default: CoroutineDispatcher

}

/**
 * Default container for [CoroutineDispatchers].
 */
class CoroutineDispatcherProvider(
    override val main: CoroutineDispatcher,
    override val io: CoroutineDispatcher,
    override val default: CoroutineDispatcher
) : CoroutineDispatchers
