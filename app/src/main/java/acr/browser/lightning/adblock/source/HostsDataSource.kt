package acr.browser.lightning.adblock.source

import io.reactivex.Single

/**
 * A data source that contains hosts.
 */
interface HostsDataSource {

    /**
     * Load the hosts and emit them as a [Single] [HostsResult].
     */
    fun loadHosts(): Single<HostsResult>

    /**
     * The unique [String] identifier for this source.
     */
    fun identifier(): String

}
