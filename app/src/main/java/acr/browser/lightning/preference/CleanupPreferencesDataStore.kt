package acr.browser.lightning.preference

import acr.browser.lightning.preference.datastore.NullablePreferenceStore
import acr.browser.lightning.preference.datastore.PreferenceStore
import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import javax.inject.Inject

/**
 * Store for [acr.browser.lightning.migration.Cleanup] that knows the last installed app version.
 */
class CleanupPreferencesDataStore @Inject constructor(
    private val application: Application
) {

    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = {
            application.preferencesDataStoreFile(FILE_NAME)
        }
    )

    /**
     * The last installed app version or null if not known (either too old of a version or a fresh
     * install).
     */
    val lastInstalledVersion: PreferenceStore<Int?> = NullablePreferenceStore(
        key = intPreferencesKey("last_installed_version"),
        dataStore = dataStore,
    )

    companion object {
        private const val FILE_NAME = "cleanup_preferences"
    }
}
