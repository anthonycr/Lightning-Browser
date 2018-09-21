package acr.browser.lightning.utils

/**
 * An option type, taken from the Arrow library.
 */
sealed class Option<out T> {

    /**
     * A type representing the presences of [some] [T].
     */
    data class Some<T>(val some: T) : Option<T>()

    /**
     * A type representing the absence of [T].
     */
    object None : Option<Nothing>()

    companion object {
        /**
         * Creates an [Option] from the potentially nullable [value].
         */
        fun <T> fromNullable(value: T?): Option<T> =
            if (value != null) {
                Option.Some(value)
            } else {
                Option.None
            }
    }

}

/**
 * Returns the value held by the [Option] as a nullable [T].
 */
fun <T> Option<T>.value(): T? = when (this) {
    is Option.Some -> some
    Option.None -> null
}
