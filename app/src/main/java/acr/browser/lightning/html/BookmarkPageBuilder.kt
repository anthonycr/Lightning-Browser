package acr.browser.lightning.html

import acr.browser.lightning.R
import acr.browser.lightning.constant.Constants
import acr.browser.lightning.database.HistoryItem
import acr.browser.lightning.favicon.FaviconModel
import acr.browser.lightning.favicon.FaviconUtils
import android.app.Application
import com.anthonycr.mezzanine.MezzanineGenerator
import org.jsoup.Jsoup
import java.io.File

/**
 * A builder for the bookmark page.
 */
class BookmarkPageBuilder(private val faviconModel: FaviconModel,
                          private val app: Application) {

    private val FOLDER_ICON = "folder.png"
    private val DEFAULT_ICON = "default.png"
    private val FILENAME = "bookmarks.html"

    private fun getBookmarkPage(application: Application, folder: String?): File {
        val prefix = if (folder != null) "$folder-" else ""
        return File(application.filesDir, prefix + FILENAME)
    }

    private fun getFaviconFile(application: Application): File =
            File(application.cacheDir, FOLDER_ICON)

    private fun getDefaultIconFile(application: Application): File =
            File(application.cacheDir, DEFAULT_ICON)

    fun buildPage(bookmarkList: List<HistoryItem>): String {
        val bookmarkPageReader = MezzanineGenerator.BookmarkPageReader()

        val pageContents = bookmarkPageReader.provideString()

        val document = Jsoup.parse(pageContents)
        document.title(app.getString(R.string.action_bookmarks))

        val body = document.body()
        val repeatableElement = body.getElementById("repeated")
        val container = body.getElementById("content")
        repeatableElement.remove()

        val folderIconPath = getFaviconFile(app).toString()

        bookmarkList.forEach {
            val newElement = repeatableElement.clone()
            val iconUrl: String
            val url: String

            if (it.isFolder) {
                val folderPage = getBookmarkPage(app, it.title)
                iconUrl = folderIconPath
                url = "${Constants.FILE}$folderPage"
            } else {
                val bookmarkUri = FaviconUtils.safeUri(it.url)

                iconUrl = if (bookmarkUri != null) {
                    val faviconFile = FaviconModel.getFaviconCacheFile(app, bookmarkUri)
                    if (!faviconFile.exists()) {
                        val defaultFavicon = faviconModel.getDefaultBitmapForString(it.title)
                        faviconModel.cacheFaviconForUrl(defaultFavicon, it.url).subscribe()
                    }

                    "${Constants.FILE}$faviconFile"
                } else {
                    "${Constants.FILE}${getDefaultIconFile(app)}"
                }

                url = it.url
            }

            newElement.select("a").first().attr("href", url)
            newElement.select("img").first().attr("src", iconUrl)
            newElement.getElementById("title").appendText(it.title)
            container.appendChild(newElement)
        }

        return document.outerHtml()
    }

}