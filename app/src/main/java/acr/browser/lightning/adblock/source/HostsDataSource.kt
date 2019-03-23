package acr.browser.lightning.adblock.source

import io.reactivex.Single

/**
 * A data source that contains hosts.
 */
interface HostsDataSource {

    /**
     * Load the hosts and emit them as a [Single] [List].
     */
    fun loadHosts(): Single<List<String>>

}
