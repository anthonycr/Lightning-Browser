package acr.browser.lightning.database.bookmark

import acr.browser.lightning.R
import acr.browser.lightning.constant.FOLDER
import acr.browser.lightning.database.HistoryItem
import acr.browser.lightning.database.LazyDatabase
import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.text.TextUtils
import com.anthonycr.bonsai.Completable
import com.anthonycr.bonsai.Single
import com.anthonycr.bonsai.SingleAction
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The disk backed bookmark database.
 * See [BookmarkModel] for method
 * documentation.
 *
 *
 * Created by anthonycr on 5/6/17.
 */
@Singleton
class BookmarkDatabase @Inject constructor(
        application: Application
) : SQLiteOpenHelper(application, DATABASE_NAME, null, DATABASE_VERSION), BookmarkModel {

    private val defaultBookmarkTitle: String = application.getString(R.string.untitled)
    private val lazy = LazyDatabase(this)
    private val database: SQLiteDatabase
        get() = lazy.db()

    // Creating Tables
    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_BOOKMARK_TABLE = "CREATE TABLE ${DatabaseUtils.sqlEscapeString(TABLE_BOOKMARK)}(" +
                "${DatabaseUtils.sqlEscapeString(KEY_ID)} INTEGER PRIMARY KEY," +
                "${DatabaseUtils.sqlEscapeString(KEY_URL)} TEXT," +
                "${DatabaseUtils.sqlEscapeString(KEY_TITLE)} TEXT," +
                "${DatabaseUtils.sqlEscapeString(KEY_FOLDER)} TEXT," +
                "${DatabaseUtils.sqlEscapeString(KEY_POSITION)} INTEGER" +
                ')'
        db.execSQL(CREATE_BOOKMARK_TABLE)
    }

    // Upgrading database
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop older table if it exists
        db.execSQL("DROP TABLE IF EXISTS ${DatabaseUtils.sqlEscapeString(TABLE_BOOKMARK)}")
        // Create tables again
        onCreate(db)
    }

    /**
     * Queries the database for bookmarks with the provided URL. If it
     * cannot find any bookmarks with the given URL, it will try to query
     * for bookmarks with the [.alternateSlashUrl] as its URL.
     *
     * @param url the URL to query for.
     * @return a cursor with bookmarks matching the URL.
     */
    private fun queryWithOptionalEndSlash(url: String): Cursor {
        var cursor = database.query(TABLE_BOOKMARK, null, "$KEY_URL=?", arrayOf(url), null, null, null, "1")

        if (cursor.count == 0) {
            cursor.close()

            val alternateUrl = alternateSlashUrl(url)
            cursor = database.query(TABLE_BOOKMARK, null, "$KEY_URL=?", arrayOf(alternateUrl), null, null, null, "1")
        }

        return cursor
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
        var deletedRows = database.delete(TABLE_BOOKMARK, "$KEY_URL=?", arrayOf(url))

        if (deletedRows == 0) {
            val alternateUrl = alternateSlashUrl(url)
            deletedRows = database.delete(TABLE_BOOKMARK, "$KEY_URL=?", arrayOf(alternateUrl))
        }

        return deletedRows
    }

    /**
     * Updates a bookmark in the database with the provided URL. If it
     * cannot find any bookmark with the given URL, it will try to update
     * a bookmark with the [.alternateSlashUrl] as its URL.
     *
     * @param url           the URL to update.
     * @param contentValues the new values to update to.
     * @return the numebr of rows updated.
     */
    private fun updateWithOptionalEndSlash(url: String, contentValues: ContentValues): Int {
        var updatedRows = database.update(TABLE_BOOKMARK, contentValues, "$KEY_URL=?", arrayOf(url))

        if (updatedRows == 0) {
            val alternateUrl = alternateSlashUrl(url)
            updatedRows = database.update(TABLE_BOOKMARK, contentValues, "$KEY_URL=?", arrayOf(alternateUrl))
        }

        return updatedRows
    }

    override fun findBookmarkForUrl(url: String): Single<HistoryItem> {
        return Single.create { subscriber ->
            val cursor = queryWithOptionalEndSlash(url)

            if (cursor.moveToFirst()) {
                subscriber.onItem(cursor.bindToHistoryItem())
            } else {
                subscriber.onItem(null)
            }

            cursor.close()
            subscriber.onComplete()
        }
    }

    override fun isBookmark(url: String): Single<Boolean> {
        return Single.create { subscriber ->
            val cursor = queryWithOptionalEndSlash(url)

            subscriber.onItem(cursor.moveToFirst())

            cursor.close()
            subscriber.onComplete()
        }
    }

    override fun addBookmarkIfNotExists(item: HistoryItem): Single<Boolean> {
        return Single.create(SingleAction { subscriber ->
            val cursor = queryWithOptionalEndSlash(item.url)

            if (cursor.moveToFirst()) {
                cursor.close()
                subscriber.onItem(false)
                subscriber.onComplete()
                return@SingleAction
            }

            cursor.close()

            val id = database.insert(TABLE_BOOKMARK, null, bindBookmarkToContentValues(item))

            subscriber.onItem(id != -1L)
            subscriber.onComplete()
        })
    }

    override fun addBookmarkList(bookmarkItems: List<HistoryItem>): Completable {
        return Completable.create { subscriber ->
            database.beginTransaction()

            for (item in bookmarkItems) {
                addBookmarkIfNotExists(item).subscribe()
            }

            database.setTransactionSuccessful()
            database.endTransaction()

            subscriber.onComplete()
        }
    }

    override fun deleteBookmark(bookmark: HistoryItem): Single<Boolean> {
        return Single.create { subscriber ->
            val rows = deleteWithOptionalEndSlash(bookmark.url)

            subscriber.onItem(rows > 0)
            subscriber.onComplete()
        }
    }

    override fun renameFolder(oldName: String, newName: String): Completable {
        return Completable.create { subscriber ->
            val contentValues = ContentValues(1)
            contentValues.put(KEY_FOLDER, newName)

            database.update(TABLE_BOOKMARK, contentValues, "$KEY_FOLDER=?", arrayOf(oldName))

            subscriber.onComplete()
        }
    }

    override fun deleteFolder(folderToDelete: String): Completable {
        return Completable.create { subscriber ->
            renameFolder(folderToDelete, "").subscribe()

            subscriber.onComplete()
        }
    }

    override fun deleteAllBookmarks(): Completable {
        return Completable.create { subscriber ->
            database.delete(TABLE_BOOKMARK, null, null)

            subscriber.onComplete()
        }
    }

    override fun editBookmark(oldBookmark: HistoryItem, newBookmark: HistoryItem): Completable {
        return Completable.create { subscriber ->
            if (newBookmark.title.isEmpty()) {
                newBookmark.setTitle(defaultBookmarkTitle)
            }
            val contentValues = bindBookmarkToContentValues(newBookmark)

            updateWithOptionalEndSlash(oldBookmark.url, contentValues)

            subscriber.onComplete()
        }
    }

    override fun getAllBookmarks(): Single<List<HistoryItem>> {
        return Single.create { subscriber ->
            val cursor = database.query(TABLE_BOOKMARK, null, null, null, null, null, null)

            subscriber.onItem(cursor.bindToHistoryItemList())
            subscriber.onComplete()

            cursor.close()
        }
    }

    override fun getBookmarksFromFolderSorted(folder: String?): Single<List<HistoryItem>> {
        return Single.create { subscriber ->
            val finalFolder = folder ?: ""
            val cursor = database.query(TABLE_BOOKMARK, null, "$KEY_FOLDER=?", arrayOf(finalFolder), null, null, null)

            val list = cursor.bindToHistoryItemList()
            Collections.sort(list)
            subscriber.onItem(list)
            subscriber.onComplete()

            cursor.close()
        }
    }

    override fun getFoldersSorted(): Single<List<HistoryItem>> {
        return Single.create { subscriber ->
            val cursor = database.query(true, TABLE_BOOKMARK, arrayOf(KEY_FOLDER), null, null, null, null, null, null)

            val folders = ArrayList<HistoryItem>()
            while (cursor.moveToNext()) {
                val folderName = cursor.getString(cursor.getColumnIndex(KEY_FOLDER))
                if (TextUtils.isEmpty(folderName)) {
                    continue
                }

                val folder = HistoryItem()
                folder.setIsFolder(true)
                folder.setTitle(folderName)
                folder.imageId = R.drawable.ic_folder
                folder.setUrl("$FOLDER$folderName")

                folders.add(folder)
            }

            cursor.close()

            Collections.sort(folders)
            subscriber.onItem(folders)
            subscriber.onComplete()
        }
    }

    override fun getFolderNames(): Single<List<String>> {
        return Single.create { subscriber ->
            val cursor = database.query(true, TABLE_BOOKMARK, arrayOf(KEY_FOLDER), null, null, null, null, null, null)

            val folders = ArrayList<String>()
            while (cursor.moveToNext()) {
                val folderName = cursor.getString(cursor.getColumnIndex(KEY_FOLDER))
                if (TextUtils.isEmpty(folderName)) {
                    continue
                }

                folders.add(folderName)
            }

            cursor.close()

            subscriber.onItem(folders)
            subscriber.onComplete()
        }
    }

    override fun count(): Long = DatabaseUtils.queryNumEntries(database, TABLE_BOOKMARK)

    /**
     * Binds a [HistoryItem] to [ContentValues].
     *
     * @param bookmarkItem the bookmark to bind.
     * @return a valid values object that can be inserted
     * into the database.
     */
    private fun bindBookmarkToContentValues(bookmarkItem: HistoryItem): ContentValues {
        val contentValues = ContentValues(4)
        contentValues.put(KEY_TITLE, bookmarkItem.title)
        contentValues.put(KEY_URL, bookmarkItem.url)
        contentValues.put(KEY_FOLDER, bookmarkItem.folder)
        contentValues.put(KEY_POSITION, bookmarkItem.position)

        return contentValues
    }

    /**
     * Binds a cursor to a [HistoryItem]. This is
     * a non consuming operation on the cursor. Note that
     * this operation is not safe to perform on a cursor
     * unless you know that the cursor is of history items.
     *
     * @return a valid item containing all the pertinent information.
     */
    private fun Cursor.bindToHistoryItem(): HistoryItem {
        val bookmark = HistoryItem()

        bookmark.imageId = R.drawable.ic_bookmark
        bookmark.setUrl(getString(getColumnIndex(KEY_URL)))
        bookmark.setTitle(getString(getColumnIndex(KEY_TITLE)))
        bookmark.setFolder(getString(getColumnIndex(KEY_FOLDER)))
        bookmark.position = getInt(getColumnIndex(KEY_POSITION))

        return bookmark
    }

    /**
     * Binds a cursor to a list of [HistoryItem].
     * This operation consumes the cursor.
     *
     * @return a valid list of history items, may be empty.
     */
    private fun Cursor.bindToHistoryItemList(): List<HistoryItem> = use {
        val bookmarks = ArrayList<HistoryItem>()

        while (moveToNext()) {
            bookmarks.add(bindToHistoryItem())
        }

        return bookmarks
    }

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
    private fun alternateSlashUrl(url: String): String {
        return if (url.endsWith("/")) {
            url.substring(0, url.length - 1)
        } else {
            url + '/'
        }
    }

    companion object {

        // Database Version
        private const val DATABASE_VERSION = 1

        // Database Name
        private const val DATABASE_NAME = "bookmarkManager"

        // HistoryItems table name
        private const val TABLE_BOOKMARK = "bookmark"

        // HistoryItems Table Columns names
        private const val KEY_ID = "id"
        private const val KEY_URL = "url"
        private const val KEY_TITLE = "title"
        private const val KEY_FOLDER = "folder"
        private const val KEY_POSITION = "position"

    }

}
