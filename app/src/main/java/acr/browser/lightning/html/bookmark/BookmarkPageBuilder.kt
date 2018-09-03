package acr.browser.lightning.html.bookmark

import acr.browser.lightning.R
import acr.browser.lightning.constant.FILE
import acr.browser.lightning.database.HistoryItem
import acr.browser.lightning.favicon.FaviconModel
import acr.browser.lightning.favicon.toValidUri
import android.app.Application
import com.anthonycr.mezzanine.MezzanineGenerator
import io.reactivex.Scheduler
import org.jsoup.Jsoup
import java.io.File

/**
 * A builder for the bookmark page.
 */
internal class BookmarkPageBuilder(
    private val faviconModel: FaviconModel,
    private val app: Application,
    private val diskScheduler: Scheduler
) {

    private data class BookmarkViewModel(val title: String, val url: String, val iconUrl: String)

    companion object {
        private const val FOLDER_ICON = "folder.png"
        private const val DEFAULT_ICON = "default.png"
        private const val FILENAME = "bookmarks.html"
    }

    private val folderIconPath = getFaviconFile(app).toString()

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

        val pageContents = bookmarkPageReader.provideHtml()

        val document = Jsoup.parse(pageContents).apply {
            title(app.getString(R.string.action_bookmarks))
        }

        val body = document.body()
        val repeatableElement = body.getElementById("repeated")
        val container = body.getElementById("content")
        repeatableElement.remove()

        bookmarkList.forEach {
            val newElement = repeatableElement.clone()

            val viewModel = if (it.isFolder) {
                createViewModelForFolder(it)
            } else {
                createViewModelForBookmark(it)
            }

            newElement.getElementsByTag("a").first().attr("href", viewModel.url)
            newElement.getElementsByTag("img").first().attr("src", viewModel.iconUrl)
            newElement.getElementById("title").appendText(viewModel.title)
            container.appendChild(newElement)
        }

        return document.outerHtml()
    }

    private fun createViewModelForFolder(historyItem: HistoryItem): BookmarkViewModel {
        val folderPage = getBookmarkPage(app, historyItem.title)
        val iconUrl = folderIconPath
        val url = "$FILE$folderPage"

        return BookmarkViewModel(
            title = historyItem.title,
            url = url,
            iconUrl = iconUrl
        )
    }

    private fun createViewModelForBookmark(historyItem: HistoryItem): BookmarkViewModel {
        val bookmarkUri = historyItem.url.toValidUri()

        val iconUrl = if (bookmarkUri != null) {
            val faviconFile = FaviconModel.getFaviconCacheFile(app, bookmarkUri)
            if (!faviconFile.exists()) {
                val defaultFavicon = faviconModel.getDefaultBitmapForString(historyItem.title)
                faviconModel.cacheFaviconForUrl(defaultFavicon, historyItem.url)
                    .subscribeOn(diskScheduler)
                    .subscribe()
            }

            "$FILE$faviconFile"
        } else {
            "$FILE${getDefaultIconFile(app)}"
        }

        return BookmarkViewModel(
            title = historyItem.title,
            url = historyItem.url,
            iconUrl = iconUrl
        )
    }

}
