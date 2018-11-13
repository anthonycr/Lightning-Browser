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

    private val mutableSet: MutableSet<Host> = hashSetOf()

    override fun addHosts(hosts: List<Host>): Completable = Completable.fromAction {
        mutableSet.addAll(hosts)
    }

    override fun removeAllHosts(): Completable = Completable.fromAction(mutableSet::clear)

    override fun containsHost(host: Host): Single<Boolean> = Single.fromCallable {
        mutableSet.contains(host)
    }

}
