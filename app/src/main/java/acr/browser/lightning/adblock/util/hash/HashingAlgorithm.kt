package acr.browser.lightning.adblock.util.hash

/**
 * A hashing algorithm.
 *
 * @param T The type that will be hashed.
 */
interface HashingAlgorithm<T> {

    /**
     * Hashes the [item] to its [Int] representation.
     */
    fun hash(item: T): Int

}
