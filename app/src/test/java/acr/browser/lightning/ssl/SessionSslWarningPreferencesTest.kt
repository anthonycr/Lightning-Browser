package acr.browser.lightning.ssl

import acr.browser.lightning.SDK_VERSION
import acr.browser.lightning.TestApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [SessionSslWarningPreferences]
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [SDK_VERSION])
class SessionSslWarningPreferencesTest {

    @Test
    fun `recallSslWarningBehavior requires equivalent rememberSslWarningBehavior`() {
        val sessionSslWarningPreferences = SessionSslWarningPreferences()

        assertThat(sessionSslWarningPreferences.recallBehaviorForDomain("https://test.com")).isNull()

        sessionSslWarningPreferences.rememberBehaviorForDomain("https://test.com/12345", SslWarningPreferences.Behavior.PROCEED)

        assertThat(sessionSslWarningPreferences.recallBehaviorForDomain("https://test.com")).isEqualTo(SslWarningPreferences.Behavior.PROCEED)
    }

    @Test
    fun `recallSslWarningBehavior returns null for invalid url`() {
        val sessionSslWarningPreferences = SessionSslWarningPreferences()

        sessionSslWarningPreferences.rememberBehaviorForDomain("https://test.com/12345", SslWarningPreferences.Behavior.PROCEED)

        assertThat(sessionSslWarningPreferences.recallBehaviorForDomain("test.com")).isNull()
    }

    @Test
    fun `rememberSslWarningBehavior no-ops for invalid url`() {
        val sessionSslWarningPreferences = SessionSslWarningPreferences()

        sessionSslWarningPreferences.rememberBehaviorForDomain("test.com", SslWarningPreferences.Behavior.PROCEED)

        assertThat(sessionSslWarningPreferences.recallBehaviorForDomain("https://test.com")).isNull()

    }
}
