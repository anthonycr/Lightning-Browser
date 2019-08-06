package acr.browser.lightning.extensions

import io.reactivex.Observable
import io.reactivex.Single
import java.io.IOException

/**
 * Filters the [Observable] to only instances of [T].
 */
inline fun <reified T> Observable<out Any>.filterInstance(): Observable<T> {
    return this.filter { it is T }.map { it as T }
}

/**
 * On an [IOException], resume with the value provided by the [mapper].
 */
inline fun <T> Single<T>.onIOExceptionResumeNext(
    crossinline mapper: (IOException) -> T
): Single<T> = this.onErrorResumeNext {
    if (it is IOException) {
        Single.just(mapper(it))
    } else {
        Single.error(it)
    }
}
