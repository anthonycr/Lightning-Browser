/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.html.history;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.anthonycr.bonsai.Completable;
import com.anthonycr.bonsai.CompletableAction;
import com.anthonycr.bonsai.CompletableSubscriber;
import com.anthonycr.bonsai.Single;
import com.anthonycr.bonsai.SingleAction;
import com.anthonycr.bonsai.SingleOnSubscribe;
import com.anthonycr.bonsai.SingleSubscriber;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import acr.browser.lightning.BrowserApp;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.database.history.HistoryModel;
import acr.browser.lightning.utils.Preconditions;
import acr.browser.lightning.utils.Utils;

public class HistoryPage {

    private static final String TAG = "HistoryPage";

    public static final String FILENAME = "history.html";

    /**
     * Get the file that the history page is stored in
     * or should be stored in.
     *
     * @param application the application used to access the file.
     * @return a valid file object, note that the file might not exist.
     */
    @NonNull
    private static File getHistoryPageFile(@NonNull Application application) {
        return new File(application.getFilesDir(), FILENAME);
    }

    @Inject Application mApp;
    @Inject HistoryModel mHistoryModel;

    public HistoryPage() {
        BrowserApp.getAppComponent().inject(this);
    }

    @NonNull
    public Single<String> getHistoryPage() {
        return Single.create(new SingleAction<String>() {
            @Override
            public void onSubscribe(@NonNull final SingleSubscriber<String> subscriber) {
                mHistoryModel.lastHundredVisitedHistoryItems()
                    .subscribe(new SingleOnSubscribe<List<HistoryItem>>() {
                        @Override
                        public void onItem(@Nullable List<HistoryItem> item) {

                            Preconditions.checkNonNull(item);

                            HistoryPageBuilder historyPageBuilder = new HistoryPageBuilder(mApp);

                            File historyWebPage = getHistoryPageFile(mApp);
                            FileWriter historyWriter = null;
                            try {
                                //noinspection IOResourceOpenedButNotSafelyClosed
                                historyWriter = new FileWriter(historyWebPage, false);
                                historyWriter.write(historyPageBuilder.buildPage(item));
                            } catch (IOException e) {
                                Log.e(TAG, "Unable to write history page to disk", e);
                            } finally {
                                Utils.close(historyWriter);
                            }

                            subscriber.onItem(Constants.FILE + historyWebPage);
                            subscriber.onComplete();
                        }
                    });
            }
        });
    }

    /**
     * Use this observable to immediately delete the history
     * page. This will clear the cached history page that was
     * stored on file.
     *
     * @return a completable that deletes the history page
     * when subscribed.
     */
    @NonNull
    public static Completable deleteHistoryPage(@NonNull final Application application) {
        return Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                File historyWebPage = getHistoryPageFile(application);
                if (historyWebPage.exists()) {
                    historyWebPage.delete();
                }

                subscriber.onComplete();
            }
        });
    }

}