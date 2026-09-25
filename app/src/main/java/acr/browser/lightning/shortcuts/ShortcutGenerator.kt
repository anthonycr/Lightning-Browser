package acr.browser.lightning.shortcuts

import android.graphics.Bitmap

/**
 * Creates a shortcut if supported by the launcher.
 */
interface ShortcutGenerator {

    /**
     * Create a shortcut with the provided [url], [unsafeTitle], and [unsafeFavicon]. Returns true
     * if the launcher supports creating a shortcut, false otherwise.
     */
    fun createShortcut(url: String, unsafeTitle: String, unsafeFavicon: Bitmap?): Boolean
}
