package acr.browser.lightning.preference

import android.content.SharedPreferences

/**
 * A [Preference] of type [String].
 */
class StringPreference(
        private val name: String,
        private val defaultValue: String,
        private val preferences: SharedPreferences
) : Preference<String> {

    override fun edit(value: String) = preferences.edit().putString(name, value).apply()

    override fun value(): String = preferences.getString(name, defaultValue)

}