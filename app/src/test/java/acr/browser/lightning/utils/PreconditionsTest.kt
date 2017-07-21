package acr.browser.lightning.utils

import org.junit.Test

/**
 * Unit tests for [Preconditions].
 */
class PreconditionsTest {

    @Test(expected = RuntimeException::class)
    fun checkNonNull_Null_ThrowsException() {
        Preconditions.checkNonNull(null)
    }

    @Test
    fun checkNonNull_NonNull_Succeeds() {
        Preconditions.checkNonNull(Any())
    }
}