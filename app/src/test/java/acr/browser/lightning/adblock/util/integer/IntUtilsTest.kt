package acr.browser.lightning.adblock.util.integer

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Tests for [lowerHalf] and [upperHalf].
 */
class IntUtilsTest {

    @Test
    fun `lowerHalf returns lower 16 bits`() {
        assertThat(Integer.toBinaryString(Int.MAX_VALUE.lowerHalf())).isEqualTo("1111111111111111")
    }

    @Test
    fun `upperHalf returns upper 15 bits`() {
        assertThat(Integer.toBinaryString(Int.MAX_VALUE.upperHalf())).isEqualTo("1111111111111110000000000000000")
    }
}
