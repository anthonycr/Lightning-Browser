package acr.browser.lightning.adblock.allowlist

/**
 * The model that determines if a URL is whitelisted or not.
 */
interface AllowListModel {

    /**
     * Returns `true` if the [url] is allowed to display ads, `false` otherwise.
     */
    fun isUrlAllowedAds(url: String): Boolean

    /**
     * Adds the provided [url] to the list of sites that are allowed to display ads.
     */
    fun addUrlToAllowList(url: String)

    /**
     * Removes the provided [url] from the whitelist.
     */
    fun removeUrlFromAllowList(url: String)

}
