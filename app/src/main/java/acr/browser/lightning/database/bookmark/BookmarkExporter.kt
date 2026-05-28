package acr.browser.lightning.database.bookmark

import acr.browser.lightning.R
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.asFolder
import acr.browser.lightning.utils.Preconditions
import acr.browser.lightning.utils.Utils
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter

/**
 * The class responsible for importing and exporting bookmarks in the JSON format.
 */
object BookmarkExporter {
    private const val TAG = "BookmarkExporter"

    private const val KEY_URL = "url"
    private const val KEY_TITLE = "title"
    private const val KEY_FOLDER = "folder"
    private const val KEY_ORDER = "order"

    /**
     * Retrieves all the default bookmarks stored
     * in the raw file within assets.
     * 
     * @param context the context necessary to open assets.
     * @return a non null list of the bookmarks stored in assets.
     */
    suspend fun importBookmarksFromAssets(
        context: Context,
        coroutineDispatcher: CoroutineDispatcher,
    ): List<Bookmark.Entry> = withContext(coroutineDispatcher) {
        val bookmarks: MutableList<Bookmark.Entry> = mutableListOf()
        var inputStream: InputStream? = null
        try {
            inputStream = context.resources.openRawResource(R.raw.default_bookmarks)
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
                    Log.e(TAG, "Can't parse line $line", e)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading the bookmarks file", e)
        } finally {
            Utils.close(inputStream)
        }

        bookmarks
    }

    /**
     * Exports the list of bookmarks to an output stream.
     * 
     * @param bookmarkList the bookmarks to export.
     * @param outputStream the output stream to output to.
     * @return an observable that emits a completion
     * event when the export is complete, or an error
     * event if there is a problem.
     */
    suspend fun exportBookmarksToOutputStream(
        bookmarkList: List<Bookmark.Entry>,
        outputStream: OutputStream,
        coroutineDispatcher: CoroutineDispatcher,
    ): Unit = withContext(coroutineDispatcher) {
        Preconditions.checkNonNull(bookmarkList)
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
        } finally {
            Utils.close(bookmarkWriter)
        }
    }

    /**
     * Attempts to import bookmarks from the
     * given file. If the file is not in a
     * supported format, it will fail.
     * 
     * @param inputStream The stream to import from.
     * @return A list of bookmarks, or throws an exception if the bookmarks cannot be imported.
     */
    @Throws(Exception::class)
    fun importBookmarksFromFileStream(inputStream: InputStream): MutableList<Bookmark.Entry> {
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

            return bookmarks
        } finally {
            Utils.close(inputStream)
        }
    }
}
