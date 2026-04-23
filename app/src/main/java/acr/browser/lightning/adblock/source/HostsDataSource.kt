package acr.browser.lightning.adblock.source

import io.reactivex.rxjava3.core.Single

/**
 * A data source that contains hosts.
 */
interface HostsDataSource {

    /**
     * Load the hosts and emit them as a [Single] [HostsResult].
     */
    suspend fun loadHosts(): HostsResult

    /**
     * The unique [String] identifier for this source.
     */
    suspend fun identifier(): String

}
