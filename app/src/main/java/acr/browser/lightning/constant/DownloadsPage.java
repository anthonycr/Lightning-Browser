/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.constant;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.anthonycr.bonsai.Single;
import com.anthonycr.bonsai.SingleAction;
import com.anthonycr.bonsai.SingleOnSubscribe;
import com.anthonycr.bonsai.SingleSubscriber;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.BrowserApp;
import acr.browser.lightning.database.downloads.DownloadItem;
import acr.browser.lightning.database.downloads.DownloadsModel;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.utils.Preconditions;
import acr.browser.lightning.utils.Utils;

public final class DownloadsPage {

    private static final String TAG = "DownloadsPage";

    public static final String FILENAME = "downloads.html";

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

    @NonNull
    private static File getDownloadsPageFile(@NonNull Application application) {
        return new File(application.getFilesDir(), FILENAME);
    }

    @Inject Application mApp;
    @Inject PreferenceManager mPreferenceManager;
    @Inject DownloadsModel mManager;

    @NonNull private final String mTitle;

    public DownloadsPage() {
        BrowserApp.getAppComponent().inject(this);
        mTitle = mApp.getString(R.string.action_downloads);
    }

    @NonNull
    public Single<String> getDownloadsPage() {
        return Single.create(new SingleAction<String>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                buildDownloadsPage();

                File downloadsWebPage = getDownloadsPageFile(mApp);

                subscriber.onItem(Constants.FILE + downloadsWebPage);
                subscriber.onComplete();
            }
        });
    }

    private void buildDownloadsPage() {
        mManager.getAllDownloads()
            .subscribe(new SingleOnSubscribe<List<DownloadItem>>() {
                @Override
                public void onItem(@Nullable List<DownloadItem> list) {
                    Preconditions.checkNonNull(list);
                    String directory = mPreferenceManager.getDownloadDirectory();

                    final StringBuilder downloadsBuilder = new StringBuilder(HEADING_1 + mTitle + HEADING_2);

                    for (int n = 0, size = list.size(); n < size; n++) {
                        final DownloadItem item = list.get(n);
                        downloadsBuilder.append(PART1);
                        downloadsBuilder.append("file://");
                        downloadsBuilder.append(directory);
                        downloadsBuilder.append("/");
                        downloadsBuilder.append(item.getTitle());
                        downloadsBuilder.append(PART2);
                        downloadsBuilder.append(item.getTitle());

                        if (!item.getContentSize().isEmpty()) {
                            downloadsBuilder.append(" [");
                            downloadsBuilder.append(item.getContentSize());
                            downloadsBuilder.append("]");
                        }

                        downloadsBuilder.append(PART3);
                        downloadsBuilder.append(item.getUrl());
                        downloadsBuilder.append(PART4);
                    }
                    downloadsBuilder.append(END);
                    FileWriter bookWriter = null;
                    try {
                        //noinspection IOResourceOpenedButNotSafelyClosed
                        bookWriter = new FileWriter(getDownloadsPageFile(mApp), false);
                        bookWriter.write(downloadsBuilder.toString());
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to write download page to disk", e);
                    } finally {
                        Utils.close(bookWriter);
                    }
                }
            });
    }

}
