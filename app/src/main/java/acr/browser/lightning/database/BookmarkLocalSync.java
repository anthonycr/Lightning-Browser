package acr.browser.lightning.database;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import acr.browser.lightning.utils.Utils;

public class BookmarkLocalSync {

    private static final String TAG = BookmarkLocalSync.class.getSimpleName();

    private static final String STOCK_BOOKMARKS_CONTENT = "content://browser/bookmarks";
    private static final String CHROME_BOOKMARKS_CONTENT = "content://com.android.chrome.browser/bookmarks";
    private static final String CHROME_BETA_BOOKMARKS_CONTENT = "content://com.chrome.beta.browser/bookmarks";
    private static final String CHROME_DEV_BOOKMARKS_CONTENT = "content://com.chrome.dev.browser/bookmarks";

    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_URL = "url";
    private static final String COLUMN_BOOKMARK = "bookmark";

    @NonNull private final Context mContext;

    public BookmarkLocalSync(@NonNull Context context) {
        mContext = context;
    }

    @NonNull
    @WorkerThread
    public List<HistoryItem> getBookmarksFromStockBrowser() {
        List<HistoryItem> list = new ArrayList<>();
        if (!isStockSupported()) {
            return list;
        }
        Cursor cursor = getStockCursor();
        try {
            if (cursor != null) {
                for (int n = 0; n < cursor.getColumnCount(); n++) {
                    Log.d(TAG, cursor.getColumnName(n));
                }

                while (cursor.moveToNext()) {
                    if (cursor.getInt(2) == 1) {
                        String url = cursor.getString(0);
                        String title = cursor.getString(1);
                        if (url.isEmpty()) {
                            continue;
                        }
                        if (title == null || title.isEmpty()) {
                            title = Utils.getDomainName(url);
                        }
                        list.add(new HistoryItem(url, title));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Utils.close(cursor);
        return list;
    }

    @NonNull
    @WorkerThread
    public List<HistoryItem> getBookmarksFromChrome() {
        List<HistoryItem> list = new ArrayList<>();
        if (!isChromeSupported()) {
            return list;
        }
        Cursor cursor = getChromeCursor();
        try {
            if (cursor != null) {
                for (int n = 0; n < cursor.getColumnCount(); n++) {
                    Log.d(TAG, cursor.getColumnName(n));
                }

                while (cursor.moveToNext()) {
                    if (cursor.getInt(2) == 1) {
                        String url = cursor.getString(0);
                        String title = cursor.getString(1);
                        if (url.isEmpty()) {
                            continue;
                        }
                        if (title == null || title.isEmpty()) {
                            title = Utils.getDomainName(url);
                        }
                        list.add(new HistoryItem(url, title));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Utils.close(cursor);
        return list;
    }

    @WorkerThread
    public boolean isStockSupported() {
        Cursor cursor = getStockCursor();
        Utils.close(cursor);
        return cursor != null;
    }

    @WorkerThread
    public boolean isChromeSupported() {
        Cursor cursor = getChromeCursor();
        Utils.close(cursor);
        return cursor != null;
    }

    @Nullable
    @WorkerThread
    private Cursor getChromeCursor() {
        Cursor cursor;
        Uri uri = Uri.parse(CHROME_BOOKMARKS_CONTENT);
        try {
            cursor = mContext.getContentResolver().query(uri,
                    new String[]{COLUMN_URL, COLUMN_TITLE, COLUMN_BOOKMARK}, null, null, null);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return cursor;
    }

    @Nullable
    @WorkerThread
    private Cursor getStockCursor() {
        Cursor cursor;
        Uri uri = Uri.parse(STOCK_BOOKMARKS_CONTENT);
        try {
            cursor = mContext.getContentResolver().query(uri,
                    new String[]{COLUMN_URL, COLUMN_TITLE, COLUMN_BOOKMARK}, null, null, null);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return cursor;
    }

    public void printAllColumns(){
        printColumns(CHROME_BETA_BOOKMARKS_CONTENT);
        printColumns(CHROME_BOOKMARKS_CONTENT);
        printColumns(CHROME_DEV_BOOKMARKS_CONTENT);
        printColumns(STOCK_BOOKMARKS_CONTENT);
    }

    public void printColumns(String contentProvider) {
        Cursor cursor = null;
        Log.e(TAG, contentProvider);
        Uri uri = Uri.parse(contentProvider);
        try {
            cursor = mContext.getContentResolver().query(uri, null, null, null, null);
        } catch (Exception e) {
            Log.e(TAG, "Error Occurred", e);
        }
        if (cursor != null) {
            for (int n = 0; n < cursor.getColumnCount(); n++) {
                Log.d(TAG, cursor.getColumnName(n));
            }
            cursor.close();
        }
    }

}
