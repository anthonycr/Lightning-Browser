package acr.browser.lightning.favicon

import acr.browser.lightning.BuildConfig
import acr.browser.lightning.SDK_VERSION
import acr.browser.lightning.TestApplication
import android.net.Uri
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
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

    @Test(expected = RuntimeException::class)
    fun `validateUri fails with null scheme`() {
        val uri = mock<Uri> {
            on { scheme } doReturn null as String?
            on { host } doReturn "www.google.com"
        }

        uri.validateUri()
    }

    @Test(expected = RuntimeException::class)
    fun `validateUri fails with empty scheme`() {
        val uri = mock<Uri> {
            on { scheme } doReturn ""
            on { host } doReturn "www.google.com"
        }

        uri.validateUri()
    }

    @Test(expected = RuntimeException::class)
    fun `validateUri fails with blank scheme`() {
        val uri = mock<Uri> {
            on { scheme } doReturn " "
            on { host } doReturn "www.google.com"
        }

        uri.validateUri()
    }

    @Test(expected = RuntimeException::class)
    fun `validateUri fails with null host`() {
        val uri = mock<Uri> {
            on { scheme } doReturn "https://"
            on { host } doReturn null as String?
        }

        uri.validateUri()
    }

    @Test(expected = RuntimeException::class)
    fun `validateUri fails with empty host`() {
        val uri = mock<Uri> {
            on { scheme } doReturn "https://"
            on { host } doReturn ""
        }

        uri.validateUri()
    }

    @Test(expected = RuntimeException::class)
    fun `validateUri fails with blank host`() {
        val uri = mock<Uri> {
            on { scheme } doReturn "https://"
            on { host } doReturn " "
        }

        uri.validateUri()
    }

    @Test
    fun `validateUri succeeds with non blank scheme and host`() {
        val uri = mock<Uri> {
            on { scheme } doReturn "https://"
            on { host } doReturn "www.google.com"
        }

        uri.validateUri()
    }

    @Test
    fun `isValid returns false with blank scheme`() {
        val uri = mock<Uri> {
            on { scheme } doReturn ""
            on { host } doReturn "www.google.com"
        }

        assertThat(uri.isValid()).isFalse()
    }

    @Test
    fun `isValidReturns false with blank host`() {
        val uri = mock<Uri> {
            on { scheme } doReturn "https://"
            on { host } doReturn ""
        }

        assertThat(uri.isValid()).isFalse()
    }

    @Test
    fun `isValid returns true with non blank scheme and host`() {
        val uri = mock<Uri> {
            on { scheme } doReturn "https://"
            on { host } doReturn "www.google.com"
        }

        assertThat(uri.isValid()).isTrue()
    }

    @Test
    fun `safeUri returns null for empty url`() = assertThat("".toValidUri()).isNull()

    @Test
    fun `safeUri returns null for url without scheme`() = assertThat("test.com".toValidUri()).isNull()

    @Test
    fun `safeUri returns null for url without host`() = assertThat("http://".toValidUri()).isNull()

    @Test
    fun `safeUri returns valid Uri for full url`() {
        val uri = Uri.parse("http://test.com")
        assertThat("http://test.com".toValidUri()).isEqualTo(uri)
    }
}