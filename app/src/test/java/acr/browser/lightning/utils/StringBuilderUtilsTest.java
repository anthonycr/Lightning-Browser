package acr.browser.lightning.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link StringBuilderUtils}.
 */
public class StringBuilderUtilsTest {

    @Test
    public void replace_SingleCharacter() {
        StringBuilder stringBuilder = new StringBuilder("abbbaacccda");
        StringBuilderUtils.replace(stringBuilder, "a", "");
        Assert.assertEquals(stringBuilder.toString(), "bbbaacccda");
    }

    @Test
    public void replace_MultipleCharacters() {
        StringBuilder stringBuilder = new StringBuilder("___ab___cd___");
        StringBuilderUtils.replace(stringBuilder, "___", "---");
        Assert.assertEquals(stringBuilder.toString(), "---ab___cd___");
    }

    @Test
    public void trim_LeadingAndTrailingSpaces() {
        StringBuilder stringBuilder = new StringBuilder(" t e s t        ");
        StringBuilderUtils.trim(stringBuilder);
        Assert.assertEquals(stringBuilder.toString(), "t e s t");
    }

    @Test
    public void trim_OnlyContainsSpaces() {
        StringBuilder stringBuilder = new StringBuilder("           ");
        Assert.assertFalse(stringBuilder.toString().isEmpty());
        StringBuilderUtils.trim(stringBuilder);
        Assert.assertTrue(stringBuilder.toString().isEmpty());
    }

    @Test
    public void isEmpty_HasNoCharacters() {
        StringBuilder stringBuilder = new StringBuilder("");
        Assert.assertTrue(StringBuilderUtils.isEmpty(stringBuilder));
    }

    @Test
    public void isEmpty_HasCharacters() {
        // Case with normal letters
        StringBuilder stringBuilder = new StringBuilder("abcdefg");
        Assert.assertFalse(StringBuilderUtils.isEmpty(stringBuilder));

        // Case with empty spaces
        StringBuilder stringBuilder1 = new StringBuilder("       ");
        Assert.assertFalse(StringBuilderUtils.isEmpty(stringBuilder1));
    }

    @Test
    public void startsWith_SingleCharacter() {
        StringBuilder stringBuilder = new StringBuilder("1234567890");
        Assert.assertTrue(StringBuilderUtils.startsWith(stringBuilder, "1"));
        Assert.assertFalse(StringBuilderUtils.startsWith(stringBuilder, "2"));
    }

    @Test
    public void startsWith_MultipleCharacters() {
        StringBuilder stringBuilder = new StringBuilder("1234567890");
        Assert.assertTrue(StringBuilderUtils.startsWith(stringBuilder, "12345"));
        Assert.assertFalse(StringBuilderUtils.startsWith(stringBuilder, "23456"));
    }

    @Test
    public void contains_SingleCharacter() {
        StringBuilder stringBuilder = new StringBuilder("abcdefg123456");

        // This character is not in the string
        Assert.assertFalse(StringBuilderUtils.contains(stringBuilder, "z"));

        // All these characters are in the string
        for (int n = 0; n < stringBuilder.length(); n++) {
            Assert.assertTrue(StringBuilderUtils.contains(stringBuilder, String.valueOf(stringBuilder.charAt(n))));
        }
    }

    @Test
    public void contains_MultipleCharacters() {
        StringBuilder stringBuilder = new StringBuilder("abcdefg123456");

        // Should return false since characters are in reverse order
        Assert.assertFalse(StringBuilderUtils.contains(stringBuilder, "cba"));

        // Should return false since characters are not in string
        Assert.assertFalse(StringBuilderUtils.contains(stringBuilder, "zyx"));
    }

    @Test
    public void equals_SameCharacters() {
        StringBuilder stringBuilder = new StringBuilder("abcdefg");

        Assert.assertTrue(StringBuilderUtils.equals(stringBuilder, "abcdefg"));
    }

    @Test
    public void equals_DifferentCharacters() {
        StringBuilder stringBuilder = new StringBuilder("abcdefg");

        Assert.assertFalse(StringBuilderUtils.equals(stringBuilder, "abcdefg1"));
    }

    @Test
    public void substring() {
        StringBuilder stringBuilder = new StringBuilder("abcdefg");

        String string = "abcdefg";

        Assert.assertEquals(StringBuilderUtils.substring(stringBuilder, 1, 5).toString(), string.substring(1, 5));
    }

}