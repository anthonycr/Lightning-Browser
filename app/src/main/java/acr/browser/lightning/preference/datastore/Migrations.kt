package acr.browser.lightning.preference.datastore

import acr.browser.lightning.preference.IntEnum
import androidx.datastore.migrations.SharedPreferencesView
import androidx.datastore.preferences.core.MutablePreferences

/**
 * Migrate a boolean shared preference to preferences.
 */
fun Pair<MutablePreferences, SharedPreferencesView>.migrateBoolean(
    preferenceStore: NonNullPreferenceStore<Boolean>
) {
    val (mutablePreference, sharedPreferences) = this
    val key = preferenceStore.key
    val defaultValue = preferenceStore.defaultValue
    mutablePreference[key] = sharedPreferences.getBoolean(key.name, defaultValue)
}

/**
 * Migrate a string shared preference to preferences.
 */
fun Pair<MutablePreferences, SharedPreferencesView>.migrateString(
    preferenceStore: NonNullPreferenceStore<String>
) {
    val (mutablePreference, sharedPreferences) = this
    val key = preferenceStore.key
    val defaultValue = preferenceStore.defaultValue
    mutablePreference[key] = sharedPreferences.getString(key.name, defaultValue)!!
}

/**
 * Migrate a nullable string shared preference to preferences.
 */
fun Pair<MutablePreferences, SharedPreferencesView>.migrateNullableString(
    preferenceStore: NullablePreferenceStore<String>
) {
    val (mutablePreference, sharedPreferences) = this
    val key = preferenceStore.key
    sharedPreferences.getString(key.name)?.let { mutablePreference[key] = it }
}

/**
 * Migrate an int shared preference to preferences.
 */
fun Pair<MutablePreferences, SharedPreferencesView>.migrateInt(
    preferenceStore: NonNullPreferenceStore<Int>
) {
    val (mutablePreference, sharedPreferences) = this
    val key = preferenceStore.key
    val defaultValue = preferenceStore.defaultValue
    mutablePreference[key] = sharedPreferences.getInt(key.name, defaultValue)
}

/**
 * Migrate an enum shared preference to preferences.
 */
fun <T> Pair<MutablePreferences, SharedPreferencesView>.migrateEnum(
    preferenceStore: EnumPreferenceStore<T>
) where T : Enum<T>, T : IntEnum {
    val (mutablePreference, sharedPreferences) = this
    val key = preferenceStore.key
    val defaultValue = preferenceStore.defaultValue
    mutablePreference[key] = sharedPreferences.getInt(key.name, defaultValue.value)
}
