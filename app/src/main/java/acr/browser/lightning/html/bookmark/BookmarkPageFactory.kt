package acr.browser.lightning.html.bookmark

import acr.browser.lightning.R
import acr.browser.lightning.browser.theme.ThemeProvider
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.constant.FILE
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.extensions.safeUse
import acr.browser.lightning.favicon.FaviconModel
import acr.browser.lightning.favicon.toValidUri
import acr.browser.lightning.html.HtmlPageFactory
import acr.browser.lightning.html.jsoup.andBuild
import acr.browser.lightning.html.jsoup.body
import acr.browser.lightning.html.jsoup.clone
import acr.browser.lightning.html.jsoup.findId
import acr.browser.lightning.html.jsoup.id
import acr.browser.lightning.html.jsoup.parse
import acr.browser.lightning.html.jsoup.removeElement
import acr.browser.lightning.html.jsoup.style
import acr.browser.lightning.html.jsoup.tag
import acr.browser.lightning.html.jsoup.title
import acr.browser.lightning.utils.ThemeUtils
import android.app.Application
import android.graphics.Bitmap
import androidx.core.net.toUri
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import javax.inject.Inject

/**
 * Created by anthonycr on 9/23/18.
 */
class BookmarkPageFactory @Inject constructor(
    private val application: Application,
    private val bookmarkModel: BookmarkRepository,
    private val faviconModel: FaviconModel,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val bookmarkPageReader: BookmarkPageReader,
    private val themeProvider: ThemeProvider
) : HtmlPageFactory {

    private val title = application.getString(R.string.action_bookmarks)
    private val folderIconFile by lazy {
        File(FaviconModel.faviconCacheFolder(application), FOLDER_ICON)
    }
    private val defaultIconFile by lazy {
        File(FaviconModel.faviconCacheFolder(application), DEFAULT_ICON)
    }

    private fun Int.toColor(): String {
        val string = Integer.toHexString(this)

        return string.substring(2) + string.substring(0, 2)
    }

    private val backgroundColor: String
        get() = themeProvider.color(R.attr.colorPrimary).toColor()
    private val cardColor: String
        get() = themeProvider.color(R.attr.autoCompleteBackgroundColor).toColor()
    private val textColor: String
        get() = themeProvider.color(R.attr.autoCompleteTitleColor).toColor()

    override suspend fun buildPage(): String = withContext(coroutineDispatchers.io) {
        val bookmarks = bookmarkModel.getAllBookmarksSorted()
        bookmarks.groupBy { it.folder }
            .mapValues { (folder, bookmarks) ->
                if (folder == Bookmark.Folder.Root) {
                    construct((bookmarks + bookmarkModel.getFoldersSorted()).map { it.asViewModel() })
                } else {
                    construct(bookmarks.map { it.asViewModel() })
                }
            }.forEach { (folder, content) ->
                FileWriter(createBookmarkPage(folder), false).use {
                    it.write(content)
                }
            }

        cacheIcon(
            ThemeUtils.createThemedBitmap(
                application,
                R.drawable.ic_folder,
                themeProvider.color(R.attr.autoCompleteTitleColor)
            ),
            folderIconFile
        )
        cacheIcon(faviconModel.createDefaultBitmapForTitle(null), defaultIconFile)

        "$FILE${createBookmarkPage(null)}"
    }

    private fun cacheIcon(icon: Bitmap, file: File) = FileOutputStream(file).safeUse {
        icon.compress(Bitmap.CompressFormat.PNG, 100, it)
        icon.recycle()
    }

    private fun construct(list: List<BookmarkViewModel>): String {
        return parse(bookmarkPageReader.provideHtml()) andBuild {
            title { title }
            style { content ->
                content.replace("--body-bg: {COLOR}", "--body-bg: #$backgroundColor;")
                    .replace("--box-bg: {COLOR}", "--box-bg: #$cardColor;")
                    .replace("--box-txt: {COLOR}", "--box-txt: #$textColor;")
            }
            body {
                val repeatableElement = findId("repeated").removeElement()
                id("content") {
                    list.forEach { (title, url, iconUrl) ->
                        appendChild(repeatableElement.clone {
                            tag("a") { attr("href", url) }
                            tag("img") { attr("src", iconUrl) }
                            id("title") { appendText(title) }
                        })
                    }
                }
            }
        }
    }

    private suspend fun Bookmark.asViewModel(): BookmarkViewModel = when (this) {
        is Bookmark.Folder -> createViewModelForFolder(this)
        is Bookmark.Entry -> createViewModelForBookmark(this)
    }

    private fun createViewModelForFolder(folder: Bookmark.Folder): BookmarkViewModel {
        val folderPage = createBookmarkPage(folder)
        val url = "$FILE$folderPage"

        return BookmarkViewModel(
            title = folder.title,
            url = url,
            iconUrl = folderIconFile.toString()
        )
    }

    private suspend fun createViewModelForBookmark(entry: Bookmark.Entry): BookmarkViewModel {
        val bookmarkUri = entry.url.toUri().toValidUri()

        val iconUrl = if (bookmarkUri != null) {
            val faviconFile = FaviconModel.getFaviconCacheFile(application, bookmarkUri)
            if (!faviconFile.exists()) {
                val defaultFavicon = faviconModel.createDefaultBitmapForTitle(entry.title)
                faviconModel.cacheFaviconForUrl(defaultFavicon, entry.url)
            }

            faviconFile
        } else {
            defaultIconFile
        }

        return BookmarkViewModel(
            title = entry.title,
            url = entry.url,
            iconUrl = iconUrl.toString()
        )
    }

    /**
     * Create the bookmark page file.
     */
    fun createBookmarkPage(folder: Bookmark.Folder?): File {
        val prefix = if (folder?.title?.isNotBlank() == true) {
            "${folder.title}-"
        } else {
            ""
        }
        val generatedHtml = File(application.filesDir, "generated-html")
        generatedHtml.mkdirs()
        return File(generatedHtml, prefix + FILENAME)
    }

    companion object {

        const val FILENAME = "bookmarks.html"

        private const val FOLDER_ICON = "folder.png"
        private const val DEFAULT_ICON = "default.png"

    }
}
