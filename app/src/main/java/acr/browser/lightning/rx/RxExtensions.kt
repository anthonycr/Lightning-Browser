package acr.browser.lightning.rx

import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
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
inline fun <T, R, Selector_T, Selector_R, S> Flowable<T>.join(
    other: Flowable<R>,
    crossinline selectorLeft: (T) -> Publisher<Selector_T>,
    crossinline selectorRight: (R) -> Publisher<Selector_R>,
    crossinline join: (T, R) -> S
): Flowable<S> = join<R, Selector_T, Selector_R, S>(
    other,
    io.reactivex.functions.Function { selectorLeft(it) },
    io.reactivex.functions.Function { selectorRight(it) },
    BiFunction { t1, t2 -> join(t1, t2) }
)
