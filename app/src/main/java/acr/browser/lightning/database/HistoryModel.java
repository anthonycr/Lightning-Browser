package acr.browser.lightning.database;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.anthonycr.bonsai.Completable;
import com.anthonycr.bonsai.CompletableAction;
import com.anthonycr.bonsai.CompletableSubscriber;
import com.anthonycr.bonsai.Single;
import com.anthonycr.bonsai.SingleAction;
import com.anthonycr.bonsai.SingleSubscriber;

import java.util.List;

/**
 * A model class providing reactive bindings
 * with the underlying history database.
 */
public final class HistoryModel {

    private HistoryModel() {}

    @NonNull
    public static Completable deleteHistory() {
        return Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                HistoryDatabase.getInstance().deleteHistory();

                subscriber.onComplete();
            }
        });
    }

    @NonNull
    public static Completable deleteHistoryItem(@NonNull final String url) {
        return Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                HistoryDatabase.getInstance().deleteHistoryItem(url);

                subscriber.onComplete();
            }
        });
    }

    @NonNull
    public static Completable visitHistoryItem(@NonNull final String url, @Nullable final String title) {
        return Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                HistoryDatabase.getInstance().visitHistoryItem(url, title);

                subscriber.onComplete();
            }
        });
    }

    @NonNull
    public static Single<List<HistoryItem>> findHistoryItemsContaining(@NonNull final String query) {
        return Single.create(new SingleAction<List<HistoryItem>>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<List<HistoryItem>> subscriber) {
                List<HistoryItem> result = HistoryDatabase.getInstance().findItemsContaining(query);

                subscriber.onItem(result);
                subscriber.onComplete();
            }
        });
    }

    @NonNull
    public static Single<List<HistoryItem>> lastHundredVisitedHistoryItems() {
        return Single.create(new SingleAction<List<HistoryItem>>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<List<HistoryItem>> subscriber) {
                List<HistoryItem> result = HistoryDatabase.getInstance().getLastHundredItems();

                subscriber.onItem(result);
                subscriber.onComplete();
            }
        });
    }
}
