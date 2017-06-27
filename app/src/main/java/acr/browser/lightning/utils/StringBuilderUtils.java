package acr.browser.lightning.utils;

import android.support.annotation.NonNull;

/**
 * A collection of utils methods for
 * {@link StringBuilder} that provides
 * API equality with the {@link String}
 * API.
 */
public class StringBuilderUtils {

    private static final String SPACE = " ";
    private static final String EMPTY = "";

    /**
     * Replace the first string found in a
     * string builder with another string.
     *
     * @param stringBuilder the string builder.
     * @param toReplace     the string to replace.
     * @param replacement   the replacement string.
     */
    public static void replace(@NonNull StringBuilder stringBuilder,
                        @NonNull String toReplace,
                        @NonNull String replacement) {
        int index = stringBuilder.indexOf(toReplace);
        if (index >= 0) {
            stringBuilder.replace(index, index + toReplace.length(), replacement);
        }
    }

    /**
     * Trims a string builder of
     * any spaces at the beginning
     * and end.
     *
     * @param stringBuilder the string builder.
     */
    public static void trim(@NonNull StringBuilder stringBuilder) {
        while (stringBuilder.indexOf(SPACE) == 0) {
            stringBuilder.replace(0, 1, EMPTY);
        }

        while (stringBuilder.lastIndexOf(SPACE) == (stringBuilder.length() - 1) && stringBuilder.length() > 0) {
            stringBuilder.replace(stringBuilder.length() - 1, stringBuilder.length(), EMPTY);
        }
    }

    /**
     * Determines if the string builder is empty.
     *
     * @param stringBuilder the string builder.
     * @return true if the string builder is empty,
     * false otherwise.
     */
    public static boolean isEmpty(@NonNull StringBuilder stringBuilder) {
        return stringBuilder.length() == 0;
    }

    /**
     * Determines if a string builder starts with
     * a specific string.
     *
     * @param stringBuilder the string builder.
     * @param start         the starting string.
     * @return true if the string builder starts
     * with the string, false otherwise.
     */
    public static boolean startsWith(@NonNull StringBuilder stringBuilder, @NonNull String start) {
        return stringBuilder.indexOf(start) == 0;
    }

    /**
     * Determines if a string builder contains a string.
     *
     * @param stringBuilder the string builder.
     * @param contains      the string that it might contain.
     * @return true if the string builder contains the
     * string, false otherwise.
     */
    public static boolean contains(@NonNull StringBuilder stringBuilder, @NonNull String contains) {
        return stringBuilder.indexOf(contains) >= 0;
    }

    /**
     * Determines equality between a string
     * builder and a string.
     *
     * @param stringBuilder the string builder.
     * @param equal         the string.
     * @return true if the string represented by
     * the string builder is equal to the string.
     */
    public static boolean equals(@NonNull StringBuilder stringBuilder, @NonNull String equal) {

        int builderLength = stringBuilder.length();
        if (builderLength != equal.length()) {
            return false;
        }

        for (int n = 0; n < builderLength; n++) {
            if (stringBuilder.charAt(n) != equal.charAt(n)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Creates a sub-string builder from the
     * current string builder.
     *
     * @param stringBuilder the string builder.
     * @param start         the starting index.
     * @param end           the ending index.
     * @return a string builder that contains the
     * characters between the indices.
     */
    @NonNull
    public static StringBuilder substring(@NonNull StringBuilder stringBuilder, int start, int end) {
        StringBuilder newStringBuilder = new StringBuilder(stringBuilder);
        newStringBuilder.replace(end, stringBuilder.length(), EMPTY);
        newStringBuilder.replace(0, start, EMPTY);

        return newStringBuilder;
    }
}
