package acr.browser.lightning.favicon

import acr.browser.lightning.BuildConfig
import acr.browser.lightning.SDK_VERSION
import acr.browser.lightning.TestApplication
import android.net.Uri
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for FaviconUtils.kt
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, application = TestApplication::class, sdk = intArrayOf(SDK_VERSION))
class FaviconUtilsTest {

    @Test(expected = RuntimeException::class)
    fun `requireSafeUri fails with null scheme`() {
        val uri = mock<Uri>()
        whenever(uri.scheme).then { null }
        whenever(uri.host).then { "www.google.com" }

        requireUriSafe(uri)
    }

    @Test(expected = RuntimeException::class)
    fun `requireSafeUri fails with empty scheme`() {
        val uri = mock<Uri>()
        whenever(uri.scheme).then { "" }
        whenever(uri.host).then { "www.google.com" }

        requireUriSafe(uri)
    }

    @Test(expected = RuntimeException::class)
    fun `requireSafeUri fails with blank scheme`() {
        val uri = mock<Uri>()
        whenever(uri.scheme).then { " " }
        whenever(uri.host).then { "www.google.com" }

        requireUriSafe(uri)
    }

    @Test(expected = RuntimeException::class)
    fun `requireSafeUri fails with null host`() {
        val uri = mock<Uri>()
        whenever(uri.scheme).then { "https://" }
        whenever(uri.host).then { null }

        requireUriSafe(uri)
    }

    @Test(expected = RuntimeException::class)
    fun `requireSafeUri fails with empty host`() {
        val uri = mock<Uri>()
        whenever(uri.scheme).then { "https://" }
        whenever(uri.host).then { "" }

        requireUriSafe(uri)
    }

    @Test(expected = RuntimeException::class)
    fun `requireSafeUri fails with blank host`() {
        val uri = mock<Uri>()
        whenever(uri.scheme).then { "https://" }
        whenever(uri.host).then { " " }

        requireUriSafe(uri)
    }

    @Test
    fun `requireSafeUri succeeds with non blank scheme and host`() {
        val uri = mock<Uri>()
        whenever(uri.scheme).then { "https://" }
        whenever(uri.host).then { "www.google.com" }

        requireUriSafe(uri)
    }
}