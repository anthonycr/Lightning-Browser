/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

import acr.browser.lightning.R;

public class HistoryDatabase extends SQLiteOpenHelper {

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 2;

    // Database Name
    public static final String DATABASE_NAME = "historyManager";

    // HistoryItems table name
    private static final String TABLE_HISTORY = "history";

    // HistoryItems Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_URL = "url";
    private static final String KEY_TITLE = "title";
    private static final String KEY_TIME_VISITED = "time";

    private SQLiteDatabase mDatabase;

    private static HistoryDatabase mInstance;

    public static HistoryDatabase getInstance(Context context) {
        if (mInstance == null || mInstance.isClosed()) {
            mInstance = new HistoryDatabase(context);
        }
        return mInstance;
    }

    private HistoryDatabase(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
        mDatabase = this.getWritableDatabase();
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_HISTORY_TABLE = "CREATE TABLE " + TABLE_HISTORY + '(' + KEY_ID
                + " INTEGER PRIMARY KEY," + KEY_URL + " TEXT," + KEY_TITLE + " TEXT,"
                + KEY_TIME_VISITED + " INTEGER" + ')';
        db.execSQL(CREATE_HISTORY_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if it exists
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        // Create tables again
        onCreate(db);
    }

    public void deleteHistory() {
        mDatabase.delete(TABLE_HISTORY, null, null);
        mDatabase.close();
        mDatabase = this.getWritableDatabase();
    }

    public boolean isClosed() {
        return mDatabase == null || !mDatabase.isOpen();
    }

    @Override
    public synchronized void close() {
        if (mDatabase != null) {
            mDatabase.close();
        }
        super.close();
    }

    public synchronized void deleteHistoryItem(String url) {
        mDatabase.delete(TABLE_HISTORY, KEY_URL + " = ?", new String[] { url });
    }

    public synchronized void visitHistoryItem(String url, String title) {
        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, title);
        values.put(KEY_TIME_VISITED, System.currentTimeMillis());
        Cursor q = mDatabase.query(false, TABLE_HISTORY, new String[] { KEY_URL },
                KEY_URL + " = ?", new String[] { url }, null, null, null, "1");
        if (q.getCount() > 0) {
            mDatabase.update(TABLE_HISTORY, values, KEY_URL + " = ?", new String[] { url });
        } else {
            addHistoryItem(new HistoryItem(url, title));
        }
        q.close();
    }

    private synchronized void addHistoryItem(HistoryItem item) {
        ContentValues values = new ContentValues();
        values.put(KEY_URL, item.getUrl());
        values.put(KEY_TITLE, item.getTitle());
        values.put(KEY_TIME_VISITED, System.currentTimeMillis());
        mDatabase.insert(TABLE_HISTORY, null, values);
    }

    String getHistoryItem(String url) {
        Cursor cursor = mDatabase.query(TABLE_HISTORY, new String[] { KEY_ID, KEY_URL, KEY_TITLE },
                KEY_URL + " = ?", new String[] { url }, null, null, null, null);
        String m = null;
        if (cursor != null) {
            cursor.moveToFirst();
            m = cursor.getString(0);

            cursor.close();
        }
        return m;
    }

    public List<HistoryItem> findItemsContaining(String search) {
        List<HistoryItem> itemList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_HISTORY + " WHERE " + KEY_TITLE + " LIKE '%"
                + search + "%' OR " + KEY_URL + " LIKE '%" + search + "%' " + "ORDER BY "
                + KEY_TIME_VISITED + " DESC LIMIT 5";
        Cursor cursor = mDatabase.rawQuery(selectQuery, null);

        int n = 0;
        if (cursor.moveToFirst()) {
            do {
                HistoryItem item = new HistoryItem();
                item.setID(Integer.parseInt(cursor.getString(0)));
                item.setUrl(cursor.getString(1));
                item.setTitle(cursor.getString(2));
                item.setImageId(R.drawable.ic_history);
                itemList.add(item);
                n++;
            } while (cursor.moveToNext() && n < 5);
        }
        cursor.close();
        return itemList;
    }

    public List<HistoryItem> getLastHundredItems() {
        List<HistoryItem> itemList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_HISTORY + " ORDER BY " + KEY_TIME_VISITED
                + " DESC";

        Cursor cursor = mDatabase.rawQuery(selectQuery, null);
        int counter = 0;
        if (cursor.moveToFirst()) {
            do {
                HistoryItem item = new HistoryItem();
                item.setID(Integer.parseInt(cursor.getString(0)));
                item.setUrl(cursor.getString(1));
                item.setTitle(cursor.getString(2));
                item.setImageId(R.drawable.ic_history);
                itemList.add(item);
                counter++;
            } while (cursor.moveToNext() && counter < 100);
        }
        cursor.close();
        return itemList;
    }

    public List<HistoryItem> getAllHistoryItems() {
        List<HistoryItem> itemList = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + TABLE_HISTORY + " ORDER BY " + KEY_TIME_VISITED
                + " DESC";

        Cursor cursor = mDatabase.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                HistoryItem item = new HistoryItem();
                item.setID(Integer.parseInt(cursor.getString(0)));
                item.setUrl(cursor.getString(1));
                item.setTitle(cursor.getString(2));
                item.setImageId(R.drawable.ic_history);
                itemList.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return itemList;
    }

    public synchronized int updateHistoryItem(HistoryItem item) {

        ContentValues values = new ContentValues();
        values.put(KEY_URL, item.getUrl());
        values.put(KEY_TITLE, item.getTitle());
        values.put(KEY_TIME_VISITED, System.currentTimeMillis());
        return mDatabase.update(TABLE_HISTORY, values, KEY_ID + " = ?",
                new String[] { String.valueOf(item.getId()) });
    }

    public int getHistoryItemsCount() {
        String countQuery = "SELECT * FROM " + TABLE_HISTORY;
        Cursor cursor = mDatabase.rawQuery(countQuery, null);
        int n = cursor.getCount();
        cursor.close();

        return n;
    }
}
