package acr.browser.lightning.adblock.whitelist

/**
 * The model that determines if a URL is whitelisted or not.
 */
interface WhitelistModel {

    /**
     * Returns `true` if the [url] is whitelisted from having ads blocked on it, `false` otherwise.
     */
    fun isUrlWhitelisted(url: String): Boolean

    /**
     * Adds the provided [url] to the whitelist.
     */
    fun addUrlToWhitelist(url: String)

    /**
     * Removes the provided [url] from the whitelist.
     */
    fun removeUrlFromWhitelist(url: String)

}