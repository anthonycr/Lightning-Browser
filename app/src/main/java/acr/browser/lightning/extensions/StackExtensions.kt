package acr.browser.lightning.extensions

import java.util.*

/**
 * If the [Stack] is empty, null is returned, otherwise the item at the top of the stack is
 * returned.
 */
fun <T> Stack<T>.popIfNotEmpty(): T? {
    return if (empty()) {
        null
    } else {
        pop()
    }
}
