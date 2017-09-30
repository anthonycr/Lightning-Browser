package acr.browser.lightning.utils

import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Executors

/**
 * Created by anthonycr on 9/30/17.
 */
object IoSchedulers {

    @JvmStatic
    val database = Schedulers.from(Executors.newSingleThreadExecutor())

}