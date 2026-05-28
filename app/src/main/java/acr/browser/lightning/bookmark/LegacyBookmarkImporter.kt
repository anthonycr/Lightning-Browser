package acr.browser.lightning.bookmark

import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.asFolder
import acr.browser.lightning.database.bookmark.BookmarkExporter
import acr.browser.lightning.extensions.fileInputStream
import acr.browser.lightning.utils.Utils
import android.app.Application
import android.net.Uri
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

/**
 * A [BookmarkImporter] that imports bookmark files that were produced by [BookmarkExporter].
 */
class LegacyBookmarkImporter @Inject constructor(
    private val application: Application,
    private val coroutineDispatchers: CoroutineDispatchers
) : BookmarkImporter {

    override suspend fun importBookmarks(uri: Uri): List<Bookmark.Entry>? =
        withContext(coroutineDispatchers.io) {
            val inputStream = application.fileInputStream(uri) ?: return@withContext null
            try {
                val bookmarks: MutableList<Bookmark.Entry> = mutableListOf()
                inputStream.bufferedReader().lines().forEach { line ->
                    val jsonObject = JSONObject(line)
                    val folderName = jsonObject.getString(KEY_FOLDER)
                    val entry = Bookmark.Entry(
                        jsonObject.getString(KEY_URL),
                        jsonObject.getString(KEY_TITLE),
                        jsonObject.getInt(KEY_ORDER),
                        folderName.asFolder()
                    )
                    bookmarks.add(entry)
                }

                bookmarks
            } finally {
                Utils.close(inputStream)
            }
        }

    companion object {

        private const val KEY_URL = "url"
        private const val KEY_TITLE = "title"
        private const val KEY_FOLDER = "folder"
        private const val KEY_ORDER = "order"
    }

}
