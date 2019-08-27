package acr.browser.lightning.adblock.util

import acr.browser.lightning.adblock.util.hash.MurmurHashHostAdapter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Tests for [DefaultBloomFilter].
 */
class DefaultBloomFilterTest {

    @Test
    fun `empty bloom filter returns false for all values`() {
        val bloomFilter: BloomFilter<String> = DefaultBloomFilter(
            MAX_TEST_VALUE,
            0.01,
            MurmurHashHostAdapter()
        )
        for (i in 0..MAX_TEST_VALUE) {
            val entry = i.toString()
            assertThat(bloomFilter.mightContain(entry)).isFalse()
        }
    }

    @Test
    fun `strings may be present after adding to filter`() {
        val bloomFilter: BloomFilter<String> = DefaultBloomFilter(
            MAX_TEST_VALUE,
            0.01,
            MurmurHashHostAdapter()
        )
        for (i in 0..MAX_TEST_VALUE) {
            bloomFilter.put(i.toString())
            assertThat(bloomFilter.mightContain(i.toString())).isTrue()
        }
    }

    @Test
    fun `strings may be present after adding collection to filter`() {
        val bloomFilter: BloomFilter<String> = DefaultBloomFilter(
            MAX_TEST_VALUE,
            0.01,
            MurmurHashHostAdapter()
        )

        val collection = (0..MAX_TEST_VALUE).map(Int::toString)
        bloomFilter.putAll(collection)
        collection.forEach {
            assertThat(bloomFilter.mightContain(it)).isTrue()
        }
    }

    @Test
    fun `strings that are not in the filter return false positives near the accepted rate`() {
        val bloomFilter: BloomFilter<String> = DefaultBloomFilter(
            MAX_TEST_VALUE,
            0.01,
            MurmurHashHostAdapter()
        )
        for (i in 0 until MAX_TEST_VALUE) {
            val entry = i.toString()
            bloomFilter.put(entry)
        }

        var falsePositive = 0
        for (i in MAX_TEST_VALUE until 2 * MAX_TEST_VALUE) {
            val entry = i.toString()
            if (bloomFilter.mightContain(entry)) {
                falsePositive++
            }
        }

        assertThat(falsePositive.toDouble() / MAX_TEST_VALUE).isLessThan(0.03)
    }

    companion object {
        private const val MAX_TEST_VALUE = Int.MAX_VALUE / 100
    }
}
