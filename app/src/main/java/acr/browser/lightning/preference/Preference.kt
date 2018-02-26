package acr.browser.lightning.preference

/**
 * A user preference of a specified type [T].
 */
interface Preference<T> {

    /**
     * Edit the preference with the new [value].
     */
    fun edit(value: T)

    /**
     * Return the value held by this preference.
     */
    fun value(): T

}

/**
 * Create a [PreferenceDelegate] from the [Preference].
 */
internal fun <T> Preference<T>.delegate() = PreferenceDelegate(this)
