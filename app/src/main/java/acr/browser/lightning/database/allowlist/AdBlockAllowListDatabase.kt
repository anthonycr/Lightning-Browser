package acr.browser.lightning.database.allowlist

import acr.browser.lightning.database.databaseDelegate
import acr.browser.lightning.extensions.firstOrNullMap
import acr.browser.lightning.extensions.useMap
import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.annotation.WorkerThread
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The disk backed ad block allow list database. See [AdBlockAllowListRepository] for function
 * documentation.s
 */
@Singleton
@WorkerThread
class AdBlockAllowListDatabase @Inject constructor(
    application: Application
) : SQLiteOpenHelper(application, DATABASE_NAME, null, DATABASE_VERSION),
    AdBlockAllowListRepository {

    private val database: SQLiteDatabase by databaseDelegate()

    // Creating Tables
    override fun onCreate(db: SQLiteDatabase) {
        val createAllowListTable = "CREATE TABLE $TABLE_WHITELIST(" +
            " $KEY_ID INTEGER PRIMARY KEY," +
            " $KEY_URL TEXT," +
            " $KEY_CREATED INTEGER" +
            ")"
        db.execSQL(createAllowListTable)
    }

    // Upgrading database
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop older table if it exists
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WHITELIST")
        // Create tables again
        onCreate(db)
    }

    private fun Cursor.bindToAllowListItem() = AllowListEntry(
        domain = getString(1),
        timeCreated = getLong(2)
    )

    override fun allAllowListItems(): Single<List<AllowListEntry>> = Single.fromCallable {
        database.query(
            TABLE_WHITELIST,
            null,
            null,
            null,
            null,
            null,
            "$KEY_CREATED DESC"
        ).useMap { it.bindToAllowListItem() }
    }

    override fun allowListItemForUrl(url: String): Maybe<AllowListEntry> = Maybe.fromCallable {
        database.query(
            TABLE_WHITELIST,
            null,
            "$KEY_URL=?",
            arrayOf(url), null,
            null,
            "$KEY_CREATED DESC",
            "1"
        ).firstOrNullMap { it.bindToAllowListItem() }
    }

    override fun addAllowListItem(whitelistItem: AllowListEntry): Completable =
        Completable.fromAction {
            val values = ContentValues().apply {
                put(KEY_URL, whitelistItem.domain)
                put(KEY_CREATED, whitelistItem.timeCreated)
            }
            database.insert(TABLE_WHITELIST, null, values)
        }

    override fun removeAllowListItem(whitelistItem: AllowListEntry): Completable =
        Completable.fromAction {
            database.delete(TABLE_WHITELIST, "$KEY_URL = ?", arrayOf(whitelistItem.domain))
        }

    override fun clearAllowList(): Completable = Completable.fromAction {
        database.run {
            delete(TABLE_WHITELIST, null, null)
            close()
        }
    }

    companion object {

        // Database version
        private const val DATABASE_VERSION = 1

        // Database name
        private const val DATABASE_NAME = "allowListManager"

        // AllowListItems table name
        private const val TABLE_WHITELIST = "allowList"

        // AllowListItems table columns names
        private const val KEY_ID = "id"
        private const val KEY_URL = "url"
        private const val KEY_CREATED = "created"

    }
}
