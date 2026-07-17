package acr.browser.lightning.adblock

import acr.browser.lightning.SDK_VERSION
import acr.browser.lightning.TestApplication
import androidx.core.net.toUri
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [NoOpAdBlocker].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [SDK_VERSION])
class NoOpAdBlockerTest {

    @Test
    fun `isAd no-ops`() {
        val noOpAdBlocker = NoOpAdBlocker()

        assertThat(noOpAdBlocker.isAd("https://ads.google.com".toUri())).isFalse()
    }
}
