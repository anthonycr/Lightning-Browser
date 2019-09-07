package acr.browser.lightning.log

import dagger.Reusable
import javax.inject.Inject

/**
 * A logger that doesn't log.
 */
@Reusable
class NoOpLogger @Inject constructor() : Logger {

    override fun log(tag: String, message: String) = Unit

    override fun log(tag: String, message: String, throwable: Throwable) = Unit

}
