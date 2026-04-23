package acr.browser.lightning.database.adblock

import acr.browser.lightning.preference.datastore.NullablePreferenceStore
import acr.browser.lightning.preference.datastore.PreferenceStore
import android.app.Application
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Information about the contents of the hosts repository.
 */
@Singleton
class HostsPreferenceStore @Inject constructor(
    private val application: Application
) {

    private val dataStore = PreferenceDataStoreFactory.create(
        migrations = listOf(
            SharedPreferencesMigration(
                produceSharedPreferences = {
                    application.getSharedPreferences(FILE_NAME, 0)
                },
            ) { sharedPreferences: SharedPreferencesView, preferences: Preferences ->
                preferences.toMutablePreferences().apply {
                    sharedPreferences.getString(IDENTITY)
                        ?.let { set(keyIdentity, it) }
                }
            }
        ),
        produceFile = {
            application.preferencesDataStoreFile(FILE_NAME)
        }
    )

    private val keyIdentity = stringPreferencesKey(IDENTITY)

    /**
     * The identity of the contents of the hosts repository as a [String] or `null`.
     */
    val identity: PreferenceStore<String?> = NullablePreferenceStore(
        keyIdentity,
        dataStore
    )

    companion object {
        private const val FILE_NAME = "ad_block_settings"
        private const val IDENTITY = "identity"
    }
}
