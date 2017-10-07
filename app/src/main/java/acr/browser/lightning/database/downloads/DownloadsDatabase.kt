package acr.browser.lightning.database.downloads

import acr.browser.lightning.database.LazyDatabase
import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The disk backed download database. See [DownloadsModel] for method documentation.
 */
@Singleton
class DownloadsDatabase @Inject constructor(
        application: Application
) : SQLiteOpenHelper(application, DATABASE_NAME, null, DATABASE_VERSION), DownloadsModel {

    private val lazy = LazyDatabase(this)
    private val database: SQLiteDatabase
        get() = lazy.db()


    // Creating Tables
    override fun onCreate(db: SQLiteDatabase) {
        val createDownloadsTable = "CREATE TABLE ${DatabaseUtils.sqlEscapeString(TABLE_DOWNLOADS)}(" +
                "${DatabaseUtils.sqlEscapeString(KEY_ID)} INTEGER PRIMARY KEY," +
                "${DatabaseUtils.sqlEscapeString(KEY_URL)} TEXT," +
                "${DatabaseUtils.sqlEscapeString(KEY_TITLE)} TEXT," +
                "${DatabaseUtils.sqlEscapeString(KEY_SIZE)} TEXT" +
                ')'
        db.execSQL(createDownloadsTable)
    }

    // Upgrading database
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop older table if it exists
        db.execSQL("DROP TABLE IF EXISTS ${DatabaseUtils.sqlEscapeString(TABLE_DOWNLOADS)}")
        // Create tables again
        onCreate(db)
    }

    override fun findDownloadForUrl(url: String): Maybe<DownloadItem> = Maybe.fromCallable {
        database.query(TABLE_DOWNLOADS, null, "$KEY_URL=?", arrayOf(url), null, null, "1").use {
            if (it.moveToFirst()) {
                return@fromCallable it.bindToDownloadItem()
            } else {
                return@fromCallable null
            }
        }
    }

    override fun isDownload(url: String): Single<Boolean> = Single.fromCallable {
        database.query(TABLE_DOWNLOADS, null, "$KEY_URL=?", arrayOf(url), null, null, null, "1").use {
            return@fromCallable it.moveToFirst()
        }
    }

    override fun addDownloadIfNotExists(item: DownloadItem): Single<Boolean> = Single.fromCallable {
        database.query(TABLE_DOWNLOADS, null, "$KEY_URL=?", arrayOf(item.url), null, null, "1").use {
            if (it.moveToFirst()) {
                return@fromCallable false
            }
        }

        val id = database.insert(TABLE_DOWNLOADS, null, item.toContentValues())

        return@fromCallable id != -1L
    }

    override fun addDownloadsList(downloadItems: List<DownloadItem>) = Completable.fromAction {
        database.beginTransaction()

        for (item in downloadItems) {
            addDownloadIfNotExists(item).subscribe()
        }

        database.setTransactionSuccessful()
        database.endTransaction()
    }

    override fun deleteDownload(url: String): Single<Boolean> = Single.fromCallable {
        val rows = database.delete(TABLE_DOWNLOADS, "$KEY_URL=?", arrayOf(url))

        return@fromCallable rows > 0
    }

    override fun deleteAllDownloads(): Completable = Completable.fromAction {
        database.run {
            delete(TABLE_DOWNLOADS, null, null)
            close()
        }
    }

    override fun getAllDownloads(): Single<List<DownloadItem>> = Single.fromCallable {
        database.query(TABLE_DOWNLOADS, null, null, null, null, null, null).use {
            return@fromCallable it.bindToDownloadItemList()
        }
    }

    override fun count(): Long = DatabaseUtils.queryNumEntries(database, TABLE_DOWNLOADS)

    /**
     * Maps the fields of [DownloadItem] to [ContentValues].
     */
    private fun DownloadItem.toContentValues(): ContentValues {
        val contentValues = ContentValues(3)
        contentValues.put(KEY_TITLE, title)
        contentValues.put(KEY_URL, url)
        contentValues.put(KEY_SIZE, contentSize)

        return contentValues
    }

    /**
     * Binds a [Cursor] to a single [DownloadItem].
     */
    private fun Cursor.bindToDownloadItem(): DownloadItem {
        val download = DownloadItem()

        download.setUrl(getString(getColumnIndex(KEY_URL)))
        download.setTitle(getString(getColumnIndex(KEY_TITLE)))
        download.setContentSize(getString(getColumnIndex(KEY_SIZE)))

        return download
    }

    /**
     * Binds a [Cursor] to a [List] of [DownloadItem].
     */
    private fun Cursor.bindToDownloadItemList(): List<DownloadItem> = use {
        val downloads = ArrayList<DownloadItem>()

        while (moveToNext()) {
            downloads.add(bindToDownloadItem())
        }

        return@use downloads
    }

    companion object {

        // Database Version
        private const val DATABASE_VERSION = 1

        // Database Name
        private const val DATABASE_NAME = "downloadManager"

        // HistoryItems table name
        private const val TABLE_DOWNLOADS = "download"

        // HistoryItems Table Columns names
        private const val KEY_ID = "id"
        private const val KEY_URL = "url"
        private const val KEY_TITLE = "title"
        private const val KEY_SIZE = "size"

    }

}
