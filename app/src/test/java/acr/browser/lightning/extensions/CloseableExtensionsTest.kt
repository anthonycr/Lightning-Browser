package acr.browser.lightning.extensions

import acr.browser.lightning.SDK_VERSION
import acr.browser.lightning.TestApplication
import com.anthonycr.mockingbird.core.fake
import com.anthonycr.mockingbird.core.verify
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.Closeable


/**
 * Unit tests for [Closeable] extensions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [SDK_VERSION])
class CloseableExtensionsTest {

    private val closeable = fake<Closeable>()

    @Rule
    @JvmField
    val exception: ExpectedException = ExpectedException.none()

    @Test
    fun `safeUse swallows exception`() {
        // Exception swallowed
        closeable.safeUse {
            throw Exception("test exception")
        }

        verify(closeable) {
            closeable.close()
        }

        exception.expect(Exception::class.java)
        exception.expectMessage("test exception")

        closeable.use {
            throw Exception("test exception")
        }
    }
}
