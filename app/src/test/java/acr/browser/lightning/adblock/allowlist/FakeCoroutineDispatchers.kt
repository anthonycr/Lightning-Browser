package acr.browser.lightning.adblock.allowlist

import acr.browser.lightning.concurrency.CoroutineDispatchers
import kotlinx.coroutines.CoroutineDispatcher

class FakeCoroutineDispatchers(
    coroutineDispatcher: CoroutineDispatcher
) : CoroutineDispatchers {
    override val main: CoroutineDispatcher = coroutineDispatcher
    override val io: CoroutineDispatcher = coroutineDispatcher
    override val default: CoroutineDispatcher = coroutineDispatcher
}
