package acr.browser.lightning.adblock.util

import acr.browser.lightning.adblock.util.hash.HashingAlgorithm
import acr.browser.lightning.adblock.util.integer.lowerHalf
import acr.browser.lightning.adblock.util.integer.upperHalf
import java.io.Serializable
import java.util.BitSet
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * A [BloomFilter] implementation is based off the algorithm described
 * in [https://en.wikipedia.org/wiki/Bloom_filter] and borrows from Guava's bloom filter
 * implementation.
 *
 * The number of bits and number of hash functions are calculated as follows:
 * ```
 * n = number of elements
 * p = false positive rate
 * m = number of bits
 * k = number of hash functions
 *
 * m = -n ln p / (ln 2) ^ 2
 *
 * k = m ln 2 / n
 * ```
 * This bloom filter is backed by a bit array, and at very large input values and very low false
 * positive rates, the bloom filter will not perform optimally, since the maximum number of bits is
 * limited to [Int.MAX_VALUE].
 *
 * @param numberOfElements The number of elements that will be added to this filter.
 * @param falsePositiveRate The acceptable rate of false positives.
 * @param hashingAlgorithm The algorithm that should be used to hash the values.
 */
class DefaultBloomFilter<T>(
    numberOfElements: Int,
    falsePositiveRate: Double,
    private val hashingAlgorithm: HashingAlgorithm<T>
) : BloomFilter<T>, Serializable {

    private val numberOfBits: Int =
        (-numberOfElements * ln(falsePositiveRate) / (ln(2.0) * ln(2.0)))
            .roundToInt()
            .coerceAtLeast(1)

    private val numberOfHashes: Int = (numberOfBits * ln(2.0) / numberOfElements)
        .roundToInt()
        .coerceAtLeast(1)

    private val bitSet: BitSet = BitSet(numberOfBits)

    override fun put(item: T) {
        val hash = hashingAlgorithm.hash(item)

        val lowerHalf = hash.lowerHalf()
        val upperHalf = hash.upperHalf()

        val bitSize = bitSet.size()
        var combinedHash = lowerHalf
        for (i in 0 until numberOfHashes) {
            bitSet.set((combinedHash and Int.MAX_VALUE) % bitSize)
            combinedHash += upperHalf
        }
    }

    override fun putAll(collection: Collection<T>) {
        collection.forEach(::put)
    }

    override fun mightContain(item: T): Boolean {
        val hash = hashingAlgorithm.hash(item)

        val lowerHalf = hash.lowerHalf()
        val upperHalf = hash.upperHalf()

        val bitSize = bitSet.size()
        var combinedHash = lowerHalf
        for (i in 0 until numberOfHashes) {
            if (!bitSet.get((combinedHash and Int.MAX_VALUE) % bitSize)) {
                return false
            }
            combinedHash += upperHalf
        }
        return true
    }


}
