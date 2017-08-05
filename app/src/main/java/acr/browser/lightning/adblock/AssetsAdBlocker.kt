package acr.browser.lightning.adblock

import acr.browser.lightning.utils.StringBuilderUtils
import android.app.Application
import android.util.Log
import com.anthonycr.bonsai.Completable
import com.anthonycr.bonsai.CompletableAction
import com.anthonycr.bonsai.Schedulers
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An implementation of the ad blocker that checks the URLs against the hosts stored in assets.
 * Checking whether a URL is an ad is an `O(log n)` operation.
 */
@Singleton
class AssetsAdBlocker @Inject
internal constructor(private val application: Application) : AdBlocker {

    private val blockedDomainsList = HashSet<String>()

    init {
        loadHostsFile().subscribeOn(Schedulers.io()).subscribe()
    }

    override fun isAd(url: String?): Boolean {
        if (url == null) {
            return false
        }

        val domain: String
        try {
            domain = getDomainName(url)
        } catch (e: URISyntaxException) {
            Log.d(TAG, "URL '$url' is invalid", e)
            return false
        }

        val isOnBlacklist = blockedDomainsList.contains(domain)
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
    private fun loadHostsFile(): Completable {
        return Completable.create(CompletableAction {
            val asset = application.assets
            val reader = BufferedReader(InputStreamReader(asset.open(BLOCKED_DOMAINS_LIST_FILE_NAME)))
            val lineBuilder = StringBuilder()
            val time = System.currentTimeMillis()

            val domains = ArrayList<String>(1)

            reader.use {
                it.forEachLine {
                    lineBuilder.append(it)

                    parseString(lineBuilder, domains)
                    lineBuilder.setLength(0)
                }
            }

            blockedDomainsList.addAll(domains)
            Log.d(TAG, "Loaded ad list in: ${(System.currentTimeMillis() - time)} ms")
        })
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
            var mutableUrl = url
            val index = mutableUrl.indexOf('/', 8)
            if (index != -1) {
                mutableUrl = mutableUrl.take(index)
            }

            val uri = URI(mutableUrl)
            val domain = uri.host ?: return mutableUrl

            return if (domain.startsWith("www.")) domain.substring(4) else domain
        }

        @JvmStatic
        private fun parseString(lineBuilder: StringBuilder, parsedList: MutableList<String>) {
            if (!StringBuilderUtils.isEmpty(lineBuilder) && !StringBuilderUtils.startsWith(lineBuilder, COMMENT)) {
                StringBuilderUtils.replace(lineBuilder, LOCAL_IP_V4, EMPTY)
                StringBuilderUtils.replace(lineBuilder, LOCAL_IP_V4_ALT, EMPTY)
                StringBuilderUtils.replace(lineBuilder, LOCAL_IP_V6, EMPTY)
                StringBuilderUtils.replace(lineBuilder, TAB, EMPTY)

                val comment = lineBuilder.indexOf(COMMENT)
                if (comment >= 0) {
                    lineBuilder.replace(comment, lineBuilder.length, EMPTY)
                }

                StringBuilderUtils.trim(lineBuilder)

                if (!StringBuilderUtils.isEmpty(lineBuilder) && !StringBuilderUtils.equals(lineBuilder, LOCALHOST)) {
                    while (StringBuilderUtils.contains(lineBuilder, SPACE)) {
                        val space = lineBuilder.indexOf(SPACE)
                        val partial = StringBuilderUtils.substring(lineBuilder, 0, space)
                        StringBuilderUtils.trim(partial)

                        val partialLine = partial.toString()

                        // Add string to list
                        parsedList.add(partialLine)
                        StringBuilderUtils.replace(lineBuilder, partialLine, EMPTY)
                        StringBuilderUtils.trim(lineBuilder)
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
