/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler extends SQLiteOpenHelper {

	// All Static variables
	// Database Version
	private static final int DATABASE_VERSION = 1;

	// Database Name
	public static final String DATABASE_NAME = "historyManager";

	// HistoryItems table name
	public static final String TABLE_HISTORY = "history";

	// HistoryItems Table Columns names
	public static final String KEY_ID = "id";

	public static final String KEY_URL = "url";

	public static final String KEY_TITLE = "title";

	public static SQLiteDatabase mDatabase;

	public DatabaseHandler(Context context) {
		super(context.getApplicationContext(), DATABASE_NAME, null,
				DATABASE_VERSION);
		mDatabase = this.getWritableDatabase();
	}

	// Creating Tables
	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_HISTORY_TABLE = "CREATE TABLE " + TABLE_HISTORY + "("
				+ KEY_ID + " INTEGER PRIMARY KEY," + KEY_URL + " TEXT,"
				+ KEY_TITLE + " TEXT" + ")";
		db.execSQL(CREATE_HISTORY_TABLE);
	}

	// Upgrading database
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop older table if existed
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);

		// Create tables again
		onCreate(db);
	}

	public boolean isOpen() {
		if (mDatabase != null) {
			return mDatabase.isOpen();
		} else {
			return false;
		}
	}

	@Override
	public synchronized void close() {
		if (mDatabase != null) {
			mDatabase.close();
		}
		super.close();
	}

	/**
	 * All CRUD(Create, Read, Update, Delete) Operations
	 */

	public synchronized void delete(String url) {
		String n = getHistoryItem(url);
		if (n != null) {
			deleteHistoryItem(n);
		}
	}

	// Adding new item
	public synchronized void addHistoryItem(HistoryItem item) {
		ContentValues values = new ContentValues();
		values.put(KEY_URL, item.getUrl()); // HistoryItem Name
		values.put(KEY_TITLE, item.getTitle()); // HistoryItem Phone
		// Inserting Row
		mDatabase.insert(TABLE_HISTORY, null, values);
	}

	// Getting single item
	String getHistoryItem(String url) {
		Cursor cursor = mDatabase.query(TABLE_HISTORY, new String[]{KEY_ID, KEY_URL,
						KEY_TITLE}, KEY_URL + "=?", new String[]{url}, null, null,
				null, null);
		String m = null;
		if (cursor != null) {
			cursor.moveToFirst();
			m = cursor.getString(0);

			cursor.close();
		}
		// return item
		return m;
	}

	public List<HistoryItem> findItemsContaining(String search) {
		List<HistoryItem> itemList = new ArrayList<HistoryItem>();
		//select query
		String selectQuery = "SELECT * FROM " + TABLE_HISTORY + " WHERE " + KEY_TITLE + " LIKE '%" + search + "%'";
		Cursor cursor = mDatabase.rawQuery(selectQuery, null);

		// looping through all rows and adding to list
		if (cursor.moveToLast()) {
			do {
				HistoryItem item = new HistoryItem();
				item.setID(Integer.parseInt(cursor.getString(0)));
				item.setUrl(cursor.getString(1));
				item.setTitle(cursor.getString(2));
				item.setImageId(R.drawable.ic_history);
				// Adding item to list
				itemList.add(item);
			} while (cursor.moveToPrevious());
		}
		cursor.close();
		// return item list
		return itemList;
	}

	public List<HistoryItem> getLastHundredItems() {
		List<HistoryItem> itemList = new ArrayList<HistoryItem>();
		String selectQuery = "SELECT  * FROM " + TABLE_HISTORY;

		Cursor cursor = mDatabase.rawQuery(selectQuery, null);
		int counter = 0;
		if (cursor.moveToLast()) {
			do {
				HistoryItem item = new HistoryItem();
				item.setID(Integer.parseInt(cursor.getString(0)));
				item.setUrl(cursor.getString(1));
				item.setTitle(cursor.getString(2));
				item.setImageId(R.drawable.ic_history);
				itemList.add(item);
				counter++;
			} while (cursor.moveToPrevious() && counter < 100);
		}
		cursor.close();
		return itemList;
	}

	public List<HistoryItem> getAllHistoryItems() {
		List<HistoryItem> itemList = new ArrayList<HistoryItem>();
		String selectQuery = "SELECT  * FROM " + TABLE_HISTORY;

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

	// Updating single item
	public synchronized int updateHistoryItem(HistoryItem item) {

		ContentValues values = new ContentValues();
		values.put(KEY_URL, item.getUrl());
		values.put(KEY_TITLE, item.getTitle());
		int n = mDatabase.update(TABLE_HISTORY, values, KEY_ID + " = ?",
				new String[]{String.valueOf(item.getId())});
		// updating row
		return n;
	}

	// Deleting single item
	public synchronized void deleteHistoryItem(String id) {
		mDatabase.delete(TABLE_HISTORY, KEY_ID + " = ?",
				new String[]{String.valueOf(id)});
	}

	// Getting items Count
	public int getHistoryItemsCount() {
		String countQuery = "SELECT  * FROM " + TABLE_HISTORY;
		Cursor cursor = mDatabase.rawQuery(countQuery, null);
		cursor.close();

		// return count
		return cursor.getCount();
	}
}
