/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.constant;

import android.app.Application;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import acr.browser.lightning.R;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.database.BookmarkManager;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.utils.Utils;
import acr.browser.lightning.view.LightningView;

public final class BookmarkPage extends AsyncTask<Void, Void, Void> {

    /**
     * The bookmark page standard suffix
     */
    public static final String FILENAME = "bookmarks.html";

    private static final String HEADING_1 = "<!DOCTYPE html><html xmlns=http://www.w3.org/1999/xhtml>\n" +
            "<head>\n" +
            "<meta content=en-us http-equiv=Content-Language />\n" +
            "<meta content='text/html; charset=utf-8' http-equiv=Content-Type />\n" +
            "<meta name=viewport content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>\n" +
            "<title>";

    private static final String HEADING_2 = "</title>\n" +
            "</head>\n" +
            "<style>body{background:#e1e1e1;max-width:100%;min-height:100%}#content{width:100%;max-width:800px;margin:0 auto;text-align:center}.box{vertical-align:middle;text-align:center;position:relative;display:inline-block;height:45px;width:150px;margin:10px;background-color:#fff;box-shadow:0 3px 6px rgba(0,0,0,0.25);font-family:Arial;color:#444;font-size:12px;-moz-border-radius:2px;-webkit-border-radius:2px;border-radius:2px}.box-content{height:25px;width:100%;vertical-align:middle;text-align:center;display:table-cell}p.ellipses{" +
            "width:130px;font-size: small;font-family: Arial, Helvetica, 'sans-serif';white-space:nowrap;overflow:hidden;text-align:left;vertical-align:middle;margin:auto;text-overflow:ellipsis;-o-text-overflow:ellipsis;-ms-text-overflow:ellipsis}.box a{width:100%;height:100%;position:absolute;left:0;top:0}img{vertical-align:middle;margin-right:10px;width:20px;height:20px;}.margin{margin:10px}</style>\n" +
            "<body><div id=content>";

    private static final String PART1 = "<div class=box><a href='";

    private static final String PART2 = "'></a>\n" +
            "<div class=margin>\n" +
            "<div class=box-content>\n" +
            "<p class=ellipses>\n" +
            "<img src='";

    private static final String PART3 = "https://www.google.com/s2/favicons?domain=";

    private static final String PART4 = "' />";

    private static final String PART5 = "</p></div></div></div>";

    private static final String END = "</div></body></html>";

    private final File mFilesDir;
    private final File mCacheDir;

    private final BookmarkManager mManager;
    @NonNull private final WeakReference<LightningView> mTabReference;
    private final Bitmap mFolderIcon;
    @NonNull private final String mTitle;

    public BookmarkPage(LightningView tab, @NonNull Application app, BookmarkManager manager, Bitmap folderIcon) {
        mFilesDir = app.getFilesDir();
        mCacheDir = app.getCacheDir();
        mTitle = app.getString(R.string.action_bookmarks);
        mManager = manager;
        mTabReference = new WeakReference<>(tab);
        mFolderIcon = folderIcon;
    }

    @Override
    protected Void doInBackground(Void... params) {
        cacheDefaultFolderIcon();
        buildBookmarkPage(null, mManager);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        LightningView tab = mTabReference.get();
        if (tab != null) {
            File bookmarkWebPage = new File(mFilesDir, FILENAME);
            tab.loadUrl(Constants.FILE + bookmarkWebPage);
        }
    }

    private void cacheDefaultFolderIcon() {
        FileOutputStream outputStream = null;
        File image = new File(mCacheDir, "folder.png");
        try {
            outputStream = new FileOutputStream(image);
            mFolderIcon.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            mFolderIcon.recycle();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            Utils.close(outputStream);
        }
    }

    private void buildBookmarkPage(@Nullable final String folder, @NonNull final BookmarkManager manager) {
        final List<HistoryItem> list = manager.getBookmarksFromFolder(folder, true);
        final File bookmarkWebPage;
        if (folder == null || folder.isEmpty()) {
            bookmarkWebPage = new File(mFilesDir, FILENAME);
        } else {
            bookmarkWebPage = new File(mFilesDir, folder + '-' + FILENAME);
        }
        final StringBuilder bookmarkBuilder = new StringBuilder(HEADING_1 + mTitle + HEADING_2);

        final String folderIconPath = Constants.FILE + mCacheDir + "/folder.png";
        for (int n = 0, size = list.size(); n < size; n++) {
            final HistoryItem item = list.get(n);
            bookmarkBuilder.append(PART1);
            if (item.isFolder()) {
                final File folderPage = new File(mFilesDir, item.getTitle() + '-' + FILENAME);
                bookmarkBuilder.append(Constants.FILE).append(folderPage);
                bookmarkBuilder.append(PART2);
                bookmarkBuilder.append(folderIconPath);
                buildBookmarkPage(item.getTitle(), manager);
            } else {
                bookmarkBuilder.append(item.getUrl());
                bookmarkBuilder.append(PART2).append(PART3);
                bookmarkBuilder.append(item.getUrl());
            }
            bookmarkBuilder.append(PART4);
            bookmarkBuilder.append(item.getTitle());
            bookmarkBuilder.append(PART5);
        }
        bookmarkBuilder.append(END);
        FileWriter bookWriter = null;
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            bookWriter = new FileWriter(bookmarkWebPage, false);
            bookWriter.write(bookmarkBuilder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Utils.close(bookWriter);
        }
    }

    public void load() {
        executeOnExecutor(BrowserApp.getIOThread());
    }

}
