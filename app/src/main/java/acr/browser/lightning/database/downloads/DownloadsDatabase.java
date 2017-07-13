package acr.browser.lightning.database.downloads;

import android.app.Application;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import com.anthonycr.bonsai.Completable;
import com.anthonycr.bonsai.CompletableAction;
import com.anthonycr.bonsai.CompletableSubscriber;
import com.anthonycr.bonsai.Single;
import com.anthonycr.bonsai.SingleAction;
import com.anthonycr.bonsai.SingleSubscriber;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import acr.browser.lightning.R;

/**
 * The disk backed download database.
 * See {@link DownloadsModel} for method
 * documentation.
 */
@Singleton
public class DownloadsDatabase extends SQLiteOpenHelper implements DownloadsModel {

    private static final String TAG = "DownloadsDatabase";

    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "downloadManager";

    // HistoryItems table name
    private static final String TABLE_DOWNLOADS = "download";

    // HistoryItems Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_URL = "url";
    private static final String KEY_TITLE = "title";
    private static final String KEY_SIZE = "size";

    @NonNull private final String DEFAULT_DOWNLOADS_TITLE;

    @Nullable private SQLiteDatabase mDatabase;

    @Inject
    public DownloadsDatabase(@NonNull Application application) {
        super(application, DATABASE_NAME, null, DATABASE_VERSION);
        DEFAULT_DOWNLOADS_TITLE = application.getString(R.string.untitled);
    }

    /**
     * Lazily initializes the database
     * field when called.
     *
     * @return a non null writable database.
     */
    @WorkerThread
    @NonNull
    private synchronized SQLiteDatabase lazyDatabase() {
        if (mDatabase == null || !mDatabase.isOpen()) {
            mDatabase = getWritableDatabase();
        }

        return mDatabase;
    }

    // Creating Tables
    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        String CREATE_BOOKMARK_TABLE = "CREATE TABLE " +
                DatabaseUtils.sqlEscapeString(TABLE_DOWNLOADS) + '(' +
                DatabaseUtils.sqlEscapeString(KEY_ID) + " INTEGER PRIMARY KEY," +
                DatabaseUtils.sqlEscapeString(KEY_URL) + " TEXT," +
                DatabaseUtils.sqlEscapeString(KEY_TITLE) + " TEXT," +
                DatabaseUtils.sqlEscapeString(KEY_SIZE) + " TEXT" + ')';
        db.execSQL(CREATE_BOOKMARK_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if it exists
        db.execSQL("DROP TABLE IF EXISTS " + DatabaseUtils.sqlEscapeString(TABLE_DOWNLOADS));
        // Create tables again
        onCreate(db);
    }

    @NonNull
    private static ContentValues bindBookmarkToContentValues(@NonNull DownloadItem downloadItem) {
        ContentValues contentValues = new ContentValues(3);
        contentValues.put(KEY_TITLE, downloadItem.getTitle());
        contentValues.put(KEY_URL, downloadItem.getUrl());
        contentValues.put(KEY_SIZE, downloadItem.getContentSize());

        return contentValues;
    }

    @NonNull
    private static DownloadItem bindCursorToDownloadItem(@NonNull Cursor cursor) {
        DownloadItem download = new DownloadItem();

        download.setUrl(cursor.getString(cursor.getColumnIndex(KEY_URL)));
        download.setTitle(cursor.getString(cursor.getColumnIndex(KEY_TITLE)));
        download.setContentSize(cursor.getString(cursor.getColumnIndex(KEY_SIZE)));

        return download;
    }

    @NonNull
    private static List<DownloadItem> bindCursorToDownloadItemList(@NonNull Cursor cursor) {
        List<DownloadItem> downloads = new ArrayList<>();

        while (cursor.moveToNext()) {
            downloads.add(bindCursorToDownloadItem(cursor));
        }

        cursor.close();

        return downloads;
    }

    @NonNull
    @Override
    public Single<DownloadItem> findDownloadForUrl(@NonNull final String url) {
        return Single.create(new SingleAction<DownloadItem>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<DownloadItem> subscriber) {
                Cursor cursor = lazyDatabase().query(TABLE_DOWNLOADS, null, KEY_URL + "=?", new String[]{url}, null, null, "1");

                if (cursor.moveToFirst()) {
                    subscriber.onItem(bindCursorToDownloadItem(cursor));
                } else {
                    subscriber.onItem(null);
                }

                cursor.close();
                subscriber.onComplete();
            }
        });
    }

    @NonNull
    @Override
    public Single<Boolean> isDownload(@NonNull final String url) {
        return Single.create(new SingleAction<Boolean>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<Boolean> subscriber) {
                Cursor cursor = lazyDatabase().query(TABLE_DOWNLOADS, null, KEY_URL + "=?", new String[]{url}, null, null, null, "1");

                subscriber.onItem(cursor.moveToFirst());

                cursor.close();
                subscriber.onComplete();
            }
        });
    }

    @NonNull
    @Override
    public Single<Boolean> addDownloadIfNotExists(@NonNull final DownloadItem item) {
        return Single.create(new SingleAction<Boolean>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<Boolean> subscriber) {
                Cursor cursor = lazyDatabase().query(TABLE_DOWNLOADS, null, KEY_URL + "=?", new String[]{item.getUrl()}, null, null, "1");

                if (cursor.moveToFirst()) {
                    cursor.close();
                    subscriber.onItem(false);
                    subscriber.onComplete();
                    return;
                }

                cursor.close();

                long id = lazyDatabase().insert(TABLE_DOWNLOADS, null, bindBookmarkToContentValues(item));

                subscriber.onItem(id != -1);
                subscriber.onComplete();
            }
        });
    }

    @NonNull
    @Override
    public Completable addDownloadsList(@NonNull final List<DownloadItem> bookmarkItems) {
        return Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                lazyDatabase().beginTransaction();

                for (DownloadItem item : bookmarkItems) {
                    addDownloadIfNotExists(item).subscribe();
                }

                lazyDatabase().setTransactionSuccessful();
                lazyDatabase().endTransaction();

                subscriber.onComplete();
            }
        });
    }

    @NonNull
    @Override
    public Single<Boolean> deleteDownload(@NonNull final String url) {
        return Single.create(new SingleAction<Boolean>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<Boolean> subscriber) {
                int rows = lazyDatabase().delete(TABLE_DOWNLOADS, KEY_URL + "=?", new String[]{url});

                subscriber.onItem(rows > 0);
                subscriber.onComplete();
            }
        });
    }

    @NonNull
    @Override
    public Completable deleteAllDownloads() {
        return Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                lazyDatabase().delete(TABLE_DOWNLOADS, null, null);

                subscriber.onComplete();
            }
        });
    }

    @NonNull
    @Override
    public Single<List<DownloadItem>> getAllDownloads() {
        return Single.create(new SingleAction<List<DownloadItem>>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<List<DownloadItem>> subscriber) {
                Cursor cursor = lazyDatabase().query(TABLE_DOWNLOADS, null, null, null, null, null, null);

                subscriber.onItem(bindCursorToDownloadItemList(cursor));
                subscriber.onComplete();

                cursor.close();
            }
        });
    }

    @Override
    public long count() {
        return DatabaseUtils.queryNumEntries(lazyDatabase(), TABLE_DOWNLOADS);
    }

}
