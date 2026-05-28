package acr.browser.lightning.adblock.source

/**
 * The provider for the hosts data source.
 */
interface HostsDataSourceProvider {

    /**
     * Create the hosts data source.
     */
    suspend fun createHostsDataSource(): HostsDataSource

}
