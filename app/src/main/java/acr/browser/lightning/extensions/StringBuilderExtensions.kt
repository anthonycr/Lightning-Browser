package acr.browser.lightning.extensions

private const val SPACE = ' '
private const val EMPTY = ""

/**
 * Replace the first string found in a string builder with another string.
 *
 * @param toReplace The string to replace.
 * @param replacement The replacement string.
 */
fun StringBuilder.inlineReplace(toReplace: String, replacement: String) {
    val index = indexOf(toReplace)
    if (index >= 0) {
        replace(index, index + toReplace.length, replacement)
    }
}

/**
 * Replace the first char wound in a string builder with another char.
 *
 * @param toReplace The char to replace.
 * @param replacement The replacement char.
 */
fun StringBuilder.inlineReplaceChar(toReplace: Char, replacement: Char) {
    val index = indexOfChar(toReplace)
    if (index >= 0) {
        setCharAt(index, replacement)
    }
}

/**
 * Returns the index of the provided [char] in the string or -1 if it cannot be found.
 */
fun StringBuilder.indexOfChar(char: Char): Int {
    for (i in 0 until length) {
        if (this[i] == char) {
            return i
        }
    }
    return -1
}

/**
 * Returns true if the string contains the [char], false otherwise.
 */
fun StringBuilder.containsChar(char: Char): Boolean {
    return indexOfChar(char) > -1
}

/**
 * Trims a string builder of any spaces at the beginning and end.
 */
fun StringBuilder.inlineTrim() {
    var replacements = 0
    for (i in length - 1 downTo 0) {
        if (this[i] == SPACE) {
            replacements++
        } else {
            break
        }
    }
    if (replacements > 0) {
        setLength(length - replacements)
    }

    var newStartIndex = 0
    for (i in 0 until length) {
        if (this[i] == SPACE) {
            newStartIndex++
        } else {
            break
        }
    }
    if (newStartIndex > 0) {
        replace(0, newStartIndex, EMPTY)
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

    for (i in 0 until builderLength) {
        if (this[i] != equal[i]) {
            return false
        }
    }
    return true
}

/**
 * Creates a sub-string builder from the current string builder.
 *
 * @param start the starting index.
 * @param end the ending index, must be greater than [start].
 * @return a string builder that contains the characters between the indices.
 */
fun StringBuilder.substringToBuilder(start: Int, end: Int): StringBuilder {
    require(end > start) { "End must be greater than start." }
    this.substring(start, end)
    return StringBuilder(this).apply {
        setLength(end)
        replace(0, start, EMPTY)
    }
}
