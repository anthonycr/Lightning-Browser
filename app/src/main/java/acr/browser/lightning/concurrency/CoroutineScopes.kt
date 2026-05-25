package acr.browser.lightning.concurrency

import kotlinx.coroutines.CoroutineScope

class AppCoroutineScope(coroutineScope: CoroutineScope) : CoroutineScope by coroutineScope

class BrowserCoroutineScope(coroutineScope: CoroutineScope) : CoroutineScope by coroutineScope

class TabCoroutineScope(coroutineScope: CoroutineScope) : CoroutineScope by coroutineScope
