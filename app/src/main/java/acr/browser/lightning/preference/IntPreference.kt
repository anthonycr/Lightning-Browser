package acr.browser.lightning.preference

import android.content.SharedPreferences

/**
 * A [Preference] of type [Int].
 */
class IntPreference(
        private val name: String,
        private val defaultValue: Int,
        private val preferences: SharedPreferences
) : Preference<Int> {

    override fun edit(value: Int) = preferences.edit().putInt(name, value).apply()

    override fun value(): Int = preferences.getInt(name, defaultValue)

}