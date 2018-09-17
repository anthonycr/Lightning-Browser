package acr.browser.lightning.html.bookmark

import acr.browser.lightning.R
import acr.browser.lightning.constant.FILE
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.favicon.FaviconModel
import acr.browser.lightning.favicon.toValidUri
import android.app.Application
import androidx.core.net.toUri
import com.anthonycr.mezzanine.MezzanineGenerator
import io.reactivex.Scheduler
import org.jsoup.Jsoup
import java.io.File

/**
 * A builder for the bookmark page.
 */
class BookmarkPageBuilder(
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

    fun buildPage(bookmarkList: List<Bookmark>): String {
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

            val viewModel = when (it) {
                is Bookmark.Folder -> createViewModelForFolder(it)
                is Bookmark.Entry -> createViewModelForBookmark(it)
            }

            newElement.getElementsByTag("a").first().attr("href", viewModel.url)
            newElement.getElementsByTag("img").first().attr("src", viewModel.iconUrl)
            newElement.getElementById("title").appendText(viewModel.title)
            container.appendChild(newElement)
        }

        return document.outerHtml()
    }

    private fun createViewModelForFolder(folder: Bookmark.Folder): BookmarkViewModel {
        val folderPage = getBookmarkPage(app, folder.title)
        val iconUrl = folderIconPath
        val url = "$FILE$folderPage"

        return BookmarkViewModel(
            title = folder.title,
            url = url,
            iconUrl = iconUrl
        )
    }

    private fun createViewModelForBookmark(entry: Bookmark.Entry): BookmarkViewModel {
        val bookmarkUri = entry.url.toUri().toValidUri()

        val iconUrl = if (bookmarkUri != null) {
            val faviconFile = FaviconModel.getFaviconCacheFile(app, bookmarkUri)
            if (!faviconFile.exists()) {
                val defaultFavicon = faviconModel.getDefaultBitmapForString(entry.title)
                faviconModel.cacheFaviconForUrl(defaultFavicon, entry.url)
                    .subscribeOn(diskScheduler)
                    .subscribe()
            }

            "$FILE$faviconFile"
        } else {
            "$FILE${getDefaultIconFile(app)}"
        }

        return BookmarkViewModel(
            title = entry.title,
            url = entry.url,
            iconUrl = iconUrl
        )
    }

}
