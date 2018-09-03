package acr.browser.lightning.favicon

import acr.browser.lightning.BuildConfig
import acr.browser.lightning.SDK_VERSION
import acr.browser.lightning.TestApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for UriExtensions.kt
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, application = TestApplication::class, sdk = [SDK_VERSION])
class UriExtensionsKtTest {

    @Test
    fun `safeUri returns null for empty url`() = assertThat("".toValidUri()).isNull()

    @Test
    fun `safeUri returns null for url without scheme`() = assertThat("test.com".toValidUri()).isNull()

    @Test
    fun `safeUri returns null for url without host`() = assertThat("http://".toValidUri()).isNull()

    @Test
    fun `safeUri returns valid Uri for full url`() {
        assertThat("http://test.com".toValidUri()).isEqualTo(ValidUri("http", "test.com"))
    }
}
