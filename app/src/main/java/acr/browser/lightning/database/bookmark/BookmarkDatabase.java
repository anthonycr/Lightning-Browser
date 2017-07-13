package acr.browser.lightning.database.bookmark;

import android.app.Application;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.anthonycr.bonsai.Completable;
import com.anthonycr.bonsai.CompletableAction;
import com.anthonycr.bonsai.CompletableSubscriber;
import com.anthonycr.bonsai.Single;
import com.anthonycr.bonsai.SingleAction;
import com.anthonycr.bonsai.SingleSubscriber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import acr.browser.lightning.R;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.database.HistoryItem;

/**
 * The disk backed bookmark database.
 * See {@link BookmarkModel} for method
 * documentation.
 * <p>
 * Created by anthonycr on 5/6/17.
 */
@Singleton
public class BookmarkDatabase extends SQLiteOpenHelper implements BookmarkModel {

    private static final String TAG = "BookmarkDatabase";

    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "bookmarkManager";

    // HistoryItems table name
    private static final String TABLE_BOOKMARK = "bookmark";

    // HistoryItems Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_URL = "url";
    private static final String KEY_TITLE = "title";
    private static final String KEY_FOLDER = "folder";
    private static final String KEY_POSITION = "position";

    @NonNull private final String DEFAULT_BOOKMARK_TITLE;

    @Nullable private SQLiteDatabase mDatabase;

    @Inject
    public BookmarkDatabase(@NonNull Application application) {
        super(application, DATABASE_NAME, null, DATABASE_VERSION);
        DEFAULT_BOOKMARK_TITLE = application.getString(R.string.untitled);
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
            DatabaseUtils.sqlEscapeString(TABLE_BOOKMARK) + '(' +
            DatabaseUtils.sqlEscapeString(KEY_ID) + " INTEGER PRIMARY KEY," +
            DatabaseUtils.sqlEscapeString(KEY_URL) + " TEXT," +
            DatabaseUtils.sqlEscapeString(KEY_TITLE) + " TEXT," +
            DatabaseUtils.sqlEscapeString(KEY_FOLDER) + " TEXT," +
            DatabaseUtils.sqlEscapeString(KEY_POSITION) + " INTEGER" + ')';
        db.execSQL(CREATE_BOOKMARK_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if it exists
        db.execSQL("DROP TABLE IF EXISTS " + DatabaseUtils.sqlEscapeString(TABLE_BOOKMARK));
        // Create tables again
        onCreate(db);
    }

    /**
     * Binds a {@link HistoryItem} to {@link ContentValues}.
     *
     * @param bookmarkItem the bookmark to bind.
     * @return a valid values object that can be inserted
     * into the database.
     */
    @NonNull
    private static ContentValues bindBookmarkToContentValues(@NonNull HistoryItem bookmarkItem) {
        ContentValues contentValues = new ContentValues(4);
        contentValues.put(KEY_TITLE, bookmarkItem.getTitle());
        contentValues.put(KEY_URL, bookmarkItem.getUrl());
        contentValues.put(KEY_FOLDER, bookmarkItem.getFolder());
        contentValues.put(KEY_POSITION, bookmarkItem.getPosition());

        return contentValues;
    }

    /**
     * Binds a cursor to a {@link HistoryItem}. This is
     * a non consuming operation on the cursor. Note that
     * this operation is not safe to perform on a cursor
     * unless you know that the cursor is of history items.
     *
     * @param cursor the cursor to read from.
     * @return a valid item containing all the pertinent information.
     */
    @NonNull
    private static HistoryItem bindCursorToHistoryItem(@NonNull Cursor cursor) {
        HistoryItem bookmark = new HistoryItem();

        bookmark.setImageId(R.drawable.ic_bookmark);
        bookmark.setUrl(cursor.getString(cursor.getColumnIndex(KEY_URL)));
        bookmark.setTitle(cursor.getString(cursor.getColumnIndex(KEY_TITLE)));
        bookmark.setFolder(cursor.getString(cursor.getColumnIndex(KEY_FOLDER)));
        bookmark.setPosition(cursor.getInt(cursor.getColumnIndex(KEY_POSITION)));

        return bookmark;
    }

    /**
     * Binds a cursor to a list of {@link HistoryItem}.
     * This operation consumes the cursor.
     *
     * @param cursor the cursor to bind.
     * @return a valid list of history items, may be empty.
     */
    @NonNull
    private static List<HistoryItem> bindCursorToHistoryItemList(@NonNull Cursor cursor) {
        List<HistoryItem> bookmarks = new ArrayList<>();

        while (cursor.moveToNext()) {
            bookmarks.add(bindCursorToHistoryItem(cursor));
        }

        cursor.close();

        return bookmarks;
    }

    /**
     * URLs can represent the same thing with or without a trailing slash,
     * for instance, google.com/ is the same page as google.com. Since these
     * can be represented as different bookmarks within the bookmark database,
     * it is important to be able to get the alternate version of a URL.
     *
     * @param url the string that might have a trailing slash.
     * @return a string without a trailing slash if the original had one,
     * or a string with a trailing slash if the original did not.
     */
    @NonNull
    private static String alternateSlashUrl(@NonNull String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        } else {
            return url + '/';
        }
    }

