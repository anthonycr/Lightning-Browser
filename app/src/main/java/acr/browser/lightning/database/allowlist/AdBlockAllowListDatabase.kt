package acr.browser.lightning.database.allowlist

import acr.browser.lightning.browser.di.DatabaseScheduler
import acr.browser.lightning.database.databaseDelegate
import acr.browser.lightning.extensions.firstOrNullMap
import acr.browser.lightning.extensions.useMap
import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.annotation.WorkerThread
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The disk backed ad block allow list database. See [AdBlockAllowListRepository] for function
 * documentation.s
 */
@Singleton
@WorkerThread
class AdBlockAllowListDatabase @Inject constructor(
    application: Application,
    @DatabaseScheduler
    private val databaseDispatcher: CoroutineDispatcher,
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

    override suspend fun allAllowListItems(): List<AllowListEntry> =
        withContext(databaseDispatcher) {
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

    override suspend fun allowListItemForUrl(
        domain: String
    ): AllowListEntry? = withContext(databaseDispatcher) {
        database.query(
            TABLE_WHITELIST,
            null,
            "$KEY_URL=?",
            arrayOf(domain), null,
            null,
            "$KEY_CREATED DESC",
            "1"
        ).firstOrNullMap { it.bindToAllowListItem() }
    }

    override suspend fun addAllowListItem(
        whitelistItem: AllowListEntry
    ): Unit = withContext(databaseDispatcher) {
        val values = ContentValues().apply {
            put(KEY_URL, whitelistItem.domain)
            put(KEY_CREATED, whitelistItem.timeCreated)
        }
        database.insert(TABLE_WHITELIST, null, values)
    }

    override suspend fun removeAllowListItem(
        whitelistItem: AllowListEntry
    ): Unit = withContext(databaseDispatcher) {
        database.delete(TABLE_WHITELIST, "$KEY_URL = ?", arrayOf(whitelistItem.domain))
    }

    override suspend fun clearAllowList(): Unit = withContext(databaseDispatcher) {
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
