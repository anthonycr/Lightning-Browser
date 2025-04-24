package acr.browser.lightning.extensions

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.io.IOException

/**
 * Filters the [Observable] to only instances of [T].
 */
inline fun <reified T : Any> Observable<out Any>.filterInstance(): Observable<T> {
    return this.filter { it is T }.map { it as T }
}

/**
 * On an [IOException], resume with the value provided by the [mapper].
 */
inline fun <T : Any> Single<T>.onIOExceptionResumeNext(
    crossinline mapper: (IOException) -> T
): Single<T> = this.onErrorResumeNext {
    if (it is IOException) {
        Single.just(mapper(it))
    } else {
        Single.error(it)
    }
}
