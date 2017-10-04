package acr.browser.lightning.utils

import org.junit.Test

/**
 * Unit tests for [Preconditions].
 */
class PreconditionsTest {

    @Test(expected = RuntimeException::class)
    fun `checkNonNull throws exception for null param`() = Preconditions.checkNonNull(null)

    @Test
    fun `checkNonNull succeeds for non null param`() = Preconditions.checkNonNull(Any())
}