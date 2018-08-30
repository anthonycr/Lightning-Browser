package acr.browser.lightning.preference.delegates

import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


/**
 * A [String] delegate that is backed by [SharedPreferences].
 */
private class StringPreferenceDelegate(
    private val name: String,
    private val defaultValue: String,
    private val preferences: SharedPreferences
) : ReadWriteProperty<Any, String> {
    override fun getValue(thisRef: Any, property: KProperty<*>): String =
        preferences.getString(name, defaultValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: String) {
        preferences.edit().putString(name, value).apply()
    }
}

/**
 * Creates a [String] from [SharedPreferences] with the provided arguments.
 */
fun SharedPreferences.stringPreference(
    name: String,
    defaultValue: String
): ReadWriteProperty<Any, String> = StringPreferenceDelegate(name, defaultValue, this)
