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
     * The network thread dispatcher, a limited parallelism window of [io].
     */
    val network: CoroutineDispatcher

    /**
     * The default dispatcher.
     */
    val default: CoroutineDispatcher

    /**
     * Creates a new single threaded dispatcher for use with databases, backed by [io] dispatcher.
     */
    fun createDatabaseDispatcher(): CoroutineDispatcher
}

/**
 * Default container for [CoroutineDispatchers].
 */
class CoroutineDispatcherProvider(
    override val main: CoroutineDispatcher,
    override val io: CoroutineDispatcher,
    override val default: CoroutineDispatcher
) : CoroutineDispatchers {
    override val network: CoroutineDispatcher = io.limitedParallelism(4)
    override fun createDatabaseDispatcher(): CoroutineDispatcher = io.limitedParallelism(1)
}
