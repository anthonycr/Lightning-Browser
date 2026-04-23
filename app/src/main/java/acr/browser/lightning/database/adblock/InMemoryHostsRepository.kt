package acr.browser.lightning.database.adblock

import javax.inject.Inject
import javax.inject.Singleton

/**
 * An in memory hosts repository. Hosts are stored in a [Set].
 */
@Singleton
class InMemoryHostsRepository @Inject constructor() : HostsRepository {

    private var mutableHostsSet: Set<Host> = emptySet()

    override suspend fun addHosts(hosts: List<Host>) {
        mutableHostsSet = hosts.toSet()
    }

    override suspend fun removeAllHosts() {
        mutableHostsSet = emptySet()
    }

    override fun containsHost(host: Host): Boolean = mutableHostsSet.contains(host)

    override fun hasHosts(): Boolean = mutableHostsSet.isNotEmpty()

    override suspend fun allHosts(): List<Host> = mutableHostsSet.toList()
}
