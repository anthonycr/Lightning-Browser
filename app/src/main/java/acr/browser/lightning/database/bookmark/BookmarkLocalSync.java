package acr.browser.lightning.database.bookmark;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.anthonycr.bonsai.Single;
import com.anthonycr.bonsai.SingleAction;
import com.anthonycr.bonsai.SingleSubscriber;

import java.util.ArrayList;
import java.util.List;

import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.utils.Utils;

public class BookmarkLocalSync {

    private static final String TAG = "BookmarkLocalSync";

    private static final String STOCK_BOOKMARKS_CONTENT = "content://browser/bookmarks";
    private static final String CHROME_BOOKMARKS_CONTENT = "content://com.android.chrome.browser/bookmarks";
    private static final String CHROME_BETA_BOOKMARKS_CONTENT = "content://com.chrome.beta.browser/bookmarks";
    private static final String CHROME_DEV_BOOKMARKS_CONTENT = "content://com.chrome.dev.browser/bookmarks";

    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_URL = "url";
    private static final String COLUMN_BOOKMARK = "bookmark";

    @NonNull private final Context mContext;

    public enum Source {
        STOCK,
        CHROME_STABLE,
        CHROME_BETA,
        CHROME_DEV
    }

    public BookmarkLocalSync(@NonNull Context context) {
        mContext = context;
    }

    @NonNull
    private List<HistoryItem> getBookmarksFromContentUri(String contentUri) {
        List<HistoryItem> list = new ArrayList<>();
        Cursor cursor = getBrowserCursor(contentUri);
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
                        if (title != null) {
                            list.add(new HistoryItem(url, title));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Utils.close(cursor);
        return list;
    }

    @Nullable
    @WorkerThread
    private Cursor getBrowserCursor(String contentUri) {
        Cursor cursor;
        Uri uri = Uri.parse(contentUri);
        try {
            cursor = mContext.getContentResolver().query(uri,
                new String[]{COLUMN_URL, COLUMN_TITLE, COLUMN_BOOKMARK}, null, null, null);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return cursor;
    }

    @NonNull
    public Single<List<Source>> getSupportedBrowsers() {
        return Single.create(new SingleAction<List<Source>>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<List<Source>> subscriber) {
                List<Source> sources = new ArrayList<>(1);
                if (isBrowserSupported(STOCK_BOOKMARKS_CONTENT)) {
                    sources.add(Source.STOCK);
                }
                if (isBrowserSupported(CHROME_BOOKMARKS_CONTENT)) {
                    sources.add(Source.CHROME_STABLE);
                }
                if (isBrowserSupported(CHROME_BETA_BOOKMARKS_CONTENT)) {
                    sources.add(Source.CHROME_BETA);
                }
                if (isBrowserSupported(CHROME_DEV_BOOKMARKS_CONTENT)) {
                    sources.add(Source.CHROME_DEV);
                }
                subscriber.onItem(sources);
                subscriber.onComplete();
            }
        });
    }

    private boolean isBrowserSupported(String contentUri) {
        Cursor cursor = getBrowserCursor(contentUri);
        boolean supported = cursor != null;
        Utils.close(cursor);
        return supported;
    }

    @NonNull
    @WorkerThread
    public List<HistoryItem> getBookmarksFromStockBrowser() {
        return getBookmarksFromContentUri(STOCK_BOOKMARKS_CONTENT);
    }

    @NonNull
    @WorkerThread
    public List<HistoryItem> getBookmarksFromChrome() {
        return getBookmarksFromContentUri(CHROME_BOOKMARKS_CONTENT);
    }

    @NonNull
    @WorkerThread
    public List<HistoryItem> getBookmarksFromChromeBeta() {
        return getBookmarksFromContentUri(CHROME_BETA_BOOKMARKS_CONTENT);
    }

    @NonNull
    @WorkerThread
    public List<HistoryItem> getBookmarksFromChromeDev() {
        return getBookmarksFromContentUri(CHROME_DEV_BOOKMARKS_CONTENT);
    }

    @NonNull
    public Single<Boolean> isBrowserImportSupported() {
        return Single.create(new SingleAction<Boolean>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<Boolean> subscriber) {
                Cursor chrome = getChromeCursor();
                Utils.close(chrome);
                Cursor dev = getChromeDevCursor();
                Utils.close(dev);
                Cursor beta = getChromeBetaCursor();
                Cursor stock = getStockCursor();
                Utils.close(stock);

                subscriber.onItem(chrome != null || dev != null || beta != null || stock != null);
                subscriber.onComplete();
            }
        });
    }

    @Nullable
    @WorkerThread
    private Cursor getChromeBetaCursor() {
        return getBrowserCursor(CHROME_BETA_BOOKMARKS_CONTENT);
    }

    @Nullable
    @WorkerThread
    private Cursor getChromeDevCursor() {
        return getBrowserCursor(CHROME_DEV_BOOKMARKS_CONTENT);
    }

    @Nullable
    @WorkerThread
    private Cursor getChromeCursor() {
        return getBrowserCursor(CHROME_BOOKMARKS_CONTENT);
    }

    @Nullable
    @WorkerThread
    private Cursor getStockCursor() {
        return getBrowserCursor(STOCK_BOOKMARKS_CONTENT);
    }

    public void printAllColumns() {
        printColumns(CHROME_BETA_BOOKMARKS_CONTENT);
        printColumns(CHROME_BOOKMARKS_CONTENT);
        printColumns(CHROME_DEV_BOOKMARKS_CONTENT);
        printColumns(STOCK_BOOKMARKS_CONTENT);
    }

    private void printColumns(String contentProvider) {
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
