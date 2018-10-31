package acr.browser.lightning.log

/**
 * A logger.
 */
interface Logger {

    /**
     * Log the [message] for the provided [tag].
     */
    fun log(tag: String, message: String)

    /**
     * Log the [message] and [throwable] for the provided [tag].
     */
    fun log(tag: String, message: String, throwable: Throwable)

}
