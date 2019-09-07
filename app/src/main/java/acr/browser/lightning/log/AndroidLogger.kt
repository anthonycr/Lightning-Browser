package acr.browser.lightning.log

import android.util.Log
import dagger.Reusable
import javax.inject.Inject

/**
 * A logger that utilizes the [Log] class.
 */
@Reusable
class AndroidLogger @Inject constructor() : Logger {

    override fun log(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun log(tag: String, message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
    }

}
