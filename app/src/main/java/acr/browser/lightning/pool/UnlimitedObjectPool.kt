package acr.browser.lightning.pool

/**
 * A no-op implementation of [ObjectPool] that creates a new instance every time.
 */
class UnlimitedObjectPool<T>(
    private val factory: suspend () -> T
) : ObjectPool<T> {

    override suspend fun acquire(
        highPriority: Boolean
    ): ObjectPool.AcquiredObject<T> = UnreleasableAcquiredObject(factory())

    override suspend fun release(acquiredObject: ObjectPool.AcquiredObject<T>) = Unit

    private class UnreleasableAcquiredObject<T>(
        override val actual: T
    ) : ObjectPool.AcquiredObject<T> {
        override suspend fun awaitRelease(onRelease: suspend () -> Unit) = Unit
    }
}
