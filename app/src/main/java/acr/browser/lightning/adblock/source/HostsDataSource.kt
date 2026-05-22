package acr.browser.lightning.adblock.source

/**
 * A data source that contains hosts.
 */
interface HostsDataSource {

    /**
     * Load the hosts and return them as a [HostsResult].
     */
    suspend fun loadHosts(): HostsResult

    /**
     * The unique [String] identifier for this source.
     */
    suspend fun identifier(): String

}
