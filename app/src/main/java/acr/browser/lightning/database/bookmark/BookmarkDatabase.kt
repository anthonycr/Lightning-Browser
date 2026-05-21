package acr.browser.lightning.database.bookmark

import acr.browser.lightning.R
import acr.browser.lightning.browser.di.DatabaseScheduler
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.asFolder
import acr.browser.lightning.database.databaseDelegate
import acr.browser.lightning.extensions.firstOrNullMap
import acr.browser.lightning.extensions.useMap
import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.getStringOrNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The disk backed bookmark database. See [BookmarkRepository] for function documentation.
 *
 * Created by anthonycr on 5/6/17.
 */
@SuppressLint("Range")
@Singleton
class BookmarkDatabase @Inject constructor(
    application: Application,
    @DatabaseScheduler
    private val databaseDispatcher: CoroutineDispatcher,
) : SQLiteOpenHelper(application, DATABASE_NAME, null, DATABASE_VERSION), BookmarkRepository {

    private val defaultBookmarkTitle: String = application.getString(R.string.untitled)
    private val database: SQLiteDatabase by databaseDelegate()

    // Creating Tables
    override fun onCreate(db: SQLiteDatabase) {
        val createBookmarkTable = "CREATE TABLE ${DatabaseUtils.sqlEscapeString(TABLE_BOOKMARK)}(" +
            "${DatabaseUtils.sqlEscapeString(KEY_ID)} INTEGER PRIMARY KEY," +
            "${DatabaseUtils.sqlEscapeString(KEY_URL)} TEXT," +
            "${DatabaseUtils.sqlEscapeString(KEY_TITLE)} TEXT," +
            "${DatabaseUtils.sqlEscapeString(KEY_FOLDER)} TEXT," +
            "${DatabaseUtils.sqlEscapeString(KEY_POSITION)} INTEGER" +
            ')'
        db.execSQL(createBookmarkTable)
    }

    // Upgrading database
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop older table if it exists
        db.execSQL("DROP TABLE IF EXISTS ${DatabaseUtils.sqlEscapeString(TABLE_BOOKMARK)}")
        // Create tables again
        onCreate(db)
    }

    private suspend fun <T> databaseContext(block: suspend CoroutineScope.() -> T) =
        withContext(databaseDispatcher) { block() }

    /**
     * Queries the database for bookmarks with the provided URL. If it
     * cannot find any bookmarks with the given URL, it will try to query
     * for bookmarks with the [.alternateSlashUrl] as its URL.
     *
     * @param url the URL to query for.
     * @return a cursor with bookmarks matching the URL.
     */
    private fun queryWithOptionalEndSlash(url: String): Cursor {
        val alternateUrl = alternateSlashUrl(url)
        return database.query(
            TABLE_BOOKMARK,
            null,
            "$KEY_URL=? OR $KEY_URL=?",
            arrayOf(url, alternateUrl),
            null,
            null,
            null,
            "1"
        )
    }

    /**
     * Deletes a bookmark from the database with the provided URL. If it
     * cannot find any bookmark with the given URL, it will try to delete
     * a bookmark with the [.alternateSlashUrl] as its URL.
     *
     * @param url the URL to delete.
     * @return the number of deleted rows.
     */
    private fun deleteWithOptionalEndSlash(url: String): Int {
        return database.delete(
            TABLE_BOOKMARK,
            "$KEY_URL=? OR $KEY_URL=?",
            arrayOf(url, alternateSlashUrl(url))
        )
    }

    /**
     * Updates a bookmark in the database with the provided URL. If it
     * cannot find any bookmark with the given URL, it will try to update
     * a bookmark with the [.alternateSlashUrl] as its URL.
     *
     * @param url           the URL to update.
     * @param contentValues the new values to update to.
     * @return the number of rows updated.
     */
    private fun updateWithOptionalEndSlash(url: String, contentValues: ContentValues): Int {
        var updatedRows = database.update(
            TABLE_BOOKMARK,
            contentValues,
            "$KEY_URL=?",
            arrayOf(url)
        )

        if (updatedRows == 0) {
            val alternateUrl = alternateSlashUrl(url)
            updatedRows = database.update(
                TABLE_BOOKMARK,
                contentValues,
                "$KEY_URL=?",
                arrayOf(alternateUrl)
            )
        }

        return updatedRows
    }

    override suspend fun findBookmarkForUrl(url: String): Bookmark.Entry? = databaseContext {
        queryWithOptionalEndSlash(url).firstOrNullMap { it.bindToBookmarkEntry() }
    }

    override suspend fun isBookmark(url: String): Boolean = withContext(databaseDispatcher) {
        queryWithOptionalEndSlash(url).use {
            it.moveToFirst()
        }
    }

    override suspend fun addBookmarkIfNotExists(
        entry: Bookmark.Entry
    ): Boolean = withContext(databaseDispatcher) {
        queryWithOptionalEndSlash(entry.url).use {
            if (it.moveToFirst()) {
                return@withContext false
            }
        }

        val id = database.insert(
            TABLE_BOOKMARK,
            null,
            entry.bindBookmarkToContentValues()
        )

        return@withContext id != -1L
    }

    override suspend fun addBookmarkList(
        bookmarkItems: List<Bookmark.Entry>
    ): Unit = withContext(databaseDispatcher) {
        database.apply {
            beginTransaction()

            for (item in bookmarkItems) {
                addBookmarkIfNotExists(item)
            }

            setTransactionSuccessful()
            endTransaction()
        }
    }

    override suspend fun deleteBookmark(
        entry: Bookmark.Entry
    ): Boolean = withContext(databaseDispatcher) {
        deleteWithOptionalEndSlash(entry.url) > 0
    }

    override suspend fun renameFolder(
        oldName: String,
        newName: String
    ): Unit = withContext(databaseDispatcher) {
        val contentValues = ContentValues(1).apply {
            put(KEY_FOLDER, newName)
        }

        database.update(TABLE_BOOKMARK, contentValues, "$KEY_FOLDER=?", arrayOf(oldName))
    }

    override suspend fun deleteFolder(
        folderToDelete: String
    ): Unit = withContext(databaseDispatcher) {
        renameFolder(folderToDelete, "")
    }

    override suspend fun deleteAllBookmarks(): Unit = withContext(databaseDispatcher) {
        database.run {
            delete(TABLE_BOOKMARK, null, null)
            close()
        }
    }

    override suspend fun editBookmark(
        oldBookmark: Bookmark.Entry,
        newBookmark: Bookmark.Entry
    ): Unit = withContext(databaseDispatcher) {
        val contentValues = newBookmark.bindBookmarkToContentValues()

        updateWithOptionalEndSlash(oldBookmark.url, contentValues)
    }

    override suspend fun getAllBookmarksSorted(): List<Bookmark.Entry> =
        withContext(databaseDispatcher) {
            database.query(
                TABLE_BOOKMARK,
                null,
                null,
                null,
                null,
                null,
                "$KEY_FOLDER, $KEY_POSITION ASC, $KEY_TITLE COLLATE NOCASE ASC, $KEY_URL ASC"
            ).useMap { it.bindToBookmarkEntry() }
        }

    override suspend fun getBookmarksFromFolderSorted(
        folder: String?
    ): List<Bookmark> = withContext(databaseDispatcher) {
        val finalFolder = folder ?: ""
        database.query(
            TABLE_BOOKMARK,
            null,
            "$KEY_FOLDER=?",
            arrayOf(finalFolder),
            null,
            null,
            "$KEY_POSITION ASC, $KEY_TITLE COLLATE NOCASE ASC, $KEY_URL ASC"
        ).useMap { it.bindToBookmarkEntry() }
    }

    override suspend fun getFoldersSorted(): List<Bookmark.Folder> =
        withContext(databaseDispatcher) {
            database
                .query(
                    true,
                    TABLE_BOOKMARK,
                    arrayOf(KEY_FOLDER),
                    null,
                    null,
                    null,
                    null,
                    "$KEY_FOLDER ASC",
                    null
                )
                .useMap { it.getString(it.getColumnIndex(KEY_FOLDER)) }
                .filter { !it.isNullOrEmpty() }
                .map(String::asFolder)
        }

    override suspend fun getFolderNames(): List<String> = withContext(databaseDispatcher) {
        database.query(
            true,
            TABLE_BOOKMARK,
            arrayOf(KEY_FOLDER),
            null,
            null,
            null,
            null,
            "$KEY_FOLDER ASC",
            null
        ).useMap { it.getString(it.getColumnIndex(KEY_FOLDER)) }
            .filter { !it.isNullOrEmpty() }
    }

    override suspend fun count(): Long = withContext(databaseDispatcher) {
        DatabaseUtils.queryNumEntries(database, TABLE_BOOKMARK)
    }

    /**
     * Binds a [Bookmark.Entry] to [ContentValues].
     *
     * @return a valid values object that can be inserted into the database.
     */
    private fun Bookmark.Entry.bindBookmarkToContentValues() = ContentValues(4).apply {
        put(KEY_TITLE, title.takeIf(String::isNotBlank) ?: defaultBookmarkTitle)
        put(KEY_URL, url)
        put(KEY_FOLDER, folder.title)
        put(KEY_POSITION, position)
    }

    /**
     * Binds a cursor to a [Bookmark.Entry]. This is
     * a non consuming operation on the cursor. Note that
     * this operation is not safe to perform on a cursor
     * unless you know that the cursor is of history items.
     *
     * @return a valid item containing all the pertinent information.
     */
    private fun Cursor.bindToBookmarkEntry() = Bookmark.Entry(
        url = getString(getColumnIndex(KEY_URL)),
        title = getString(getColumnIndex(KEY_TITLE)),
        folder = getStringOrNull(getColumnIndex(KEY_FOLDER)).asFolder(),
        position = getInt(getColumnIndex(KEY_POSITION))
    )

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
    private fun alternateSlashUrl(url: String): String = if (url.endsWith("/")) {
        url.substring(0, url.length - 1)
    } else {
        "$url/"
    }

    companion object {

        // Database version
        private const val DATABASE_VERSION = 1

        // Database name
        private const val DATABASE_NAME = "bookmarkManager"

        // Bookmark table name
        private const val TABLE_BOOKMARK = "bookmark"

        // Bookmark table columns names
        private const val KEY_ID = "id"
        private const val KEY_URL = "url"
        private const val KEY_TITLE = "title"
        private const val KEY_FOLDER = "folder"
        private const val KEY_POSITION = "position"

    }

}
