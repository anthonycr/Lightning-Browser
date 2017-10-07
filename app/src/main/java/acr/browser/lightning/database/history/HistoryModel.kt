package acr.browser.lightning.database.history

import acr.browser.lightning.database.HistoryItem
import io.reactivex.Completable
import io.reactivex.Single

/**
 * An interface that should be used to communicate with the history database.
 *
 * Created by anthonycr on 6/9/17.
 */
interface HistoryModel {

    /**
     * An observable that deletes browser history.
     *
     * @return a valid observable.
     */
    fun deleteHistory(): Completable

    /**
     * An observable that deletes the history entry with the specific URL.
     *
     * @param url the URL of the item to delete.
     * @return a valid observable.
     */
    fun deleteHistoryItem(url: String): Completable

    /**
     * An observable that visits the URL by adding it to the database if it doesn't exist or
     * updating the time visited if it does.
     *
     * @param url   the URL of the item that was visited.
     * @param title the title of the item that was visited.
     * @return a valid observable.
     */
    fun visitHistoryItem(url: String, title: String?): Completable

    /**
     * An observable that finds all history items containing the given query. If the query is
     * contained anywhere within the title or the URL of the history item, it will be returned. For
     * the sake of performance, only the first five items will be emitted.
     *
     * @param query the query to search for.
     * @return a valid observable that emits
     * a list of history items.
     */
    fun findHistoryItemsContaining(query: String): Single<List<HistoryItem>>

    /**
     * An observable that emits a list of the last 100 visited history items.
     *
     * @return a valid observable that emits a list of history items.
     */
    fun lastHundredVisitedHistoryItems(): Single<List<HistoryItem>>
}
