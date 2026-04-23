package acr.browser.lightning.preference.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first

/**
 * Holds a persistable preference of type [T].
 */
interface PreferenceStore<T> {

    /**
     * Get the current value of the preference.
     */
    suspend fun get(): T

    /**
     * Set the [newValue] of the preference.
     */
    suspend fun set(newValue: T)

}

/**
 * A nullable implementation [PreferenceStore] backed by [DataStore].
 */
class NullablePreferenceStore<T>(
    private val key: Preferences.Key<T>,
    private val dataStore: DataStore<Preferences>
) : PreferenceStore<T?> {
    override suspend fun get(): T? = dataStore.data.first()[key]

    override suspend fun set(newValue: T?) {
        dataStore.updateData {
            it.toMutablePreferences().apply {
                if (newValue == null) {
                    remove(key)
                } else {
                    set(key, newValue)
                }
            }
        }
    }
}

/**
 * A non-null implementation [PreferenceStore] backed by [DataStore].
 */
class NonNullPreferenceStore<T>(
    private val key: Preferences.Key<T>,
    private val dataStore: DataStore<Preferences>,
    private val defaultValue: T
) : PreferenceStore<T> {
    override suspend fun get(): T = dataStore.data.first()[key] ?: defaultValue

    override suspend fun set(newValue: T) {
        dataStore.updateData {
            it.toMutablePreferences().apply {
                set(key, newValue)
            }
        }
    }
}
