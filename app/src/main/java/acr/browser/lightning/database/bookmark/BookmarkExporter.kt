package acr.browser.lightning.database.bookmark

import acr.browser.lightning.R
import acr.browser.lightning.bookmark.LegacyBookmarkImporter
import acr.browser.lightning.bookmark.NetscapeBookmarkFormatImporter
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.asFolder
import acr.browser.lightning.extensions.fileName
import acr.browser.lightning.extensions.fileOutputStream
import acr.browser.lightning.log.Logger
import acr.browser.lightning.utils.Utils
import android.app.Application
import android.net.Uri
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter
import javax.inject.Inject

class BookmarkExporter @Inject constructor(
    private val application: Application,
    private val bookmarkRepository: BookmarkRepository,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val logger: Logger,
    private val netscapeBookmarkFormatImporter: NetscapeBookmarkFormatImporter,
    private val legacyBookmarkImporter: LegacyBookmarkImporter,
) {

    /**
     * Retrieves all the default bookmarks stored
     * in the raw file within assets.
     *
     * @param context the context necessary to open assets.
     * @return a non null list of the bookmarks stored in assets.
     */
    suspend fun importBookmarksFromAssets(): List<Bookmark.Entry> =
        withContext(coroutineDispatchers.io) {
            val bookmarks: MutableList<Bookmark.Entry> = mutableListOf()
            var inputStream: InputStream? = null
            try {
                inputStream = application.resources.openRawResource(R.raw.default_bookmarks)
                inputStream.bufferedReader().lines().forEach { line ->
                    try {
                        val jsonObject = JSONObject(line)
                        val folderTitle = jsonObject.getString(KEY_FOLDER)
                        bookmarks.add(
                            Bookmark.Entry(
                                jsonObject.getString(KEY_URL),
                                jsonObject.getString(KEY_TITLE),
                                jsonObject.getInt(KEY_ORDER),
                                folderTitle.asFolder()
                            )
                        )
                    } catch (e: JSONException) {
                        logger.log(TAG, "Can't parse line $line", e)
                    }
                }
            } catch (e: IOException) {
                logger.log(TAG, "Error reading the bookmarks file", e)
            } finally {
                Utils.close(inputStream)
            }

            bookmarks
        }

    /**
     * Exports the list of bookmarks to a file URI.
     */
    suspend fun exportBookmarksToUri(uri: Uri): String? = withContext(coroutineDispatchers.io) {
        val fileName = application.fileName(uri)
        val outputStream = application.fileOutputStream(uri, coroutineDispatchers.io)
            ?: return@withContext null
        val bookmarkList = bookmarkRepository.getAllBookmarksSorted()
        var bookmarkWriter: BufferedWriter? = null
        try {
            bookmarkWriter = BufferedWriter(OutputStreamWriter(outputStream))

            val jsonObject = JSONObject()
            for ((url, title, position, folder) in bookmarkList) {
                jsonObject.put(KEY_TITLE, title)
                jsonObject.put(KEY_URL, url)
                jsonObject.put(KEY_FOLDER, folder.title)
                jsonObject.put(KEY_ORDER, position)
                bookmarkWriter.write(jsonObject.toString())
                bookmarkWriter.newLine()
            }
            return@withContext fileName
        } catch (ioException: IOException) {
            logger.log(TAG, "onError: exporting bookmarks", ioException)
            return@withContext null
        } finally {
            Utils.close(bookmarkWriter)
        }
    }

    /**
     * Attempts to import bookmarks from a file URI.
     */
    suspend fun importBookmarksFromUri(uri: Uri): List<Bookmark.Entry>? =
        withContext(coroutineDispatchers.io) {
            try {
                val fileName = application.fileName(uri)
                if (fileName?.endsWith(EXTENSION_HTML) == true) {
                    netscapeBookmarkFormatImporter.importBookmarks(uri)
                } else {
                    legacyBookmarkImporter.importBookmarks(uri)
                }
            } catch (ioException: IOException) {
                logger.log(TAG, "onError: importing bookmarks", ioException)
                null
            }
        }


    private companion object {
        private const val TAG = "BookmarkExporter"

        private const val EXTENSION_HTML = "html"

        private const val KEY_URL = "url"
        private const val KEY_TITLE = "title"
        private const val KEY_FOLDER = "folder"
        private const val KEY_ORDER = "order"
    }
}
