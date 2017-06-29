/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.constant;

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
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.BrowserApp;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.database.history.HistoryModel;
import acr.browser.lightning.utils.Preconditions;
import acr.browser.lightning.utils.Utils;

public class HistoryPage {

    private static final String TAG = "HistoryPage";

    public static final String FILENAME = "history.html";

    private static final String HEADING_1 = "<!DOCTYPE html><html xmlns=http://www.w3.org/1999/xhtml>\n" +
        "<head>\n" +
        "<meta content=en-us http-equiv=Content-Language />\n" +
        "<meta content='text/html; charset=utf-8' http-equiv=Content-Type />\n" +
        "<meta name=viewport content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>\n" +
        "<title>";

    private static final String HEADING_2 = "</title></head><style>body,html {margin: 0px; padding: 0px;}" +
        ".box { vertical-align:middle;position:relative; display: block; margin: 0px;padding-left:14px;padding-right:14px;padding-top:9px;padding-bottom:9px; background-color:#fff;border-bottom: 1px solid #d2d2d2;font-family: Arial;color: #444;font-size: 12px;}" +
        ".box a { width: 100%; height: 100%; position: absolute; left: 0; top: 0;}" +
        ".black {color: black;font-size: 15px;font-family: Arial; white-space: nowrap; overflow: hidden;margin:auto; text-overflow: ellipsis; -o-text-overflow: ellipsis; -ms-text-overflow: ellipsis;}" +
        ".font {color: gray;font-size: 10px;font-family: Arial; white-space: nowrap; overflow: hidden;margin:auto; text-overflow: ellipsis; -o-text-overflow: ellipsis; -ms-text-overflow: ellipsis;}" +
        "</style><body><div id=\"content\">";

    private static final String PART1 = "<div class=box><a href='";

    private static final String PART2 = "'></a><p class='black'>";

    private static final String PART3 = "</p><p class='font'>";

    private static final String PART4 = "</p></div>";

    private static final String END = "</div></body></html>";

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

    @NonNull private final String mTitle;

    @Inject Application mApp;
    @Inject HistoryModel mHistoryModel;

    public HistoryPage() {
        BrowserApp.getAppComponent().inject(this);
        mTitle = mApp.getString(R.string.action_history);
    }

    @NonNull
    public Single<String> getHistoryPage() {
        return Single.create(new SingleAction<String>() {
            @Override
            public void onSubscribe(@NonNull final SingleSubscriber<String> subscriber) {
                final StringBuilder historyBuilder = new StringBuilder(HEADING_1 + mTitle + HEADING_2);

                mHistoryModel.lastHundredVisitedHistoryItems()
                    .subscribe(new SingleOnSubscribe<List<HistoryItem>>() {
                        @Override
                        public void onItem(@Nullable List<HistoryItem> item) {

                            Preconditions.checkNonNull(item);
                            Iterator<HistoryItem> it = item.iterator();
                            HistoryItem helper;
                            while (it.hasNext()) {
                                helper = it.next();
                                historyBuilder.append(PART1);
                                historyBuilder.append(helper.getUrl());
                                historyBuilder.append(PART2);
                                historyBuilder.append(helper.getTitle());
                                historyBuilder.append(PART3);
                                historyBuilder.append(helper.getUrl());
                                historyBuilder.append(PART4);
                            }

                            historyBuilder.append(END);
                            File historyWebPage = getHistoryPageFile(mApp);
                            FileWriter historyWriter = null;
                            try {
                                //noinspection IOResourceOpenedButNotSafelyClosed
                                historyWriter = new FileWriter(historyWebPage, false);
                                historyWriter.write(historyBuilder.toString());
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