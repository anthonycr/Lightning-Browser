package acr.browser.lightning.database.adblock

import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An in memory hosts repository. Hosts are stored in a [Set].
 */
@Singleton
class InMemoryHostsRepository @Inject constructor() : HostsRepository {

    private val mutableHostsSet: MutableSet<Host> = hashSetOf()

    override fun addHosts(hosts: List<Host>): Completable = Completable.fromAction {
        mutableHostsSet.addAll(hosts)
    }

    override fun removeAllHosts(): Completable = Completable.fromAction(mutableHostsSet::clear)

    override fun containsHost(host: Host): Boolean = mutableHostsSet.contains(host)

    override fun hasHosts(): Boolean = mutableHostsSet.size > 0

    override fun allHosts(): Single<List<Host>> = Single.just(mutableHostsSet.toList())
}
