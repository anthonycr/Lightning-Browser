package acr.browser.lightning.adblock

import acr.browser.lightning.extensions.inlineReplace
import acr.browser.lightning.extensions.inlineTrim
import acr.browser.lightning.extensions.stringEquals
import acr.browser.lightning.extensions.substringToBuilder
import acr.browser.lightning.log.Logger
import android.app.Application
import io.reactivex.Single
import java.io.InputStreamReader
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A [HostsDataSource] that reads from the hosts list in assets.
 */
@Singleton
class AssetsHostsDataSource @Inject constructor(
    private val application: Application,
    private val logger: Logger
) : HostsDataSource {

    /**
     * A [Single] that reads through a hosts file and extracts the domains that should be redirected
     * to localhost (a.k.a. IP address 127.0.0.1). It can handle files that simply have a list of
     * host names to block, or it can handle a full blown hosts file. It will strip out comments,
     * references to the base IP address and just extract the domains to be used.
     */
    override fun loadHosts(): Single<List<String>> = Single.create { emitter ->
        val asset = application.assets
        val reader = InputStreamReader(asset.open(BLOCKED_DOMAINS_LIST_FILE_NAME))
        val lineBuilder = StringBuilder()
        val time = System.currentTimeMillis()

        val domains = ArrayList<String>(1)

        reader.use { inputStreamReader ->
            inputStreamReader.forEachLine {
                lineBuilder.append(it)

                parseString(lineBuilder, domains)
                lineBuilder.setLength(0)
            }
        }

        logger.log(TAG, "Loaded ad list in: ${(System.currentTimeMillis() - time)} ms")
        emitter.onSuccess(domains)
    }

    companion object {

        private const val TAG = "AdBlock"
        private const val BLOCKED_DOMAINS_LIST_FILE_NAME = "hosts.txt"
        private const val LOCAL_IP_V4 = "127.0.0.1"
        private const val LOCAL_IP_V4_ALT = "0.0.0.0"
        private const val LOCAL_IP_V6 = "::1"
        private const val LOCALHOST = "localhost"
        private const val COMMENT = "#"
        private const val TAB = "\t"
        private const val SPACE = " "
        private const val EMPTY = ""

        @JvmStatic
        internal fun parseString(lineBuilder: StringBuilder, parsedList: MutableList<String>) {
            if (lineBuilder.isNotEmpty() && !lineBuilder.startsWith(COMMENT)) {
                lineBuilder.inlineReplace(LOCAL_IP_V4, EMPTY)
                lineBuilder.inlineReplace(LOCAL_IP_V4_ALT, EMPTY)
                lineBuilder.inlineReplace(LOCAL_IP_V6, EMPTY)
                lineBuilder.inlineReplace(TAB, EMPTY)

                val comment = lineBuilder.indexOf(COMMENT)
                if (comment >= 0) {
                    lineBuilder.replace(comment, lineBuilder.length, EMPTY)
                }

                lineBuilder.inlineTrim()

                if (lineBuilder.isNotEmpty() && !lineBuilder.stringEquals(LOCALHOST)) {
                    while (lineBuilder.contains(SPACE)) {
                        val space = lineBuilder.indexOf(SPACE)
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
    }

}
