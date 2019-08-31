package acr.browser.lightning.database.allowlist

import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single

/**
 * The interface used to communicate with the ad block whitelist interface.
 */
interface AdBlockAllowListRepository {

    /**
     * Returns a [Single] that emits a list of all [AllowListEntry] in the database.
     */
    fun allAllowListItems(): Single<List<AllowListEntry>>

    /**
     * Returns a [Maybe] that emits the [AllowListEntry] associated with the [url] if there is one.
     */
    fun allowListItemForUrl(url: String): Maybe<AllowListEntry>

    /**
     * Returns a [Completable] that adds a [AllowListEntry] to the database and completes when done.
     */
    fun addAllowListItem(whitelistItem: AllowListEntry): Completable

    /**
     * Returns a [Completable] that removes a [AllowListEntry] from the database and completes when
     * done.
     */
    fun removeAllowListItem(whitelistItem: AllowListEntry): Completable

    /**
     * Returns a [Completable] that clears the entire database.
     */
    fun clearAllowList(): Completable
}
