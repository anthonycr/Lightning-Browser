package acr.browser.lightning.preference.delegates

import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


/**
 * A [String] delegate that is backed by [SharedPreferences].
 */
private class NullableStringPreferenceDelegate(
    private val name: String,
    private val defaultValue: String? = null,
    private val preferences: SharedPreferences
) : ReadWriteProperty<Any, String?> {
    override fun getValue(thisRef: Any, property: KProperty<*>): String? =
        preferences.getString(name, defaultValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: String?) {
        preferences.edit().putString(name, value).apply()
    }
}

/**
 * Creates a [String] from [SharedPreferences] with the provided arguments.
 */
fun SharedPreferences.nullableStringPreference(
    name: String,
    defaultValue: String? = null
): ReadWriteProperty<Any, String?> = NullableStringPreferenceDelegate(name, defaultValue, this)
