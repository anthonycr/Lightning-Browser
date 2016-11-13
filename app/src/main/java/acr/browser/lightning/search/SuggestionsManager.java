package acr.browser.lightning.search;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

import com.anthonycr.bonsai.Action;
import com.anthonycr.bonsai.Observable;
import com.anthonycr.bonsai.Subscriber;

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

    static Observable<List<HistoryItem>> getObservable(@NonNull final String query, @NonNull final Context context, @NonNull final Source source) {
        final Application application = BrowserApp.get(context);
        return Observable.create(new Action<List<HistoryItem>>() {
            @Override
            public void onSubscribe(@NonNull final Subscriber<List<HistoryItem>> subscriber) {
                sIsTaskExecuting = true;
                switch (source) {
                    case GOOGLE:
                        new GoogleSuggestionsTask(query, application, new SuggestionsResult() {
                            @Override
                            public void resultReceived(@NonNull List<HistoryItem> searchResults) {
                                subscriber.onNext(searchResults);
                                subscriber.onComplete();
                            }
                        }).run();
                        break;
                    case DUCK:
                        new DuckSuggestionsTask(query, application, new SuggestionsResult() {
                            @Override
                            public void resultReceived(@NonNull List<HistoryItem> searchResults) {
                                subscriber.onNext(searchResults);
                                subscriber.onComplete();
                            }
                        }).run();
                }
                sIsTaskExecuting = false;
            }
        });
    }

}