    /**
     * Queries the database for bookmarks with the provided URL. If it
     * cannot find any bookmarks with the given URL, it will try to query
     * for bookmarks with the {@link #alternateSlashUrl(String)} as its URL.
     *
     * @param url the URL to query for.
     * @return a cursor with bookmarks matching the URL.
     */
    @NonNull
    private Cursor queryWithOptionalEndSlash(@NonNull String url) {
        Cursor cursor = lazyDatabase().query(TABLE_BOOKMARK, null, KEY_URL + "=?", new String[]{url}, null, null, null, "1");

        if (cursor.getCount() == 0) {
            cursor.close();

            String alternateUrl = alternateSlashUrl(url);
            cursor = lazyDatabase().query(TABLE_BOOKMARK, null, KEY_URL + "=?", new String[]{alternateUrl}, null, null, null, "1");
        }

        return cursor;
    }

    /**
     * Deletes a bookmark from the database with the provided URL. If it
     * cannot find any bookmark with the given URL, it will try to delete
     * a bookmark with the {@link #alternateSlashUrl(String)} as its URL.
     *
     * @param url the URL to delete.
     * @return the number of deleted rows.
     */
    private int deleteWithOptionalEndSlash(@NonNull String url) {
        int deletedRows = lazyDatabase().delete(TABLE_BOOKMARK, KEY_URL + "=?", new String[]{url});

        if (deletedRows == 0) {
            String alternateUrl = alternateSlashUrl(url);
            deletedRows = lazyDatabase().delete(TABLE_BOOKMARK, KEY_URL + "=?", new String[]{alternateUrl});
        }

        return deletedRows;
    }

    /**
     * Updates a bookmark in the database with the provided URL. If it
     * cannot find any bookmark with the given URL, it will try to update
     * a bookmark with the {@link #alternateSlashUrl(String)} as its URL.
     *
     * @param url           the URL to update.
     * @param contentValues the new values to update to.
     * @return the numebr of rows updated.
     */
    private int updateWithOptionalEndSlash(@NonNull String url, @NonNull ContentValues contentValues) {
        int updatedRows = lazyDatabase().update(TABLE_BOOKMARK, contentValues, KEY_URL + "=?", new String[]{url});

        if (updatedRows == 0) {
            String alternateUrl = alternateSlashUrl(url);
            updatedRows = lazyDatabase().update(TABLE_BOOKMARK, contentValues, KEY_URL + "=?", new String[]{alternateUrl});
        }

        return updatedRows;
    }

