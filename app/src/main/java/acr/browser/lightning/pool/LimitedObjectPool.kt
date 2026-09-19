package acr.browser.lightning.pool

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe object "pool" for objects that cannot be reused that limits the total number of
 * allocated objects releasing old ones and creating new ones as needed.
 *
 * @param factory Produce a new instance of the object.
 * @param poolSize The size maximum of the object pool.
 */
class LimitedObjectPool<T>(
    private val factory: suspend () -> T,
    private val poolSize: Int,
) : ObjectPool<T> {

    private val pool = ArrayDeque<MutableAcquiredObject<T>>(poolSize)
    private val poolMutex = Mutex()

    override suspend fun acquire(
        highPriority: Boolean
    ): ObjectPool.AcquiredObject<T> = poolMutex.withLock {
        if (pool.size == poolSize) {
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

    override suspend fun release(
        acquiredObject: ObjectPool.AcquiredObject<T>
    ): Unit = poolMutex.withLock {
        pool.remove(acquiredObject)
    }

    private class MutableAcquiredObject<T>(
        override val actual: T,
    ) : ObjectPool.AcquiredObject<T> {

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
