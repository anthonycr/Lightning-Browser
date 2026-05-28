package acr.browser.lightning.bookmark

import acr.browser.lightning.database.Bookmark
import android.net.Uri
import java.io.InputStream

/**
 * An importer that imports [Bookmark.Entry] from an [InputStream]. Supported formats are details of
 * the implementation.
 */
interface BookmarkImporter {

    /**
     * Converts a [Uri] to a [List] of [Bookmark.Entry], returns null if an error occurred.
     */
    suspend fun importBookmarks(uri: Uri): List<Bookmark.Entry>?

}
