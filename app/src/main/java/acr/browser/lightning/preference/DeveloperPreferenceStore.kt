package acr.browser.lightning.preference

import acr.browser.lightning.preference.datastore.NonNullPreferenceStore
import android.app.Application
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preferences related to development debugging.
 */
@Singleton
class DeveloperPreferenceStore @Inject constructor(
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
                    set(keyUseLeakCanary, sharedPreferences.getBoolean(LEAK_CANARY, false))
                }
            }
        ),
        produceFile = {
            application.preferencesDataStoreFile(FILE_NAME)
        }
    )

    private val keyUseLeakCanary = booleanPreferencesKey(LEAK_CANARY)
    // private val keyCheckedForTor = booleanPreferencesKey(INITIAL_CHECK_FOR_TOR)
    // private val keyCheckedForI2P = booleanPreferencesKey(INITIAL_CHECK_FOR_I2P)

    val useLeakCanary = NonNullPreferenceStore(
        keyUseLeakCanary,
        dataStore,
        false
    )

    // val checkedForTor = NonNullPreferenceStore(
    //     keyCheckedForTor,
    //     dataStore,
    //     false
    // )

    // val checkedForI2P = NonNullPreferenceStore(
    //     keyCheckedForI2P,
    //     dataStore,
    //     false
    // )

    // var checkedForI2P by preferences.booleanPreference(INITIAL_CHECK_FOR_I2P, false)

    companion object {
        private const val FILE_NAME = "developer_settings"

        private const val LEAK_CANARY = "leakCanary"
        // private const val INITIAL_CHECK_FOR_TOR = "checkForTor"
        // private const val INITIAL_CHECK_FOR_I2P = "checkForI2P"
    }
}
