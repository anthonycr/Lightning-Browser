package acr.browser.lightning.adblock

import acr.browser.lightning.extensions.inlineReplace
import acr.browser.lightning.extensions.inlineTrim
import acr.browser.lightning.extensions.stringEquals
import acr.browser.lightning.extensions.substringToBuilder
import android.app.Application
import android.util.Log
import io.reactivex.Completable
import io.reactivex.Scheduler
import java.io.InputStreamReader
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * An implementation of the ad blocker that checks the URLs against the hosts stored in assets.
 * Checking whether a URL is an ad is an `O(log n)` operation.
 */
@Singleton
class AssetsAdBlocker @Inject internal constructor(
        private val application: Application,
        @Named("disk") diskScheduler: Scheduler
) : AdBlocker {

    private val blockedDomainsSet = HashSet<String>()

    init {
        loadHostsFile().subscribeOn(diskScheduler).subscribe()
    }

    override fun isAd(url: String?): Boolean {
        if (url == null) {
            return false
        }

        val domain = try {
            getDomainName(url)
        } catch (e: URISyntaxException) {
            Log.d(TAG, "URL '$url' is invalid", e)
            return false
        }

        val isOnBlacklist = blockedDomainsSet.contains(domain)
        if (isOnBlacklist) {
            Log.d(TAG, "URL '$url' is an ad")
        }
        return isOnBlacklist
    }

    /**
     * This Completable reads through a hosts file and extracts the domains that should
     * be redirected to localhost (a.k.a. IP address 127.0.0.1). It can handle files that
     * simply have a list of host names to block, or it can handle a full blown hosts file.
     * It will strip out comments, references to the base IP address and just extract the
     * domains to be used.
     *
     * @return a Completable that will load the hosts file into memory.
     */
    private fun loadHostsFile() = Completable.fromAction {
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

        blockedDomainsSet.addAll(domains)
        Log.d(TAG, "Loaded ad list in: ${(System.currentTimeMillis() - time)} ms")
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

        /**
         * Returns the probable domain name for a given URL
         *
         * @param url the url to parse
         * @return returns the domain
         * @throws URISyntaxException throws an exception if the string cannot form a URI
         */
        @JvmStatic
        @Throws(URISyntaxException::class)
        private fun getDomainName(url: String): String {
            val host = url.indexOf('/', 8)
                    .takeIf { it != -1 }
                    ?.let(url::take)
                    ?: url

            val uri = URI(host)
            val domain = uri.host ?: return host

            return if (domain.startsWith("www.")) {
                domain.substring(4)
            } else {
                domain
            }
        }

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
