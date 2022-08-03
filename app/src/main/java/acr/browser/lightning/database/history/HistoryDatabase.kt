/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.database.history

import acr.browser.lightning.database.HistoryEntry
import acr.browser.lightning.database.databaseDelegate
import acr.browser.lightning.extensions.firstOrNullMap
import acr.browser.lightning.extensions.useMap
import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.annotation.WorkerThread
import io.reactivex.Completable
import io.reactivex.Single
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

    private val database: SQLiteDatabase by databaseDelegate()

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

    override fun deleteHistoryEntry(url: String): Completable = Completable.fromAction {
        database.delete(TABLE_HISTORY, "$KEY_URL = ?", arrayOf(url))
    }

    override fun visitHistoryEntry(url: String, title: String?): Completable =
        Completable.fromAction {
            val values = ContentValues().apply {
                put(KEY_TITLE, title ?: "")
                put(KEY_TIME_VISITED, System.currentTimeMillis())
            }

            database.query(
                false,
                TABLE_HISTORY,
                arrayOf(KEY_ID),
                "$KEY_URL = ?",
                arrayOf(url),
                null,
                null,
                null,
                "1"
            ).use {
                if (it.count > 0) {
                    database.update(TABLE_HISTORY, values, "$KEY_URL = ?", arrayOf(url))
                } else {
                    addHistoryEntry(HistoryEntry(url, title ?: ""))
                }
            }
        }

    override fun findHistoryEntriesContaining(query: String): Single<List<HistoryEntry>> =
        Single.fromCallable {
            val search = "%$query%"

            return@fromCallable database.query(
                TABLE_HISTORY,
                null,
                "$KEY_TITLE LIKE ? OR $KEY_URL LIKE ?",
                arrayOf(search, search),
                null,
                null,
                "$KEY_TIME_VISITED DESC",
                "5"
            ).useMap { it.bindToHistoryEntry() }
        }

    override fun lastHundredVisitedHistoryEntries(): Single<List<HistoryEntry>> =
        Single.fromCallable {
            database.query(
                TABLE_HISTORY,
                null,
                null,
                null,
                null,
                null,
                "$KEY_TIME_VISITED DESC",
                "100"
            ).useMap { it.bindToHistoryEntry() }
        }

    @WorkerThread
    private fun addHistoryEntry(item: HistoryEntry) {
        database.insert(TABLE_HISTORY, null, item.toContentValues())
    }

    @WorkerThread
    fun getHistoryEntry(url: String): String? =
        database.query(
            TABLE_HISTORY,
            arrayOf(KEY_ID, KEY_URL, KEY_TITLE),
            "$KEY_URL = ?",
            arrayOf(url),
            null,
            null,
            null,
            "1"
        ).firstOrNullMap { it.getString(0) }


    fun getAllHistoryEntries(): List<HistoryEntry> {
        return database.query(
            TABLE_HISTORY,
            null,
            null,
            null,
            null,
            null,
            "$KEY_TIME_VISITED DESC"
        ).useMap { it.bindToHistoryEntry() }
    }

    fun getHistoryEntriesCount(): Long = DatabaseUtils.queryNumEntries(database, TABLE_HISTORY)

    private fun HistoryEntry.toContentValues() = ContentValues().apply {
        put(KEY_URL, url)
        put(KEY_TITLE, title)
        put(KEY_TIME_VISITED, lastTimeVisited)
    }

    private fun Cursor.bindToHistoryEntry() = HistoryEntry(
        url = getString(1),
        title = getString(2),
        lastTimeVisited = getLong(3)
    )

    companion object {

        // Database version
        private const val DATABASE_VERSION = 2

        // Database name
        private const val DATABASE_NAME = "historyManager"

        // HistoryEntry table name
        private const val TABLE_HISTORY = "history"

        // HistoryEntry table columns names
        private const val KEY_ID = "id"
        private const val KEY_URL = "url"
        private const val KEY_TITLE = "title"
        private const val KEY_TIME_VISITED = "time"

    }
}
