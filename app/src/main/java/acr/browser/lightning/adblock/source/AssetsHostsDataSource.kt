package acr.browser.lightning.adblock.source

import acr.browser.lightning.adblock.HostsFileParser
import acr.browser.lightning.log.Logger
import android.content.res.AssetManager
import io.reactivex.Single
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A [HostsDataSource] that reads from the hosts list in assets.
 *
 * @param assetManager The store for application assets.
 * @param logger The logger used to log status.
 */
@Singleton
class AssetsHostsDataSource @Inject constructor(
    private val assetManager: AssetManager,
    private val logger: Logger
) : HostsDataSource {

    /**
     * A [Single] that reads through a hosts file and extracts the domains that should be redirected
     * to localhost (a.k.a. IP address 127.0.0.1). It can handle files that simply have a list of
     * host names to block, or it can handle a full blown hosts file. It will strip out comments,
     * references to the base IP address and just extract the domains to be used.
     *
     * @see HostsDataSource.loadHosts
     */
    override fun loadHosts(): Single<HostsResult> = Single.create { emitter ->
        val reader = InputStreamReader(assetManager.open(BLOCKED_DOMAINS_LIST_FILE_NAME))
        val hostsFileParser = HostsFileParser(logger)

        val domains = hostsFileParser.parseInput(reader)

        logger.log(TAG, "Loaded ${domains.size} domains")
        emitter.onSuccess(HostsResult.Success(domains))
    }

    companion object {
        private const val TAG = "AssetsHostsDataSource"
        private const val BLOCKED_DOMAINS_LIST_FILE_NAME = "hosts.txt"
    }

}
