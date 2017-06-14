package acr.browser.lightning.database.history;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.anthonycr.bonsai.Completable;
import com.anthonycr.bonsai.Single;

import java.util.List;

import acr.browser.lightning.database.HistoryItem;

/**
 * An interface that should be used to communicate
 * with the history database.
 * <p>
 * Created by anthonycr on 6/9/17.
 */
public interface HistoryModel {

    /**
     * An observable that deletes browser history.
     *
     * @return a valid observable.
     */
    @NonNull
    Completable deleteHistory();

    /**
     * An observable that deletes the history
     * entry with the specific URL.
     *
     * @param url the URL of the item to delete.
     * @return a valid observable.
     */
    @NonNull
    Completable deleteHistoryItem(@NonNull final String url);

    /**
     * An observable that visits the URL by
     * adding it to the database if it doesn't
     * exist or updating the time visited if
     * it does.
     *
     * @param url   the URL of the item that was visited.
     * @param title the title of the item that was visited.
     * @return a valid observable.
     */
    @NonNull
    Completable visitHistoryItem(@NonNull final String url, @Nullable final String title);

    /**
     * An observable that finds all history items
     * containing the given query. If the query
     * is contained anywhere within the title or
     * the URL of the history item, it will be
     * returned. For the sake of performance, only
     * the first five items will be emitted.
     *
     * @param query the query to search for.
     * @return a valid observable that emits
     * a list of history items.
     */
    @NonNull
    Single<List<HistoryItem>> findHistoryItemsContaining(@NonNull final String query);

    /**
     * An observable that emits a list of the
     * last 100 visited history items.
     *
     * @return a valid observable that emits
     * a list of history items.
     */
    @NonNull
    Single<List<HistoryItem>> lastHundredVisitedHistoryItems();
}
