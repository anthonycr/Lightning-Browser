package acr.browser.lightning.database.adblock

/**
 * A repository that stores [Host].
 */
interface HostsRepository {

    /**
     * Add the [List] of [Host] to the repository.
     */
    suspend fun addHosts(hosts: List<Host>)

    /**
     * Remove all hosts in the repository.
     */
    suspend fun removeAllHosts()

    /**
     * @return `true` if the repository contains the [Host], `false` otherwise.
     */
    fun containsHost(host: Host): Boolean

    /**
     * @return `true` if the repository has been initialized, `false` otherwise.
     */
    fun hasHosts(): Boolean

    /**
     * @return A list of all hosts in the repository.
     */
    suspend fun allHosts(): List<Host>

}
