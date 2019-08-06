package acr.browser.lightning.adblock.source

/**
 * The result of a request for the hosts to block.
 */
sealed class HostsResult {

    /**
     * A successful request.
     *
     * @param hosts The hosts to block.
     */
    data class Success(val hosts: List<String>) : HostsResult()

    /**
     * An unsuccessful request.
     *
     * @param cause The cause of the failure.
     */
    data class Failure(val cause: Exception) : HostsResult()

}
