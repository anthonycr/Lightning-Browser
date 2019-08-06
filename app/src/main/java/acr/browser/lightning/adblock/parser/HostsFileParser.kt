package acr.browser.lightning.adblock.parser

import acr.browser.lightning.extensions.*
import acr.browser.lightning.log.Logger
import java.io.InputStreamReader

/**
 * A parser for a hosts file.
 */
class HostsFileParser(
    private val logger: Logger
) {

    private val lineBuilder = StringBuilder()

    /**
     * Parse the lines of the [input] from a hosts file and return the list of [String] domains held
     * in that file.
     */
    fun parseInput(input: InputStreamReader): List<String> {
        val time = System.currentTimeMillis()

        val domains = ArrayList<String>(100)

        input.use { inputStreamReader ->
            inputStreamReader.forEachLine {
                parseLine(it, domains)
            }
        }

        logger.log(TAG, "Parsed ad list in: ${(System.currentTimeMillis() - time)} ms")

        return domains
    }

    /**
     * Parse a [line] from a hosts file and populate the [parsedList] with the extracted hosts.
     */
    private fun parseLine(line: String, parsedList: MutableList<String>) {
        lineBuilder.setLength(0)
        lineBuilder.append(line)
        if (lineBuilder.isNotEmpty() && lineBuilder[0] != COMMENT_CHAR) {
            lineBuilder.inlineReplace(LOCAL_IP_V4, EMPTY)
            lineBuilder.inlineReplace(LOCAL_IP_V4_ALT, EMPTY)
            lineBuilder.inlineReplace(LOCAL_IP_V6, EMPTY)
            lineBuilder.inlineReplaceChar(TAB_CHAR, SPACE_CHAR)

            val comment = lineBuilder.indexOfChar(COMMENT_CHAR)
            if (comment > 0) {
                lineBuilder.setLength(comment)
            } else if (comment == 0) {
                return
            }

            lineBuilder.inlineTrim()

            if (lineBuilder.isNotEmpty() && !lineBuilder.stringEquals(LOCALHOST)) {
                while (lineBuilder.containsChar(SPACE_CHAR)) {
                    val space = lineBuilder.indexOfChar(SPACE_CHAR)
                    val partial = lineBuilder.substringToBuilder(0, space)
                    partial.inlineTrim()

                    val partialLine = partial.toString()

                    // Add string to list
                    parsedList.add(partialLine)
                    lineBuilder.inlineReplace(partialLine, EMPTY)
                    lineBuilder.inlineTrim()
                }
                if (lineBuilder.isNotEmpty()) {
                    // Add string to list.
                    parsedList.add(lineBuilder.toString())
                }
            }
        }
    }

    companion object {
        private const val TAG = "HostsFileParser"

        private const val LOCAL_IP_V4 = "127.0.0.1"
        private const val LOCAL_IP_V4_ALT = "0.0.0.0"
        private const val LOCAL_IP_V6 = "::1"
        private const val LOCALHOST = "localhost"
        private const val COMMENT_CHAR = '#'
        private const val TAB_CHAR = '\t'
        private const val SPACE_CHAR = ' '
        private const val EMPTY = ""
    }
}
