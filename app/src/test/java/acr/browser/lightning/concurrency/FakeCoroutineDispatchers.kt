package acr.browser.lightning.concurrency

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler

class FakeCoroutineDispatchers(coroutineDispatcher: CoroutineDispatcher) : CoroutineDispatchers {

    constructor(testCoroutineScheduler: TestCoroutineScheduler) : this(
        StandardTestDispatcher(testCoroutineScheduler)
    )

    override val main: CoroutineDispatcher = coroutineDispatcher
    override val io: CoroutineDispatcher = coroutineDispatcher
    override val default: CoroutineDispatcher = coroutineDispatcher
}
