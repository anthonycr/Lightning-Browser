package acr.browser.lightning.utils

import org.junit.Assert
import org.junit.Test

/**
 * Tests for [StringBuilderUtils].
 */
class StringBuilderUtilsTest {

    @Test
    fun replace_SingleCharacter() {
        val stringBuilder = StringBuilder("abbbaacccda")
        StringBuilderUtils.replace(stringBuilder, "a", "")
        Assert.assertEquals(stringBuilder.toString(), "bbbaacccda")
    }

    @Test
    fun replace_MultipleCharacters() {
        val stringBuilder = StringBuilder("___ab___cd___")
        StringBuilderUtils.replace(stringBuilder, "___", "---")
        Assert.assertEquals(stringBuilder.toString(), "---ab___cd___")
    }

    @Test
    fun trim_LeadingAndTrailingSpaces() {
        val stringBuilder = StringBuilder(" t e s t        ")
        StringBuilderUtils.trim(stringBuilder)
        Assert.assertEquals(stringBuilder.toString(), "t e s t")
    }

    @Test
    fun trim_OnlyContainsSpaces() {
        val stringBuilder = StringBuilder("           ")
        Assert.assertFalse(stringBuilder.toString().isEmpty())
        StringBuilderUtils.trim(stringBuilder)
        Assert.assertTrue(stringBuilder.toString().isEmpty())
    }

    @Test
    fun isEmpty_HasNoCharacters() {
        val stringBuilder = StringBuilder("")
        Assert.assertTrue(StringBuilderUtils.isEmpty(stringBuilder))
    }

    @Test
    fun isEmpty_HasCharacters() {
        // Case with normal letters
        val stringBuilder = StringBuilder("abcdefg")
        Assert.assertFalse(StringBuilderUtils.isEmpty(stringBuilder))

        // Case with empty spaces
        val stringBuilder1 = StringBuilder("       ")
        Assert.assertFalse(StringBuilderUtils.isEmpty(stringBuilder1))
    }

    @Test
    fun startsWith_SingleCharacter() {
        val stringBuilder = StringBuilder("1234567890")
        Assert.assertTrue(StringBuilderUtils.startsWith(stringBuilder, "1"))
        Assert.assertFalse(StringBuilderUtils.startsWith(stringBuilder, "2"))
    }

    @Test
    fun startsWith_MultipleCharacters() {
        val stringBuilder = StringBuilder("1234567890")
        Assert.assertTrue(StringBuilderUtils.startsWith(stringBuilder, "12345"))
        Assert.assertFalse(StringBuilderUtils.startsWith(stringBuilder, "23456"))
    }

    @Test
    fun contains_SingleCharacter() {
        val stringBuilder = StringBuilder("abcdefg123456")

        // This character is not in the string
        Assert.assertFalse(StringBuilderUtils.contains(stringBuilder, "z"))

        // All these characters are in the string
        for (n in 0..stringBuilder.length - 1) {
            Assert.assertTrue(StringBuilderUtils.contains(stringBuilder, stringBuilder[n].toString()))
        }
    }

    @Test
    fun contains_MultipleCharacters() {
        val stringBuilder = StringBuilder("abcdefg123456")

        // Should return false since characters are in reverse order
        Assert.assertFalse(StringBuilderUtils.contains(stringBuilder, "cba"))

        // Should return false since characters are not in string
        Assert.assertFalse(StringBuilderUtils.contains(stringBuilder, "zyx"))
    }

    @Test
    fun equals_SameCharacters() {
        val stringBuilder = StringBuilder("abcdefg")

        Assert.assertTrue(StringBuilderUtils.equals(stringBuilder, "abcdefg"))
    }

    @Test
    fun equals_DifferentCharacters() {
        val stringBuilder = StringBuilder("abcdefg")

        Assert.assertFalse(StringBuilderUtils.equals(stringBuilder, "abcdefg1"))
    }

    @Test
    fun substring() {
        val stringBuilder = StringBuilder("abcdefg")

        val string = "abcdefg"

        Assert.assertEquals(StringBuilderUtils.substring(stringBuilder, 1, 5).toString(), string.substring(1, 5))
    }

}