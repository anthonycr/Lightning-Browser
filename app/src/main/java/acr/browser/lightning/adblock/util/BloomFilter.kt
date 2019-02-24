package acr.browser.lightning.adblock.util

/**
 * A bloom filter.
 *
 * See [https://en.wikipedia.org/wiki/Bloom_filter] for data structure details.
 */
interface BloomFilter<T> {

    /**
     * Adds the [item] to the filter. Adding the [item] guarantees that a subsequent call to
     * [mightContain] with the same arguments will return `true`.
     */
    fun put(item: T)

    /**
     * Returns `true` if the [item] might have been added to the filter, `false` otherwise. Due to
     * the probabilistic nature of this data structure, returning `true` cannot guarantee that the
     * [item] was ever added, but returning `false` guarantees that it was not.
     */
    fun mightContain(item: T): Boolean
}
