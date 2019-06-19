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

/**
 * Map the cursor to a [List] of [T], passing the cursor back into the [block] for convenience, and
 * then closing the cursor upon return.
 */
inline fun <T> Cursor.useMap(block: (Cursor) -> T): List<T> {
    use {
        val outputList = mutableListOf<T>()
        while (moveToNext()) {
            outputList.add(block(this))
        }
        return outputList
    }
}


/**
 * Return the first element returned by this cursor as [T] or null if there were no elements in the
 * cursor.
 */
inline fun <T> Cursor.firstOrNullMap(block: (Cursor) -> T): T? {
    return if (moveToFirst()) {
        return block(this)
    } else {
        null
    }
}
