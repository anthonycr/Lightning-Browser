package acr.browser.lightning.concurrency

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler

class FakeCoroutineDispatchers(
    private val coroutineDispatcher: CoroutineDispatcher
) : CoroutineDispatchers {

    constructor(testCoroutineScheduler: TestCoroutineScheduler) : this(
        StandardTestDispatcher(testCoroutineScheduler)
    )

    override val main: CoroutineDispatcher = coroutineDispatcher
    override val io: CoroutineDispatcher = coroutineDispatcher
    override val network: CoroutineDispatcher = coroutineDispatcher
    override val default: CoroutineDispatcher = coroutineDispatcher
    override fun createDatabaseDispatcher(): CoroutineDispatcher = coroutineDispatcher
}
