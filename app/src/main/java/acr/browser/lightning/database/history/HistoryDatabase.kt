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
import io.reactivex.Completable
import io.reactivex.Single
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


/**
 * The disk backed download database. See [HistoryRepository] for function documentation.
 */
@Singleton
@WorkerThread
class HistoryDatabase @Inject constructor(
        application: Application
) : SQLiteOpenHelper(application, DATABASE_NAME, null, DATABASE_VERSION), HistoryRepository {

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

    override fun deleteHistory(): Completable = Completable.fromAction {
        database.run {
            delete(TABLE_HISTORY, null, null)
            close()
        }
    }

    override fun deleteHistoryItem(url: String): Completable = Completable.fromAction {
        database.delete(TABLE_HISTORY, "$KEY_URL = ?", arrayOf(url))
    }

    override fun visitHistoryItem(url: String, title: String?): Completable = Completable.create {
        val values = ContentValues()
        values.put(KEY_TITLE, title ?: "")
        values.put(KEY_TIME_VISITED, System.currentTimeMillis())

        database.query(false, TABLE_HISTORY, arrayOf(KEY_URL), "$KEY_URL = ?", arrayOf(url), null, null, null, "1").use {
            if (it.count > 0) {
                database.update(TABLE_HISTORY, values, KEY_URL + " = ?", arrayOf(url))
            } else {
                addHistoryItem(HistoryItem(url, title ?: ""))
            }
        }
    }

    override fun findHistoryItemsContaining(query: String): Single<List<HistoryItem>> =
            Single.fromCallable {
                val itemList = ArrayList<HistoryItem>(5)

                val search = "%$query%"

                database.query(TABLE_HISTORY, null, "$KEY_TITLE LIKE ? OR $KEY_URL LIKE ?",
                        arrayOf(search, search), null, null, "$KEY_TIME_VISITED DESC", "5").use {
                    while (it.moveToNext()) {
                        itemList.add(it.bindToHistoryItem())
                    }
                }

                return@fromCallable itemList
            }

    override fun lastHundredVisitedHistoryItems(): Single<List<HistoryItem>> =
            Single.fromCallable {
                val itemList = ArrayList<HistoryItem>(100)
                database.query(TABLE_HISTORY, null, null, null, null, null, "$KEY_TIME_VISITED DESC", "100").use {
                    while (it.moveToNext()) {
                        itemList.add(it.bindToHistoryItem())
                    }
                }

                return@fromCallable itemList
            }

    @WorkerThread
    @Synchronized
    private fun addHistoryItem(item: HistoryItem) {
        val values = ContentValues()
        values.put(KEY_URL, item.url)
        values.put(KEY_TITLE, item.title)
        values.put(KEY_TIME_VISITED, System.currentTimeMillis())
        database.insert(TABLE_HISTORY, null, values)
    }

    @WorkerThread
    @Synchronized internal fun getHistoryItem(url: String): String? =
            database.query(TABLE_HISTORY, arrayOf(KEY_ID, KEY_URL, KEY_TITLE),
                    "$KEY_URL = ?", arrayOf(url), null, null, null, "1").use {
                it.moveToFirst()

                return it.getString(0)
            }

    internal fun getAllHistoryItems(): List<HistoryItem> {
        val itemList = ArrayList<HistoryItem>()

        database.query(TABLE_HISTORY, null, null, null, null, null, "$KEY_TIME_VISITED DESC").use {
            while (it.moveToNext()) {
                itemList.add(it.bindToHistoryItem())
            }
        }

        return itemList
    }

    internal fun getHistoryItemsCount(): Long = DatabaseUtils.queryNumEntries(database, TABLE_HISTORY)

    private fun Cursor.bindToHistoryItem() = HistoryItem().apply {
        setUrl(getString(1))
        setTitle(getString(2))
        imageId = R.drawable.ic_history
    }

    companion object {

        // Database version
        private const val DATABASE_VERSION = 2

        // Database name
        private const val DATABASE_NAME = "historyManager"

        // HistoryItem table name
        private const val TABLE_HISTORY = "history"

        // HistoryItem table columns names
        private const val KEY_ID = "id"
        private const val KEY_URL = "url"
        private const val KEY_TITLE = "title"
        private const val KEY_TIME_VISITED = "time"

    }
}
