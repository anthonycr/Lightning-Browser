package acr.browser.lightning.extensions

import android.util.Log
import java.io.Closeable

/**
 * Close a [Closeable] and absorb any exceptions within [block], logging them when they occur.
 */
fun <T : Closeable> T.safeUse(block: (T) -> Unit) {
    try {
        this.use(block)
    } catch (throwable: Throwable) {
        Log.e("Closeable", "Unable to parse results", throwable)
    }
}