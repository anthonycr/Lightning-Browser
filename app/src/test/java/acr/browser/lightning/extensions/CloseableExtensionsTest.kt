package acr.browser.lightning.extensions

import acr.browser.lightning.SDK_VERSION
import acr.browser.lightning.TestApplication
import com.anthonycr.mockingbird.core.fake
import com.anthonycr.mockingbird.core.verify
import org.junit.Assert.assertThrows
import org.junit.Test
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

    @Test
    fun `safeUse swallows exception`() {
        // Exception swallowed
        closeable.safeUse {
            throw Exception("test exception")
        }

        verify(closeable) {
            closeable.close()
        }

        assertThrows("test exception", Exception::class.java) {
            closeable.use {
                throw Exception("test exception")
            }
        }
    }
}
