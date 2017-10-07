package acr.browser.lightning.database.downloads

import acr.browser.lightning.database.LazyDatabase
import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.anthonycr.bonsai.Completable
import com.anthonycr.bonsai.Single
import com.anthonycr.bonsai.SingleAction
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

    override fun findDownloadForUrl(url: String): Single<DownloadItem> =
            Single.create { subscriber ->
                database.query(TABLE_DOWNLOADS, null, "$KEY_URL=?", arrayOf(url), null, null, "1").use {
                    if (it.moveToFirst()) {
                        subscriber.onItem(it.bindToDownloadItem())
                    } else {
                        subscriber.onItem(null)
                    }
                }
                subscriber.onComplete()
            }

    override fun isDownload(url: String): Single<Boolean> = Single.create { subscriber ->
        database.query(TABLE_DOWNLOADS, null, "$KEY_URL=?", arrayOf(url), null, null, null, "1").use {
            subscriber.onItem(it.moveToFirst())
        }
        subscriber.onComplete()
    }

    override fun addDownloadIfNotExists(item: DownloadItem): Single<Boolean> =
            Single.create(SingleAction { subscriber ->
                database.query(TABLE_DOWNLOADS, null, "$KEY_URL=?", arrayOf(item.url), null, null, "1").use {
                    if (it.moveToFirst()) {
                        subscriber.onItem(false)
                        subscriber.onComplete()
                        return@SingleAction
                    }
                }

                val id = database.insert(TABLE_DOWNLOADS, null, item.toContentValues())

                subscriber.onItem(id != -1L)
                subscriber.onComplete()
            })

    override fun addDownloadsList(downloadItems: List<DownloadItem>): Completable =
            Completable.create { subscriber ->
                database.beginTransaction()

                for (item in downloadItems) {
                    addDownloadIfNotExists(item).subscribe()
                }

                database.setTransactionSuccessful()
                database.endTransaction()

                subscriber.onComplete()
            }

    override fun deleteDownload(url: String): Single<Boolean> = Single.create { subscriber ->
        val rows = database.delete(TABLE_DOWNLOADS, "$KEY_URL=?", arrayOf(url))

        subscriber.onItem(rows > 0)
        subscriber.onComplete()
    }

    override fun deleteAllDownloads(): Completable = Completable.create { subscriber ->
        database.delete(TABLE_DOWNLOADS, null, null)

        subscriber.onComplete()
    }

    override fun getAllDownloads(): Single<List<DownloadItem>> = Single.create { subscriber ->
        database.query(TABLE_DOWNLOADS, null, null, null, null, null, null).use {
            subscriber.onItem(it.bindToDownloadItemList())
            subscriber.onComplete()
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
