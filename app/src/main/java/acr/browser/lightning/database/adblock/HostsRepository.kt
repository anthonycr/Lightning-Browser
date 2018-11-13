package acr.browser.lightning.database.adblock

import io.reactivex.Completable
import io.reactivex.Single

/**
 * A repository that stores [Host].
 */
interface HostsRepository {

    /**
     * Add the [List] of [Host] to the repository.
     *
     * @return A [Completable] that completes when the addition finishes.
     */
    fun addHosts(hosts: List<Host>): Completable

    /**
     * Remove all hosts in the repository.
     *
     * @return A [Completable] that completes when the removal finishes.
     */
    fun removeAllHosts(): Completable

    /**
     * @return A [Single] that emits `true` if the repository contains the [Host], `false`
     * otherwise.
     */
    fun containsHost(host: Host): Single<Boolean>

}
