package acr.browser.lightning.adblock.util

/**
 * A bloom filter that delegates to a mutable [BloomFilter]. It does not support additions.
 */
class DelegatingBloomFilter<T>(var delegate: BloomFilter<T>? = null) : BloomFilter<T> {

    override fun put(item: T) = throw IllegalStateException("DelegatingBloomFilter does not support put")

    override fun putAll(collection: Collection<T>) = throw IllegalStateException("DelegatingBloomFilter does not support putAll")

    override fun mightContain(item: T): Boolean = delegate?.mightContain(item) ?: false

}
