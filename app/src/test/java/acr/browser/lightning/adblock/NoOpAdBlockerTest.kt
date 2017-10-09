package acr.browser.lightning.adblock

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Unit tests for [NoOpAdBlocker].
 */
class NoOpAdBlockerTest {

    @Test
    fun `isAd no-ops`() {
        val noOpAdBlocker = NoOpAdBlocker()

        assertThat(noOpAdBlocker.isAd("https://ads.google.com")).isFalse()
        assertThat(noOpAdBlocker.isAd(null)).isFalse()
    }
}