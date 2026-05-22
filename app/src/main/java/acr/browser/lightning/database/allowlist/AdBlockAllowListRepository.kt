package acr.browser.lightning.database.allowlist

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

/**
 * The interface used to communicate with the ad block whitelist interface.
 */
interface AdBlockAllowListRepository {

    /**
     * Returns a [Single] that emits a list of all [AllowListEntry] in the database.
     */
    suspend fun allAllowListItems(): List<AllowListEntry>

    /**
     * Returns a [Maybe] that emits the [AllowListEntry] associated with the [domain] if there is
     * one.
     */
    suspend fun allowListItemForUrl(domain: String): AllowListEntry?

    /**
     * Returns a [Completable] that adds a [AllowListEntry] to the database and completes when done.
     */
    suspend fun addAllowListItem(whitelistItem: AllowListEntry)

    /**
     * Returns a [Completable] that removes a [AllowListEntry] from the database and completes when
     * done.
     */
    suspend fun removeAllowListItem(whitelistItem: AllowListEntry)

    /**
     * Returns a [Completable] that clears the entire database.
     */
    suspend fun clearAllowList()
}