    @NonNull
    @Override
    public Single<HistoryItem> findBookmarkForUrl(@NonNull final String url) {
        return Single.create(new SingleAction<HistoryItem>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<HistoryItem> subscriber) {
                Cursor cursor = queryWithOptionalEndSlash(url);

                if (cursor.moveToFirst()) {
                    subscriber.onItem(bindCursorToHistoryItem(cursor));
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
    public Single<Boolean> isBookmark(@NonNull final String url) {
        return Single.create(new SingleAction<Boolean>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<Boolean> subscriber) {
                Cursor cursor = queryWithOptionalEndSlash(url);

                subscriber.onItem(cursor.moveToFirst());

                cursor.close();
                subscriber.onComplete();
            }
        });
    }

    @NonNull
    @Override
    public Single<Boolean> addBookmarkIfNotExists(@NonNull final HistoryItem item) {
        return Single.create(new SingleAction<Boolean>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<Boolean> subscriber) {
                Cursor cursor = queryWithOptionalEndSlash(item.getUrl());

                if (cursor.moveToFirst()) {
                    cursor.close();
                    subscriber.onItem(false);
                    subscriber.onComplete();
                    return;
                }

                cursor.close();

                long id = lazyDatabase().insert(TABLE_BOOKMARK, null, bindBookmarkToContentValues(item));

                subscriber.onItem(id != -1);
                subscriber.onComplete();
            }
        });
    }

    @NonNull
    @Override
    public Completable addBookmarkList(@NonNull final List<HistoryItem> bookmarkItems) {
        return Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                lazyDatabase().beginTransaction();

                for (HistoryItem item : bookmarkItems) {
                    addBookmarkIfNotExists(item).subscribe();
                }

                lazyDatabase().setTransactionSuccessful();
                lazyDatabase().endTransaction();

                subscriber.onComplete();
            }
        });
    }

    @NonNull
    @Override
    public Single<Boolean> deleteBookmark(@NonNull final HistoryItem bookmark) {
        return Single.create(new SingleAction<Boolean>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<Boolean> subscriber) {
                int rows = deleteWithOptionalEndSlash(bookmark.getUrl());

                subscriber.onItem(rows > 0);
                subscriber.onComplete();
            }
        });
    }

    @NonNull
    @Override
    public Completable renameFolder(@NonNull final String oldName, @NonNull final String newName) {
        return Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                ContentValues contentValues = new ContentValues(1);
                contentValues.put(KEY_FOLDER, newName);

                lazyDatabase().update(TABLE_BOOKMARK, contentValues, KEY_FOLDER + "=?", new String[]{oldName});

                subscriber.onComplete();
            }
        });
    }

    @NonNull
    @Override
    public Completable deleteFolder(@NonNull final String folderToDelete) {
        return Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                renameFolder(folderToDelete, "").subscribe();

                subscriber.onComplete();
            }
        });
    }

    @NonNull
    @Override
    public Completable deleteAllBookmarks() {
        return Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                lazyDatabase().delete(TABLE_BOOKMARK, null, null);

                subscriber.onComplete();
            }
        });
    }

    @NonNull
    @Override
    public Completable editBookmark(@NonNull final HistoryItem oldBookmark, @NonNull final HistoryItem newBookmark) {
        return Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                if (newBookmark.getTitle().isEmpty()) {
                    newBookmark.setTitle(DEFAULT_BOOKMARK_TITLE);
                }
                ContentValues contentValues = bindBookmarkToContentValues(newBookmark);

                updateWithOptionalEndSlash(oldBookmark.getUrl(), contentValues);

                subscriber.onComplete();
            }
        });
    }

    @NonNull
    @Override
    public Single<List<HistoryItem>> getAllBookmarks() {
        return Single.create(new SingleAction<List<HistoryItem>>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<List<HistoryItem>> subscriber) {
                Cursor cursor = lazyDatabase().query(TABLE_BOOKMARK, null, null, null, null, null, null);

                subscriber.onItem(bindCursorToHistoryItemList(cursor));
                subscriber.onComplete();

                cursor.close();
            }
        });
    }

    @NonNull
    @Override
    public Single<List<HistoryItem>> getBookmarksFromFolderSorted(@Nullable final String folder) {
        return Single.create(new SingleAction<List<HistoryItem>>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<List<HistoryItem>> subscriber) {
                String finalFolder = folder != null ? folder : "";
                Cursor cursor = lazyDatabase().query(TABLE_BOOKMARK, null, KEY_FOLDER + "=?", new String[]{finalFolder}, null, null, null);

                List<HistoryItem> list = bindCursorToHistoryItemList(cursor);
                Collections.sort(list);
                subscriber.onItem(list);
                subscriber.onComplete();

                cursor.close();
            }
        });
    }

    @NonNull
    @Override
    public Single<List<HistoryItem>> getFoldersSorted() {
        return Single.create(new SingleAction<List<HistoryItem>>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<List<HistoryItem>> subscriber) {
                Cursor cursor = lazyDatabase().query(true, TABLE_BOOKMARK, new String[]{KEY_FOLDER}, null, null, null, null, null, null);

                List<HistoryItem> folders = new ArrayList<>();
                while (cursor.moveToNext()) {
                    String folderName = cursor.getString(cursor.getColumnIndex(KEY_FOLDER));
                    if (TextUtils.isEmpty(folderName)) {
                        continue;
                    }

                    final HistoryItem folder = new HistoryItem();
                    folder.setIsFolder(true);
                    folder.setTitle(folderName);
                    folder.setImageId(R.drawable.ic_folder);
                    folder.setUrl(Constants.FOLDER + folderName);

                    folders.add(folder);
                }

                cursor.close();

                Collections.sort(folders);
                subscriber.onItem(folders);
                subscriber.onComplete();
            }
        });
    }

    @NonNull
    @Override
    public Single<List<String>> getFolderNames() {
        return Single.create(new SingleAction<List<String>>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<List<String>> subscriber) {
                Cursor cursor = lazyDatabase().query(true, TABLE_BOOKMARK, new String[]{KEY_FOLDER}, null, null, null, null, null, null);

                List<String> folders = new ArrayList<>();
                while (cursor.moveToNext()) {
                    String folderName = cursor.getString(cursor.getColumnIndex(KEY_FOLDER));
                    if (TextUtils.isEmpty(folderName)) {
                        continue;
                    }

                    folders.add(folderName);
                }

                cursor.close();

                subscriber.onItem(folders);
                subscriber.onComplete();
            }
        });
    }

    @Override
    public long count() {
        return DatabaseUtils.queryNumEntries(lazyDatabase(), TABLE_BOOKMARK);
    }

}
