package acr.browser.lightning.database.history;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.anthonycr.bonsai.Completable;
import com.anthonycr.bonsai.CompletableAction;
import com.anthonycr.bonsai.CompletableSubscriber;
import com.anthonycr.bonsai.Single;
import com.anthonycr.bonsai.SingleAction;
import com.anthonycr.bonsai.SingleSubscriber;

import java.util.List;

import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.database.HistoryItem;

/**
 * A model class providing reactive bindings
 * with the underlying history database.
 */
public final class HistoryModel {

    private HistoryModel() {}

    /**
     * An observable that deletes browser history.
     *
     * @return a valid observable.
     */
    @NonNull
    public static Completable deleteHistory() {
        return Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                BrowserApp.getAppComponent()
                    .historyDatabase()
                    .deleteHistory();

                subscriber.onComplete();
            }
        });
    }

    /**
     * An observable that deletes the history
     * entry with the specific URL.
     *
     * @param url the URL of the item to delete.
     * @return a valid observable.
     */
    @NonNull
    public static Completable deleteHistoryItem(@NonNull final String url) {
        return Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                BrowserApp.getAppComponent()
                    .historyDatabase()
                    .deleteHistoryItem(url);

                subscriber.onComplete();
            }
        });
    }

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
    public static Completable visitHistoryItem(@NonNull final String url, @Nullable final String title) {
        return Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                BrowserApp.getAppComponent()
                    .historyDatabase()
                    .visitHistoryItem(url, title);

                subscriber.onComplete();
            }
        });
    }

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
    public static Single<List<HistoryItem>> findHistoryItemsContaining(@NonNull final String query) {
        return Single.create(new SingleAction<List<HistoryItem>>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<List<HistoryItem>> subscriber) {
                List<HistoryItem> result = BrowserApp.getAppComponent()
                    .historyDatabase().findItemsContaining(query);

                subscriber.onItem(result);
                subscriber.onComplete();
            }
        });
    }

    /**
     * An observable that emits a list of the
     * last 100 visited history items.
     *
     * @return a valid observable that emits
     * a list of history items.
     */
    @NonNull
    public static Single<List<HistoryItem>> lastHundredVisitedHistoryItems() {
        return Single.create(new SingleAction<List<HistoryItem>>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<List<HistoryItem>> subscriber) {
                List<HistoryItem> result = BrowserApp.getAppComponent()
                    .historyDatabase().getLastHundredItems();

                subscriber.onItem(result);
                subscriber.onComplete();
            }
        });
    }
}
