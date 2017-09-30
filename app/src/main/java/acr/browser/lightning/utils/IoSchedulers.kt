package acr.browser.lightning.utils

import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Executors

/**
 * Schedulers for various io.
 */
object IoSchedulers {

    @JvmStatic
    val database = Schedulers.from(Executors.newSingleThreadExecutor())

}