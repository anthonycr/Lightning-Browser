package acr.browser.lightning.preference.delegates

import acr.browser.lightning.preference.IntEnum
import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * An [Enum] delegate that is backed by [SharedPreferences].
 */
class EnumPreference<T>(
    name: String,
    private val defaultValue: T,
    private val clazz: Class<T>,
    preferences: SharedPreferences
) : ReadWriteProperty<Any, T> where T : Enum<T>, T : IntEnum {

    private var backingInt: Int by preferences.intPreference(name, defaultValue.value)

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return clazz.enumConstants.first { it.value == backingInt } ?: defaultValue
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        backingInt = value.value
    }

}

/**
 * Creates a [T] enum from [SharedPreferences] with the provide arguments.
 */
inline fun <reified T> SharedPreferences.enumPreference(
    name: String,
    defaultValue: T
): ReadWriteProperty<Any, T> where T : Enum<T>, T : IntEnum = EnumPreference(
    name,
    defaultValue,
    T::class.java,
    this
)
