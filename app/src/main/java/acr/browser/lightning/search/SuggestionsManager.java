package acr.browser.lightning.search;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

import com.anthonycr.bonsai.Single;
import com.anthonycr.bonsai.SingleAction;
import com.anthonycr.bonsai.SingleSubscriber;

import java.util.List;

import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.database.HistoryItem;

class SuggestionsManager {

    public enum Source {
        GOOGLE,
        DUCK
    }

    private static volatile boolean sIsTaskExecuting;

    static boolean isRequestInProgress() {
        return sIsTaskExecuting;
    }

    static Single<List<HistoryItem>> getObservable(@NonNull final String query, @NonNull final Context context, @NonNull final Source source) {
        final Application application = BrowserApp.get(context);
        return Single.create(new SingleAction<List<HistoryItem>>() {
            @Override
            public void onSubscribe(@NonNull final SingleSubscriber<List<HistoryItem>> subscriber) {
                sIsTaskExecuting = true;
                switch (source) {
                    case GOOGLE:
                        new GoogleSuggestionsTask(query, application, new SuggestionsResult() {
                            @Override
                            public void resultReceived(@NonNull List<HistoryItem> searchResults) {
                                subscriber.onItem(searchResults);
                                subscriber.onComplete();
                            }
                        }).run();
                        break;
                    case DUCK:
                        new DuckSuggestionsTask(query, application, new SuggestionsResult() {
                            @Override
                            public void resultReceived(@NonNull List<HistoryItem> searchResults) {
                                subscriber.onItem(searchResults);
                                subscriber.onComplete();
                            }
                        }).run();
                }
                sIsTaskExecuting = false;
            }
        });
    }

}
