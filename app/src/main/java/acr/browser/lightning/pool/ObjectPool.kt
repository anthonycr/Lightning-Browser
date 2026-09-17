package acr.browser.lightning.pool

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe object "pool" for objects that cannot be reused that limits the total number of
 * allocated objects releasing old ones and creating new ones as needed.
 *
 * @param factory Produce a new instance of the object.
 */
class ObjectPool<T>(
    private val factory: suspend () -> T
) {

    private val pool = ArrayDeque<MutableAcquiredObject<T>>(POOL_SIZE)
    private val poolMutex = Mutex()

    /**
     * Acquire a new object.
     *
     * @param highPriority True if the acquired object should displace the current head of the queue
     * or if it should be added behind the head.
     */
    suspend fun acquire(highPriority: Boolean): AcquiredObject<T> = poolMutex.withLock {
        if (pool.size == POOL_SIZE) {
            pool.removeLast().release()
        }

        MutableAcquiredObject(
            actual = factory(),
        ).also { created ->
            if (!highPriority && pool.isNotEmpty()) {
                pool.add(1, created)
            } else {
                pool.addFirst(created)
            }
        }
    }

    /**
     * Release the [acquiredObject].
     */
    suspend fun release(acquiredObject: AcquiredObject<T>) = poolMutex.withLock {
        pool.remove(acquiredObject)
    }

    companion object {
        private const val POOL_SIZE = 5
    }

    interface AcquiredObject<T> {
        /**
         * The underlying value that is being held.
         */
        val actual: T

        /**
         * Suspends until the acquired object should be released and then invokes the [onRelease]
         * block.
         */
        suspend fun awaitRelease(onRelease: suspend () -> Unit)
    }

    private class MutableAcquiredObject<T>(
        override val actual: T,
    ) : AcquiredObject<T> {

        private val shouldRelease: Mutex = Mutex(locked = true)
        private val releaseFinished = Mutex(locked = true)

        suspend fun release() {
            shouldRelease.unlock()
            releaseFinished.lock()
        }

        override suspend fun awaitRelease(onRelease: suspend () -> Unit) {
            shouldRelease.lock()
            onRelease()
            releaseFinished.unlock()
        }
    }
}
