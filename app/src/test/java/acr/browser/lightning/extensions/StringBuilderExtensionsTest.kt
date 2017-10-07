package acr.browser.lightning.extensions

import org.junit.Assert
import org.junit.Test

/**
 * Tests for extensions of [StringBuilder].
 */
class StringBuilderExtensionsTest {

    @Test
    fun `inlineReplace with single character`() {
        val stringBuilder = StringBuilder("abbbaacccda")
        stringBuilder.inlineReplace("a", "")
        Assert.assertEquals(stringBuilder.toString(), "bbbaacccda")
    }

    @Test
    fun `inlineReplace with multiple characters`() {
        val stringBuilder = StringBuilder("___ab___cd___")
        stringBuilder.inlineReplace("___", "---")
        Assert.assertEquals(stringBuilder.toString(), "---ab___cd___")
    }

    @Test
    fun `inlineTrim with leading and trailing whitespace`() {
        val stringBuilder = StringBuilder(" t e s t        ")
        stringBuilder.inlineTrim()
        Assert.assertEquals(stringBuilder.toString(), "t e s t")
    }

    @Test
    fun `inlineTrim with only whitespace`() {
        val stringBuilder = StringBuilder("           ")
        Assert.assertFalse(stringBuilder.toString().isEmpty())
        stringBuilder.inlineTrim()
        Assert.assertTrue(stringBuilder.toString().isEmpty())
    }

    @Test
    fun `stringEquals with same characters returns true`() {
        val stringBuilder = StringBuilder("abcdefg")

        Assert.assertTrue(stringBuilder.stringEquals("abcdefg"))
    }

    @Test
    fun `stringEquals with different characters returns false`() {
        val stringBuilder = StringBuilder("abcdefg")

        Assert.assertFalse(stringBuilder.stringEquals("abcdefg1"))
    }

    @Test
    fun `substringToBuilder correctly substrings`() {
        val stringBuilder = StringBuilder("abcdefg")

        val string = "abcdefg"

        Assert.assertEquals(stringBuilder.substringToBuilder(1, 5).toString(), string.substring(1, 5))
    }

}