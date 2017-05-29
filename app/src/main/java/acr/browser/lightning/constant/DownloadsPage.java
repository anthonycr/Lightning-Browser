/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.constant;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.database.downloads.DownloadItem;
import acr.browser.lightning.database.downloads.DownloadsModel;
import acr.browser.lightning.utils.Preconditions;
import acr.browser.lightning.utils.Utils;

public final class DownloadsPage {

    /**
     * The download page standard suffix
     */
    public static final String FILENAME = "downloads.html";

    private static final String HEADING_1 = "<!DOCTYPE html><html xmlns=http://www.w3.org/1999/xhtml>\n" +
        "<head>\n" +
        "<meta content=en-us http-equiv=Content-Language />\n" +
        "<meta content='text/html; charset=utf-8' http-equiv=Content-Type />\n" +
        "<meta name=viewport content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>\n" +
        "<title>";

    private static final String HEADING_2 = "</title>" +
        "</head>" +
        "<style>body{background:#f5f5f5;}.box{vertical-align:middle;position:relative; display: block; margin: 10px;padding:10px; background-color:#fff;box-shadow: 0px 2px 4px rgba( 0, 0, 0, 0.25 );font-family: Arial;color: #444;font-size: 12px;-moz-border-radius: 2px;-webkit-border-radius: 2px;border-radius: 2px;}.box a { width: 100%; height: 100%; position: absolute; left: 0; top: 0;}.black {color: black;font-size: 15px;font-family: Arial; white-space: nowrap; overflow: hidden;margin:auto; text-overflow: ellipsis; -o-text-overflow: ellipsis; -ms-text-overflow: ellipsis;}.font {color: gray;font-size: 10px;font-family: Arial; white-space: nowrap; overflow: hidden;margin:auto; text-overflow: ellipsis; -o-text-overflow: ellipsis; -ms-text-overflow: ellipsis;}</style>" +
        "<body><div id='content'>";

    private static final String PART1 = "<div class=box><a href='";

    private static final String PART2 = "'></a><p class='black'>";

    private static final String PART3 = "</p><p class='font'>";

    private static final String PART4 = "</p></div>";

    private static final String END = "</div></body></html>";

    private File mFilesDir;

    @Inject Application mApp;
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
                mFilesDir = mApp.getFilesDir();

                buildDownloadsPage(null);

                File downloadsWebPage = new File(mFilesDir, FILENAME);

                subscriber.onItem(Constants.FILE + downloadsWebPage);
                subscriber.onComplete();
            }
        });
    }

    private void buildDownloadsPage(@Nullable final String folder) {
        mManager.getAllDownloads()
            .subscribe(new SingleOnSubscribe<List<DownloadItem>>() {
                @Override
                public void onItem(@Nullable List<DownloadItem> list) {
                    Preconditions.checkNonNull(list);

                    final File downloadsWebPage;
                    if (folder == null || folder.isEmpty()) {
                        downloadsWebPage = new File(mFilesDir, FILENAME);
                    } else {
                        downloadsWebPage = new File(mFilesDir, folder + '-' + FILENAME);
                    }
                    final StringBuilder downloadsBuilder = new StringBuilder(HEADING_1 + mTitle + HEADING_2);

                    for (int n = 0, size = list.size(); n < size; n++) {
                        final DownloadItem item = list.get(n);
                        downloadsBuilder.append(PART1);
                        downloadsBuilder.append(item.getUrl());
                        downloadsBuilder.append(PART2);
                        downloadsBuilder.append(item.getTitle());
                        downloadsBuilder.append(PART3);
                        downloadsBuilder.append(item.getContentSize());
                        downloadsBuilder.append(PART4);
                    }
                    downloadsBuilder.append(END);
                    FileWriter bookWriter = null;
                    try {
                        //noinspection IOResourceOpenedButNotSafelyClosed
                        bookWriter = new FileWriter(downloadsWebPage, false);
                        bookWriter.write(downloadsBuilder.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        Utils.close(bookWriter);
                    }
                }
            });
    }

}
