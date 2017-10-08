package acr.browser.lightning.database.whitelist

import acr.browser.lightning.database.LazyDatabase
import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.support.annotation.WorkerThread
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The disk backed ad block whitelist database. See [AdBlockWhitelistModel] for function
 * documentation.s
 */
@Singleton
@WorkerThread
class AdBlockWhitelistDatabase @Inject constructor(
        application: Application
) : SQLiteOpenHelper(application, DATABASE_NAME, null, DATABASE_VERSION), AdBlockWhitelistModel {

    private val lazyDatabase = LazyDatabase(this)

    private val database: SQLiteDatabase
        get() = lazyDatabase.db()

    // Creating Tables
    override fun onCreate(db: SQLiteDatabase) {
        val createWhitelistTable = "CREATE TABLE $TABLE_WHITELIST(" +
                " $KEY_ID INTEGER PRIMARY KEY," +
                " $KEY_URL TEXT," +
                " $KEY_CREATED INTEGER" +
                ")"
        db.execSQL(createWhitelistTable)
    }

    // Upgrading database
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop older table if it exists
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WHITELIST")
        // Create tables again
        onCreate(db)
    }

    private fun Cursor.bindToWhitelistItem(): WhitelistItem {
        return WhitelistItem(
                url = getString(1),
                timeCreated = getLong(2)
        )
    }

    override fun allWhitelistItems(): Single<List<WhitelistItem>> = Single.fromCallable {
        val whitelistItems = mutableListOf<WhitelistItem>()
        database.query(TABLE_WHITELIST, null, null, null, null, null, "$KEY_CREATED DESC").use {
            while (it.moveToNext()) {
                whitelistItems.add(it.bindToWhitelistItem())
            }
        }

        return@fromCallable whitelistItems
    }

    override fun whitelistItemForUrl(url: String): Maybe<WhitelistItem> = Maybe.fromCallable {
        database.query(TABLE_WHITELIST, null, "$KEY_URL=?",
                arrayOf(url), null, null, "$KEY_CREATED DESC", "1").use {
            if (it.moveToFirst()) {
                return@fromCallable it.bindToWhitelistItem()
            }
        }

        return@fromCallable null
    }

    override fun addWhitelistItem(whitelistItem: WhitelistItem): Completable = Completable.fromAction {
        val values = ContentValues().apply {
            put(KEY_URL, whitelistItem.url)
            put(KEY_CREATED, whitelistItem.timeCreated)
        }
        database.insert(TABLE_WHITELIST, null, values)
    }

    override fun removeWhitelistItem(whitelistItem: WhitelistItem): Completable = Completable.fromAction {
        database.delete(TABLE_WHITELIST, "$KEY_URL = ?", arrayOf(whitelistItem.url))
    }

    override fun clearWhitelist(): Completable = Completable.fromAction {
        database.run {
            delete(TABLE_WHITELIST, null, null)
            close()
        }
    }

    companion object {

        // Database version
        private const val DATABASE_VERSION = 1

        // Database name
        private const val DATABASE_NAME = "whitelistManager"

        // WhitelistItems table name
        private const val TABLE_WHITELIST = "whitelist"

        // WhitelistItems table columns names
        private const val KEY_ID = "id"
        private const val KEY_URL = "url"
        private const val KEY_CREATED = "created"

    }
}
