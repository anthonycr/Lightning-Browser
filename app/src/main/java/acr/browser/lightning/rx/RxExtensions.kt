package acr.browser.lightning.rx

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import org.reactivestreams.Publisher

/**
 * Kotlin friendly version of [Observable.join].
 *
 * @param other The other observable that will be joined to the current one.
 * @param selectorLeft Provides an observable used to signal sampling windows for this observable.
 * @param selectorRight Provides an observable used to signal sampling windows for the [other]
 * observable.
 * @param join The function that joins the output of this and the [other] observable into the output
 * type.
 * @see Observable.join
 */
inline fun <T : Any, R : Any, Selector_T : Any, Selector_R : Any, S : Any> Flowable<T>.join(
    other: Flowable<R>,
    crossinline selectorLeft: (T) -> Publisher<Selector_T>,
    crossinline selectorRight: (R) -> Publisher<Selector_R>,
    crossinline join: (T, R) -> S
): Flowable<S> = join(
    other,
    { selectorLeft(it) },
    { selectorRight(it) },
    { t1, t2 -> join(t1, t2) }
)
