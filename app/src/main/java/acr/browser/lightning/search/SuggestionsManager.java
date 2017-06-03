package acr.browser.lightning.search;

import android.app.Application;
import android.support.annotation.NonNull;

import com.anthonycr.bonsai.Single;
import com.anthonycr.bonsai.SingleAction;
import com.anthonycr.bonsai.SingleSubscriber;

import java.util.List;

import acr.browser.lightning.database.HistoryItem;

class SuggestionsManager {

    private static volatile boolean sIsTaskExecuting;

    static boolean isRequestInProgress() {
        return sIsTaskExecuting;
    }

    @NonNull
    static Single<List<HistoryItem>> createGoogleQueryObservable(@NonNull final String query,
                                                                 @NonNull final Application application) {
        return Single.create(new SingleAction<List<HistoryItem>>() {
            @Override
            public void onSubscribe(@NonNull final SingleSubscriber<List<HistoryItem>> subscriber) {
                sIsTaskExecuting = true;
                List<HistoryItem> results = new GoogleSuggestionsModel(application).getResults(query);
                subscriber.onItem(results);
                subscriber.onComplete();
                sIsTaskExecuting = false;
            }
        });
    }

    @NonNull
    static Single<List<HistoryItem>> createBaiduQueryObservable(@NonNull final String query,
                                                                 @NonNull final Application application) {
        return Single.create(new SingleAction<List<HistoryItem>>() {
            @Override
            public void onSubscribe(@NonNull final SingleSubscriber<List<HistoryItem>> subscriber) {
                sIsTaskExecuting = true;
                List<HistoryItem> results = new BaiduSuggestionsModel(application).getResults(query);
                subscriber.onItem(results);
                subscriber.onComplete();
                sIsTaskExecuting = false;
            }
        });
    }

    @NonNull
    static Single<List<HistoryItem>> createDuckQueryObservable(@NonNull final String query,
                                                               @NonNull final Application application) {
        return Single.create(new SingleAction<List<HistoryItem>>() {
            @Override
            public void onSubscribe(@NonNull final SingleSubscriber<List<HistoryItem>> subscriber) {
                sIsTaskExecuting = true;
                List<HistoryItem> results = new DuckSuggestionsModel(application).getResults(query);
                subscriber.onItem(results);
                subscriber.onComplete();
                sIsTaskExecuting = false;
            }
        });
    }

}
