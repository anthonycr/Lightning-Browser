package acr.browser.lightning.database.allowlist

/**
 * The interface used to communicate with the ad block whitelist interface.
 */
interface AdBlockAllowListRepository {

    /**
     * Returns a list of all [AllowListEntry] in the database.
     */
    suspend fun allAllowListItems(): List<AllowListEntry>

    /**
     * Returns the [AllowListEntry] associated with the [domain] if there is one.
     */
    suspend fun allowListItemForUrl(domain: String): AllowListEntry?

    /**
     * Adds a [AllowListEntry] to the database.
     */
    suspend fun addAllowListItem(whitelistItem: AllowListEntry)

    /**
     * Removes a [AllowListEntry] from the database.
     */
    suspend fun removeAllowListItem(whitelistItem: AllowListEntry)

    /**
     * Clears the entire database.
     */
    suspend fun clearAllowList()
}
