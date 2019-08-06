package acr.browser.lightning.database.adblock

import acr.browser.lightning.database.databaseDelegate
import acr.browser.lightning.extensions.safeUse
import acr.browser.lightning.extensions.useMap
import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A database that holds hosts, backed by SQLite.
 */
@Singleton
class HostsDatabase @Inject constructor(
    application: Application
) : SQLiteOpenHelper(application, DATABASE_NAME, null, DATABASE_VERSION), HostsRepository {

    private val database: SQLiteDatabase by databaseDelegate()

    // Creating Tables
    override fun onCreate(db: SQLiteDatabase) {
        val createHostsTable = "CREATE TABLE ${DatabaseUtils.sqlEscapeString(TABLE_HOSTS)}(" +
            "${DatabaseUtils.sqlEscapeString(KEY_ID)} INTEGER PRIMARY KEY," +
            "${DatabaseUtils.sqlEscapeString(KEY_NAME)} TEXT" +
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

    override fun addHosts(hosts: List<Host>): Completable = Completable.create {
        database.apply {
            beginTransaction()

            for (item in hosts) {
                if (it.isDisposed) {
                    endTransaction()
                    it.onComplete()
                    return@apply
                }
                database.insert(TABLE_HOSTS, null, item.toContentValues())
            }

            setTransactionSuccessful()
            endTransaction()
        }
        it.onComplete()
    }

    override fun removeAllHosts(): Completable = Completable.fromCallable {
        database.run {
            delete(TABLE_HOSTS, null, null)
            close()
        }
    }

    override fun containsHost(host: Host): Boolean {
        database.query(
            TABLE_HOSTS,
            null,
            "$KEY_NAME=?",
            arrayOf(host.name),
            null,
            null,
            "1"
        ).safeUse {
            return it.moveToFirst()
        }

        return false
    }

    override fun hasHosts(): Boolean = DatabaseUtils.queryNumEntries(database, TABLE_HOSTS) > 0

    override fun allHosts(): Single<List<Host>> = Single.fromCallable {
        return@fromCallable database.query(
            TABLE_HOSTS,
            null,
            null,
            null,
            null,
            null,
            "$KEY_ID DESC"
        ).useMap { it.bindToHost() }
    }

    /**
     * Maps the fields of [Host] to [ContentValues].
     */
    private fun Host.toContentValues() = ContentValues(3).apply {
        put(KEY_NAME, name)
    }

    /**
     * Binds a [Cursor] to a single [Host].
     */
    private fun Cursor.bindToHost() = Host(
        name = getString(getColumnIndex(KEY_NAME))
    )

    companion object {

        // Database version
        private const val DATABASE_VERSION = 1

        // Database name
        private const val DATABASE_NAME = "hostsDatabase"

        // Host table name
        private const val TABLE_HOSTS = "hosts"

        // Host table columns names
        private const val KEY_ID = "id"
        private const val KEY_NAME = "url"
    }

}
