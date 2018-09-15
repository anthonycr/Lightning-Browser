package acr.browser.lightning.preference.delegates

import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * An [Int] delegate that is backed by [SharedPreferences].
 */
private class IntPreferenceDelegate(
    private val name: String,
    private val defaultValue: Int,
    private val preferences: SharedPreferences
) : ReadWriteProperty<Any, Int> {
    override fun getValue(thisRef: Any, property: KProperty<*>): Int =
        preferences.getInt(name, defaultValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) {
        preferences.edit().putInt(name, value).apply()
    }

}

/**
 * Creates a [Boolean] from [SharedPreferences] with the provide arguments.
 */
fun SharedPreferences.intPreference(
    name: String,
    defaultValue: Int
): ReadWriteProperty<Any, Int> = IntPreferenceDelegate(name, defaultValue, this)
