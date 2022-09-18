package acr.browser.lightning.extensions

/**
 * Returns the current object as an instance of [T] if it is the right type.
 */
inline fun <reified T> Any.takeIfInstance(): T? {
    return if (this is T) {
        this
    } else {
        null
    }
}
