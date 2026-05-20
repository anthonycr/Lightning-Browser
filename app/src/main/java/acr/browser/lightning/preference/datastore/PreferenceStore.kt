package acr.browser.lightning.preference.datastore

import acr.browser.lightning.preference.IntEnum
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

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
 * Convenience function for the [EnumPreferenceStore] constructor.
 */
inline fun <reified T> EnumPreferenceStore(
    key: Preferences.Key<Int>,
    dataStore: DataStore<Preferences>,
    defaultValue: T
): EnumPreferenceStore<T> where T : Enum<T>, T : IntEnum = EnumPreferenceStore(
    key = key,
    dataStore = dataStore,
    defaultValue = defaultValue,
    clazz = T::class.java
)

/**
 * An int backed implementation of a [PreferenceStore] for [Enum] types.
 */
class EnumPreferenceStore<T>(
    val key: Preferences.Key<Int>,
    private val dataStore: DataStore<Preferences>,
    val defaultValue: T,
    private val clazz: Class<T>
) : PreferenceStore<T> where T : Enum<T>, T : IntEnum {

    private val backingPreferenceStore = NonNullPreferenceStore(
        key = key,
        dataStore = dataStore,
        defaultValue = defaultValue.value
    )

    override suspend fun get(): T = clazz.enumConstants!!.first {
        it.value == backingPreferenceStore.get()
    } ?: defaultValue

    override suspend fun set(newValue: T) {
        backingPreferenceStore.set(newValue.value)
    }
}

/**
 * A nullable implementation [PreferenceStore] backed by [DataStore].
 */
class NullablePreferenceStore<T>(
    val key: Preferences.Key<T>,
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
    val key: Preferences.Key<T>,
    private val dataStore: DataStore<Preferences>,
    val defaultValue: T
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

@Deprecated("Use coroutines")
fun <T> PreferenceStore<T>.getUnsafe(): T = runBlocking {
    get()
}

@Deprecated("Use coroutines")
fun <T> PreferenceStore<T>.setUnsafe(value: T) = runBlocking {
    set(value)
}
