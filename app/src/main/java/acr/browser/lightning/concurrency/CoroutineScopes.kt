package acr.browser.lightning.concurrency

import kotlinx.coroutines.CoroutineScope

/**
 * The coroutine scope that lives for the duration of the entire app process.
 */
class AppCoroutineScope(coroutineScope: CoroutineScope) : CoroutineScope by coroutineScope

/**
 * The coroutine scope that lives for the duration of the browser. This is usually roughly the same
 * scope as [AppCoroutineScope], but it's possible for it to be shorter.
 */
class BrowserCoroutineScope(coroutineScope: CoroutineScope) : CoroutineScope by coroutineScope

/**
 * The coroutine scope that lives for the duration of a tab. All work associated to a specific tab
 * should be scheduled using it.
 */
class TabCoroutineScope(coroutineScope: CoroutineScope) : CoroutineScope by coroutineScope
