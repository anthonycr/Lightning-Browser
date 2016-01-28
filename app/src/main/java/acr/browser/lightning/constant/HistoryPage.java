/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.constant;

import android.app.Application;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;

import acr.browser.lightning.R;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.database.HistoryDatabase;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.utils.Utils;
import acr.browser.lightning.view.LightningView;

public class HistoryPage extends AsyncTask<Void, Void, Void> {

    public static final String FILENAME = "history.html";

    private static final String HEADING_1 = "<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta content=\"en-us\" http-equiv=\"Content-Language\" /><meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\"><title>";

    private static final String HEADING_2 = "</title></head><style>body { background: #e1e1e1;}.box { vertical-align:middle;position:relative; display: block; margin: 10px;padding-left:10px;padding-right:10px;padding-top:5px;padding-bottom:5px; background-color:#fff;box-shadow: 0px 2px 3px rgba( 0, 0, 0, 0.25 );font-family: Arial;color: #444;font-size: 12px;-moz-border-radius: 2px;-webkit-border-radius: 2px;border-radius: 2px;}.box a { width: 100%; height: 100%; position: absolute; left: 0; top: 0;}.black {color: black;font-size: 15px;font-family: Arial; white-space: nowrap; overflow: hidden;margin:auto; text-overflow: ellipsis; -o-text-overflow: ellipsis; -ms-text-overflow: ellipsis;}.font {color: gray;font-size: 10px;font-family: Arial; white-space: nowrap; overflow: hidden;margin:auto; text-overflow: ellipsis; -o-text-overflow: ellipsis; -ms-text-overflow: ellipsis;}</style><body><div id=\"content\">";

    private static final String PART1 = "<div class=\"box\"><a href=\"";

    private static final String PART2 = "\"></a><p class=\"black\">";

    private static final String PART3 = "</p><p class=\"font\">";

    private static final String PART4 = "</p></div></div>";

    private static final String END = "</div></body></html>";

    @NonNull private final WeakReference<LightningView> mTabReference;
    private final File mFilesDir;
    @NonNull private final String mTitle;
    private final HistoryDatabase mHistoryDatabase;

    @Nullable private String mHistoryUrl = null;

    public HistoryPage(LightningView tab, @NonNull Application app, HistoryDatabase database) {
        mTabReference = new WeakReference<>(tab);
        mFilesDir = app.getFilesDir();
        mTitle = app.getString(R.string.action_history);
        mHistoryDatabase = database;
    }

    @Nullable
    @Override
    protected Void doInBackground(Void... params) {
        mHistoryUrl = getHistoryPage();
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        LightningView tab = mTabReference.get();
        if (tab != null && mHistoryUrl != null) {
            tab.loadUrl(mHistoryUrl);
        }
    }

    @NonNull
    private String getHistoryPage() {
        StringBuilder historyBuilder = new StringBuilder(HEADING_1 + mTitle + HEADING_2);
        List<HistoryItem> historyList = mHistoryDatabase.getLastHundredItems();
        Iterator<HistoryItem> it = historyList.iterator();
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
        File historyWebPage = new File(mFilesDir, FILENAME);
        FileWriter historyWriter = null;
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            historyWriter = new FileWriter(historyWebPage, false);
            historyWriter.write(historyBuilder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Utils.close(historyWriter);
        }
        return Constants.FILE + historyWebPage;
    }

    public void load() {
        executeOnExecutor(BrowserApp.getIOThread());
    }

}