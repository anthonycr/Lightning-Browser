package acr.browser.lightning.extensions

import org.json.JSONArray

/**
 * Map each item in a [JSONArray] to a list of a new type.
 */
inline fun <T> JSONArray.map(map: (Any) -> T): List<T> = (0 until length()).map { map(this[it]) }
