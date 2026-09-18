package acr.browser.lightning.pool

/**
 * A pool of objects that can be acquired that notify the consumer when they should be released.
 */
interface ObjectPool<T> {

    /**
     * Acquire a new object.
     *
     * @param highPriority True if the acquired object should displace the current head of the queue
     * or if it should be added behind the head.
     */
    suspend fun acquire(highPriority: Boolean): AcquiredObject<T>

    /**
     * Release the [acquiredObject].
     */
    suspend fun release(acquiredObject: AcquiredObject<T>)

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
}
