package acr.browser.lightning.preference

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A delegate that adapts a [Preference] to a field of type [T].
 */
class PreferenceDelegate<T>(private val preference: Preference<T>) : ReadWriteProperty<Any, T> {

    override fun getValue(thisRef: Any, property: KProperty<*>) = preference.value()

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) = preference.edit(value)

}