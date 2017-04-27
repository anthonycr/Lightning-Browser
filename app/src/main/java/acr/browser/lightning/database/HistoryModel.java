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

import acr.browser.lightning.app.BrowserApp;

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
                BrowserApp.getAppComponent()
                    .historyDatabase()
                    .deleteHistory();

                subscriber.onComplete();
            }
        });
    }

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

    @NonNull
    public static Completable visitHistoryItem(@NonNull final String url, @Nullable final String title) {
        return Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                BrowserApp.getAppComponent()
                    .historyDatabase()
                    .visitHistoryItem(url, title);

                System.out.println("SHIT: " + BrowserApp.getAppComponent().historyDatabase().toString());
                System.out.println("SHIT: " + BrowserApp.getAppComponent().historyDatabase().toString());

                subscriber.onComplete();
            }
        });
    }

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
