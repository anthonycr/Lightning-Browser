package acr.browser.lightning.extensions

private const val SPACE = " "
private const val EMPTY = ""

/**
 * Replace the first string found in a string builder with another string.
 *
 * @param toReplace the string to replace.
 * @param replacement the replacement string.
 */
fun StringBuilder.inlineReplace(toReplace: String,
                                replacement: String) {
    val index = indexOf(toReplace)
    if (index >= 0) {
        replace(index, index + toReplace.length, replacement)
    }
}

/**
 * Trims a string builder of any spaces at the beginning and end.
 */
fun StringBuilder.inlineTrim() {
    while (indexOf(SPACE) == 0) {
        replace(0, 1, EMPTY)
    }

    while (lastIndexOf(SPACE) == length - 1 && length > 0) {
        replace(length - 1, length, EMPTY)
    }
}

/**
 * Determines equality between a string builder and a string.
 *
 * @param equal the string.
 * @return true if the string represented by the string builder is equal to the string.
 */
fun StringBuilder.stringEquals(equal: String): Boolean {
    val builderLength = length
    if (builderLength != equal.length) {
        return false
    }

    return (0 until builderLength).none { this[it] != equal[it] }
}

/**
 * Creates a sub-string builder from the current string builder.
 *
 * @param start the starting index.
 * @param end the ending index.
 * @return a string builder that contains the characters between the indices.
 */
fun StringBuilder.substringToBuilder(start: Int, end: Int): StringBuilder {
    val newStringBuilder = StringBuilder(this)
    newStringBuilder.replace(end, length, EMPTY)
    newStringBuilder.replace(0, start, EMPTY)

    return newStringBuilder
}
