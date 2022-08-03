package acr.browser.lightning.database.downloads

import acr.browser.lightning.database.databaseDelegate
import acr.browser.lightning.extensions.firstOrNullMap
import acr.browser.lightning.extensions.useMap
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
 * The disk backed download database. See [DownloadsRepository] for function documentation.
 */
@Singleton
class DownloadsDatabase @Inject constructor(
    application: Application
) : SQLiteOpenHelper(application, DATABASE_NAME, null, DATABASE_VERSION), DownloadsRepository {

    private val database: SQLiteDatabase by databaseDelegate()

    // Creating Tables
    override fun onCreate(db: SQLiteDatabase) {
        val createDownloadsTable =
            "CREATE TABLE ${DatabaseUtils.sqlEscapeString(TABLE_DOWNLOADS)}(" +
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

    override fun findDownloadForUrl(url: String): Maybe<DownloadEntry> = Maybe.fromCallable {
        database.query(
            TABLE_DOWNLOADS,
            null,
            "$KEY_URL=?",
            arrayOf(url),
            null,
            null,
            "1"
        ).firstOrNullMap { it.bindToDownloadItem() }
    }

    override fun isDownload(url: String): Single<Boolean> = Single.fromCallable {
        database.query(
            TABLE_DOWNLOADS,
            null,
            "$KEY_URL=?",
            arrayOf(url),
            null,
            null,
            null,
            "1"
        ).use {
            return@fromCallable it.moveToFirst()
        }
    }

    override fun addDownloadIfNotExists(entry: DownloadEntry): Single<Boolean> =
        Single.fromCallable {
            database.query(
                TABLE_DOWNLOADS,
                null,
                "$KEY_URL=?",
                arrayOf(entry.url),
                null,
                null,
                "1"
            ).use {
                if (it.moveToFirst()) {
                    return@fromCallable false
                }
            }

            val id = database.insert(TABLE_DOWNLOADS, null, entry.toContentValues())

            return@fromCallable id != -1L
        }

    override fun addDownloadsList(downloadEntries: List<DownloadEntry>): Completable =
        Completable.fromAction {
            database.apply {
                beginTransaction()
                setTransactionSuccessful()

                for (item in downloadEntries) {
                    addDownloadIfNotExists(item).subscribe()
                }

                endTransaction()
            }
        }

    override fun deleteDownload(url: String): Single<Boolean> = Single.fromCallable {
        return@fromCallable database.delete(TABLE_DOWNLOADS, "$KEY_URL=?", arrayOf(url)) > 0
    }

    override fun deleteAllDownloads(): Completable = Completable.fromAction {
        database.run {
            delete(TABLE_DOWNLOADS, null, null)
            close()
        }
    }

    override fun getAllDownloads(): Single<List<DownloadEntry>> = Single.fromCallable {
        return@fromCallable database.query(
            TABLE_DOWNLOADS,
            null,
            null,
            null,
            null,
            null,
            "$KEY_ID DESC"
        ).useMap { it.bindToDownloadItem() }
    }

    override fun count(): Long = DatabaseUtils.queryNumEntries(database, TABLE_DOWNLOADS)

    /**
     * Maps the fields of [DownloadEntry] to [ContentValues].
     */
    private fun DownloadEntry.toContentValues() = ContentValues(3).apply {
        put(KEY_TITLE, title)
        put(KEY_URL, url)
        put(KEY_SIZE, contentSize)
    }

    /**
     * Binds a [Cursor] to a single [DownloadEntry].
     */
    private fun Cursor.bindToDownloadItem() = DownloadEntry(
        url = getString(getColumnIndex(KEY_URL)),
        title = getString(getColumnIndex(KEY_TITLE)),
        contentSize = getString(getColumnIndex(KEY_SIZE))
    )

    companion object {

        // Database version
        private const val DATABASE_VERSION = 1

        // Database name
        private const val DATABASE_NAME = "downloadManager"

        // DownloadItem table name
        private const val TABLE_DOWNLOADS = "download"

        // DownloadItem table columns names
        private const val KEY_ID = "id"
        private const val KEY_URL = "url"
        private const val KEY_TITLE = "title"
        private const val KEY_SIZE = "size"

    }

}
