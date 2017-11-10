package acr.browser.lightning.utils

import acr.browser.lightning.BuildConfig
import acr.browser.lightning.SDK_VERSION
import acr.browser.lightning.TestApplication
import android.net.Uri
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [Uri] utils.
 *
 * Created by anthonycr on 10/27/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, application = TestApplication::class, sdk = intArrayOf(SDK_VERSION))
class UriUtilsTest {

    @Test
    fun `safeUri returns null for empty url`() = assertThat(safeUri("")).isNull()

    @Test
    fun `safeUri returns null for url without scheme`() = assertThat(safeUri("test.com")).isNull()

    @Test
    fun `safeUri returns null for url without host`() = assertThat(safeUri("http://")).isNull()

    @Test
    fun `safeUri returns valid Uri for full url`() {
        val uri = Uri.parse("http://test.com")
        assertThat(safeUri("http://test.com")).isEqualTo(uri)
    }

    @Test
    fun `domainForUrl returns null for null url`() = assertThat(domainForUrl(null)).isNull()

    @Test
    fun `domainForUrl returns null for url without domain`() =
            assertThat(domainForUrl("http://")).isNull()

    @Test
    fun `domainForUrl returns domain for valid url`() {
        assertThat(domainForUrl("http://test.com")).isEqualTo("test.com")
    }
}