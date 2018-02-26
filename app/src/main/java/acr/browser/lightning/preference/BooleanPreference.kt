package acr.browser.lightning.preference

import android.content.SharedPreferences

/**
 * A [Preference] of type [Boolean].
 */
class BooleanPreference(
        private val name: String,
        private val defaultValue: Boolean,
        private val preferences: SharedPreferences
) : Preference<Boolean> {

    override fun edit(value: Boolean) = preferences.edit().putBoolean(name, value).apply()

    override fun value() = preferences.getBoolean(name, defaultValue)

}