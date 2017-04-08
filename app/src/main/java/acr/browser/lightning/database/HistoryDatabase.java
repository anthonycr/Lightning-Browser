/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import acr.browser.lightning.R;

@SuppressWarnings("unused")
@WorkerThread
@Singleton
public class HistoryDatabase extends SQLiteOpenHelper {

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 2;

    // Database Name
    private static final String DATABASE_NAME = "historyManager";

    // HistoryItems table name
    private static final String TABLE_HISTORY = "history";

    // HistoryItems Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_URL = "url";
    private static final String KEY_TITLE = "title";
    private static final String KEY_TIME_VISITED = "time";

    @Nullable
    private SQLiteDatabase mDatabase;

    @Inject
    public HistoryDatabase(@NonNull Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
        mDatabase = HistoryDatabase.this.getWritableDatabase();
    }

    // Creating Tables
    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        String CREATE_HISTORY_TABLE = "CREATE TABLE " + TABLE_HISTORY + '(' + KEY_ID
                + " INTEGER PRIMARY KEY," + KEY_URL + " TEXT," + KEY_TITLE + " TEXT,"
                + KEY_TIME_VISITED + " INTEGER" + ')';
        db.execSQL(CREATE_HISTORY_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if it exists
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        // Create tables again
        onCreate(db);
    }

    @Override
    public synchronized void close() {
        if (mDatabase != null) {
            mDatabase.close();
            mDatabase = null;
        }
        super.close();
    }

    @WorkerThread
    @NonNull
    private SQLiteDatabase openIfNecessary() {
        if (mDatabase == null || !mDatabase.isOpen()) {
            mDatabase = this.getWritableDatabase();
        }
        return mDatabase;
    }

    @WorkerThread
    public synchronized void deleteHistory() {
        mDatabase = openIfNecessary();
        mDatabase.delete(TABLE_HISTORY, null, null);
        mDatabase.close();
        mDatabase = this.getWritableDatabase();
    }

    @WorkerThread
    public synchronized void deleteHistoryItem(@NonNull String url) {
        mDatabase = openIfNecessary();
        mDatabase.delete(TABLE_HISTORY, KEY_URL + " = ?", new String[]{url});
    }

    @WorkerThread
    public synchronized void visitHistoryItem(@NonNull String url, @Nullable String title) {
        mDatabase = openIfNecessary();
        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, title == null ? "" : title);
        values.put(KEY_TIME_VISITED, System.currentTimeMillis());

        Cursor cursor = mDatabase.query(false, TABLE_HISTORY, new String[]{KEY_URL},
                KEY_URL + " = ?", new String[]{url}, null, null, null, "1");

        if (cursor.getCount() > 0) {
            mDatabase.update(TABLE_HISTORY, values, KEY_URL + " = ?", new String[]{url});
        } else {
            addHistoryItem(new HistoryItem(url, title == null ? "" : title));
        }

        cursor.close();
    }

    @WorkerThread
    private synchronized void addHistoryItem(@NonNull HistoryItem item) {
        mDatabase = openIfNecessary();
        ContentValues values = new ContentValues();
        values.put(KEY_URL, item.getUrl());
        values.put(KEY_TITLE, item.getTitle());
        values.put(KEY_TIME_VISITED, System.currentTimeMillis());
        mDatabase.insert(TABLE_HISTORY, null, values);
    }

    @WorkerThread
    @Nullable
    synchronized String getHistoryItem(@NonNull String url) {
        mDatabase = openIfNecessary();
        Cursor cursor = mDatabase.query(TABLE_HISTORY, new String[]{KEY_ID, KEY_URL, KEY_TITLE},
                KEY_URL + " = ?", new String[]{url}, null, null, null, "1");
        String m = null;
        if (cursor != null) {
            cursor.moveToFirst();
            m = cursor.getString(0);

            cursor.close();
        }
        return m;
    }

    @WorkerThread
    @NonNull
    public synchronized List<HistoryItem> findItemsContaining(@Nullable String search) {
        mDatabase = openIfNecessary();
        List<HistoryItem> itemList = new ArrayList<>(5);
        if (search == null) {
            return itemList;
        }

        search = '%' + search + '%';

        Cursor cursor = mDatabase.query(TABLE_HISTORY, null, KEY_TITLE + " LIKE ? OR " + KEY_URL + " LIKE ?",
                new String[]{search, search}, null, null, KEY_TIME_VISITED + " DESC", "5");

        while (cursor.moveToNext()) {
            HistoryItem item = new HistoryItem();
            item.setUrl(cursor.getString(1));
            item.setTitle(cursor.getString(2));
            item.setImageId(R.drawable.ic_history);
            itemList.add(item);
        }

        cursor.close();

        return itemList;
    }

    @WorkerThread
    @NonNull
    public synchronized List<HistoryItem> getLastHundredItems() {
        mDatabase = openIfNecessary();
        List<HistoryItem> itemList = new ArrayList<>(100);
        Cursor cursor = mDatabase.query(TABLE_HISTORY, null, null, null, null, null, KEY_TIME_VISITED + " DESC", "100");

        while (cursor.moveToNext()) {
            HistoryItem item = new HistoryItem();
            item.setUrl(cursor.getString(1));
            item.setTitle(cursor.getString(2));
            item.setImageId(R.drawable.ic_history);
            itemList.add(item);
        }

        cursor.close();

        return itemList;
    }

    @WorkerThread
    @NonNull
    public synchronized List<HistoryItem> getAllHistoryItems() {
        mDatabase = openIfNecessary();
        List<HistoryItem> itemList = new ArrayList<>();

        Cursor cursor = mDatabase.query(TABLE_HISTORY, null, null, null, null, null, KEY_TIME_VISITED + " DESC");

        while (cursor.moveToNext()) {
            HistoryItem item = new HistoryItem();
            item.setUrl(cursor.getString(1));
            item.setTitle(cursor.getString(2));
            item.setImageId(R.drawable.ic_history);
            itemList.add(item);
        }

        cursor.close();

        return itemList;
    }

    @WorkerThread
    public synchronized long getHistoryItemsCount() {
        return DatabaseUtils.queryNumEntries(mDatabase, TABLE_HISTORY);
    }
}
