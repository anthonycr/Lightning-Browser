package acr.browser.lightning.extensions

import android.database.Cursor

/**
 * Map the cursor to a [List] of [T], passing the cursor back into the [block] for convenience.
 */
inline fun <T> Cursor.map(block: (Cursor) -> T): List<T> {
    val outputList = mutableListOf<T>()
    while (moveToNext()) {
        outputList.add(block(this))
    }
    return outputList
}
