package acr.browser.lightning.concurrency

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Type-safe analogue of [kotlinx.coroutines.flow.combine].
 */
@Suppress("UNCHECKED_CAST")
fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> combineMultiple(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    flow7: Flow<T7>,
    flow8: Flow<T8>,
    flow9: Flow<T9>,
    transform: suspend (T1, T2, T3, T4, T5, T6, T7, T8, T9) -> R
): Flow<R> = combine(flow, flow2, flow3, flow4, flow5, flow6, flow7, flow8, flow9) {
    transform(
        it[0] as T1,
        it[1] as T2,
        it[2] as T3,
        it[3] as T4,
        it[4] as T5,
        it[5] as T6,
        it[6] as T7,
        it[7] as T8,
        it[8] as T9
    )
}

/**
 * Type-safe analogue of [kotlinx.coroutines.flow.combine].
 */
@Suppress("UNCHECKED_CAST")
fun <T1, T2, T3, R> combineMultiple(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    transform: suspend (T1, T2, T3) -> R
): Flow<R> = combine(flow, flow2, flow3) {
    transform(
        it[0] as T1,
        it[1] as T2,
        it[2] as T3
    )
}
