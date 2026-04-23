package acr.browser.lightning.database.adblock

import acr.browser.lightning.browser.di.DatabaseScheduler
import acr.browser.lightning.database.databaseDelegate
import acr.browser.lightning.extensions.safeUse
import acr.browser.lightning.extensions.useMap
import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentValues
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A database that holds hosts, backed by SQLite.
 */
@SuppressLint("Range")
@Singleton
class HostsDatabase @Inject constructor(
    application: Application,
    @DatabaseScheduler
    private val databaseDispatcher: CoroutineDispatcher
) : SQLiteOpenHelper(application, DATABASE_NAME, null, DATABASE_VERSION), HostsRepository {

    private val database: SQLiteDatabase by databaseDelegate()

    // Creating Tables
    override fun onCreate(db: SQLiteDatabase) {
        val createHostsTable = "CREATE TABLE ${DatabaseUtils.sqlEscapeString(TABLE_HOSTS)}(" +
            "${DatabaseUtils.sqlEscapeString(KEY_NAME)} TEXT PRIMARY KEY" +
            ')'
        db.execSQL(createHostsTable)
    }

    // Upgrading database
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop older table if it exists
        db.execSQL("DROP TABLE IF EXISTS ${DatabaseUtils.sqlEscapeString(TABLE_HOSTS)}")
        // Create tables again
        onCreate(db)
    }

    override suspend fun addHosts(hosts: List<Host>): Unit =
        withContext(NonCancellable + databaseDispatcher) {
            database.apply {
                beginTransaction()

                for (item in hosts) {
                    database.insertWithOnConflict(
                        TABLE_HOSTS,
                        null,
                        item.toContentValues(),
                        SQLiteDatabase.CONFLICT_IGNORE
                    )
                }

                setTransactionSuccessful()
                endTransaction()
            }
        }

    override suspend fun removeAllHosts(): Unit = withContext(NonCancellable + databaseDispatcher) {
        database.run {
            delete(TABLE_HOSTS, null, null)
            close()
        }
    }

    override fun containsHost(host: Host): Boolean {
        database.query(
            TABLE_HOSTS,
            arrayOf(KEY_NAME),
            "$KEY_NAME=?",
            arrayOf(host.name),
            null,
            null,
            null,
            "1"
        ).safeUse {
            return it.moveToFirst()
        }

        return false
    }

    override fun hasHosts(): Boolean = DatabaseUtils.queryNumEntries(database, TABLE_HOSTS) > 0

    override suspend fun allHosts(): List<Host> = withContext(NonCancellable + databaseDispatcher) {
        database.query(
            TABLE_HOSTS,
            null,
            null,
            null,
            null,
            null,
            null
        ).useMap {
            Host(name = it.getString(it.getColumnIndex(KEY_NAME)))
        }
    }

    /**
     * Maps the fields of [Host] to [ContentValues].
     */
    private fun Host.toContentValues() = ContentValues(3).apply {
        put(KEY_NAME, name)
    }

    companion object {

        // Database version
        private const val DATABASE_VERSION = 2

        // Database name
        private const val DATABASE_NAME = "hostsDatabase"

        // Host table name
        private const val TABLE_HOSTS = "hosts"

        // Host table columns names
        private const val KEY_NAME = "url"
    }

}
