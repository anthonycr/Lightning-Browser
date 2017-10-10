package acr.browser.lightning.database.whitelist

import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single

/**
 * The interface used to communicate with the ad block whitelist interface.
 */
interface AdBlockWhitelistRepository {

    /**
     * Returns a [Single] that emits a list of all [WhitelistItem] in the database.
     */
    fun allWhitelistItems(): Single<List<WhitelistItem>>

    /**
     * Returns a [Maybe] that emits the [WhitelistItem] associated with the [url] if there is one.
     */
    fun whitelistItemForUrl(url: String): Maybe<WhitelistItem>

    /**
     * Returns a [Completable] that adds a [WhitelistItem] to the database and completes when done.
     */
    fun addWhitelistItem(whitelistItem: WhitelistItem): Completable

    /**
     * Returns a [Completable] that removes a [WhitelistItem] from the database and completes when
     * done.
     */
    fun removeWhitelistItem(whitelistItem: WhitelistItem): Completable

    /**
     * Returns a [Completable] that clears the entire database.
     */
    fun clearWhitelist(): Completable
}