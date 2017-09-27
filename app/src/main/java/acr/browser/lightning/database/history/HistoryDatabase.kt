/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.database.history

import acr.browser.lightning.R
import acr.browser.lightning.database.HistoryItem
import acr.browser.lightning.database.LazyDatabase
import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.support.annotation.WorkerThread
import com.anthonycr.bonsai.Completable
import com.anthonycr.bonsai.Single
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


/**
 * The disk backed download database.
 * See [HistoryModel] for method
 * documentation.
 */
@Singleton
@WorkerThread
class HistoryDatabase @Inject constructor(
        application: Application
) : SQLiteOpenHelper(application, DATABASE_NAME, null, DATABASE_VERSION), HistoryModel {

    private val lazyDatabase = LazyDatabase(this)

    private val database: SQLiteDatabase
        get() = lazyDatabase.db()

    // Creating Tables
    override fun onCreate(db: SQLiteDatabase) {
        val createHistoryTable = "CREATE TABLE $TABLE_HISTORY(" +
                " $KEY_ID INTEGER PRIMARY KEY," +
                " $KEY_URL TEXT," +
                " $KEY_TITLE TEXT," +
                " $KEY_TIME_VISITED INTEGER" +
                ")"
        db.execSQL(createHistoryTable)
    }

    // Upgrading database
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop older table if it exists
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        // Create tables again
        onCreate(db)
    }

    override fun deleteHistory(): Completable {
        return Completable.create { subscriber ->
            database.delete(TABLE_HISTORY, null, null)
            database.close()

            subscriber.onComplete()
        }
    }

    override fun deleteHistoryItem(url: String): Completable {
        return Completable.create { subscriber ->
            database.delete(TABLE_HISTORY, "$KEY_URL = ?", arrayOf(url))

            subscriber.onComplete()
        }
    }

    override fun visitHistoryItem(url: String, title: String?): Completable {
        return Completable.create {
            val values = ContentValues()
            values.put(KEY_TITLE, title ?: "")
            values.put(KEY_TIME_VISITED, System.currentTimeMillis())

            val cursor = database.query(false, TABLE_HISTORY, arrayOf(KEY_URL),
                    "$KEY_URL = ?", arrayOf(url), null, null, null, "1")

            if (cursor.count > 0) {
                database.update(TABLE_HISTORY, values, KEY_URL + " = ?", arrayOf(url))
            } else {
                addHistoryItem(HistoryItem(url, title ?: ""))
            }

            cursor.close()
        }
    }

    override fun findHistoryItemsContaining(query: String): Single<List<HistoryItem>> {
        return Single.create { subscriber ->
            val itemList = ArrayList<HistoryItem>(5)

            val search = "%$query%"

            val cursor = database.query(TABLE_HISTORY, null, "$KEY_TITLE LIKE ? OR $KEY_URL LIKE ?",
                    arrayOf(search, search), null, null, KEY_TIME_VISITED + " DESC", "5")

            while (cursor.moveToNext()) {
                itemList.add(cursor.bindToHistoryItem())
            }

            cursor.close()

            subscriber.onItem(itemList)
            subscriber.onComplete()
        }
    }

    override fun lastHundredVisitedHistoryItems(): Single<List<HistoryItem>> {
        return Single.create { subscriber ->
            val itemList = ArrayList<HistoryItem>(100)
            val cursor = database.query(TABLE_HISTORY, null, null, null, null, null, KEY_TIME_VISITED + " DESC", "100")

            while (cursor.moveToNext()) {
                itemList.add(cursor.bindToHistoryItem())
            }

            cursor.close()

            subscriber.onItem(itemList)
            subscriber.onComplete()
        }
    }

    @WorkerThread
    @Synchronized private fun addHistoryItem(item: HistoryItem) {
        val values = ContentValues()
        values.put(KEY_URL, item.url)
        values.put(KEY_TITLE, item.title)
        values.put(KEY_TIME_VISITED, System.currentTimeMillis())
        database.insert(TABLE_HISTORY, null, values)
    }

    @WorkerThread
    @Synchronized internal fun getHistoryItem(url: String): String? {
        val cursor = database.query(TABLE_HISTORY, arrayOf(KEY_ID, KEY_URL, KEY_TITLE),
                "$KEY_URL = ?", arrayOf(url), null, null, null, "1")
        var m: String? = null
        if (cursor != null) {
            cursor.moveToFirst()
            m = cursor.getString(0)

            cursor.close()
        }
        return m
    }

    internal fun getAllHistoryItems(): List<HistoryItem> {
        val itemList = ArrayList<HistoryItem>()

        val cursor = database.query(TABLE_HISTORY, null, null, null, null, null, KEY_TIME_VISITED + " DESC")

        while (cursor.moveToNext()) {
            itemList.add(cursor.bindToHistoryItem())
        }

        cursor.close()

        return itemList
    }

    internal fun getHistoryItemsCount(): Long = DatabaseUtils.queryNumEntries(database, TABLE_HISTORY)

    private fun Cursor.bindToHistoryItem(): HistoryItem {
        val historyItem = HistoryItem()
        historyItem.setUrl(this.getString(1))
        historyItem.setTitle(this.getString(2))
        historyItem.imageId = R.drawable.ic_history

        return historyItem
    }

    companion object {

        // All Static variables
        // Database Version
        private const val DATABASE_VERSION = 2

        // Database Name
        private const val DATABASE_NAME = "historyManager"

        // HistoryItems table name
        private const val TABLE_HISTORY = "history"

        // HistoryItems Table Columns names
        private const val KEY_ID = "id"
        private const val KEY_URL = "url"
        private const val KEY_TITLE = "title"
        private const val KEY_TIME_VISITED = "time"

    }
}
