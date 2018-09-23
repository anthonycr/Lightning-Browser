package acr.browser.lightning.html.bookmark

import acr.browser.lightning.R
import acr.browser.lightning.constant.FILE
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.di.DatabaseScheduler
import acr.browser.lightning.di.DiskScheduler
import acr.browser.lightning.extensions.safeUse
import acr.browser.lightning.favicon.FaviconModel
import acr.browser.lightning.favicon.toValidUri
import acr.browser.lightning.html.HtmlPageFactory
import acr.browser.lightning.utils.ThemeUtils
import android.app.Application
import android.graphics.Bitmap
import androidx.core.net.toUri
import io.reactivex.Scheduler
import io.reactivex.Single
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
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
    @DatabaseScheduler private val databaseScheduler: Scheduler,
    @DiskScheduler private val diskScheduler: Scheduler,
    private val bookmarkPageReader: BookmarkPageReader
) : HtmlPageFactory {

    private val title = application.getString(R.string.action_bookmarks)
    private val folderIconFile = File(application.cacheDir, FOLDER_ICON)
    private val defaultIconFile = File(application.cacheDir, DEFAULT_ICON)

    override fun buildPage(): Single<String> = bookmarkModel
        .getAllBookmarksSorted()
        .flattenAsObservable { it }
        .groupBy(Bookmark.Entry::folder) { it as Bookmark }
        .flatMapSingle { bookmarksInFolder ->
            val folder = bookmarksInFolder.key
            return@flatMapSingle bookmarksInFolder
                .toList()
                .concatWith(
                    if (folder == Bookmark.Folder.Root) {
                        bookmarkModel.getFoldersSorted().map { it.filterIsInstance<Bookmark.Folder.Entry>() }
                    } else {
                        Single.just(emptyList())
                    }
                )
                .toList()
                .map { bookmarksAndFolders ->
                    Pair(folder, bookmarksAndFolders.flatten().map { it.asViewModel() })
                }
        }
        .map { (folder, viewModels) -> Pair(folder, construct(viewModels)) }
        .subscribeOn(databaseScheduler)
        .observeOn(diskScheduler)
        .doOnNext { (folder, content) ->
            FileWriter(createBookmarkPage(folder), false).use {
                it.write(content)
            }
        }
        .ignoreElements()
        .toSingle {
            cacheIcon(ThemeUtils.getThemedBitmap(application, R.drawable.ic_folder, false), folderIconFile)
            cacheIcon(faviconModel.getDefaultBitmapForString(null), defaultIconFile)

            "$FILE${createBookmarkPage(null)}"
        }

    private fun cacheIcon(icon: Bitmap, file: File) = FileOutputStream(file).safeUse {
        icon.compress(Bitmap.CompressFormat.PNG, 100, it)
        icon.recycle()
    }

    private data class BookmarkViewModel(val title: String, val url: String, val iconUrl: String)

    private fun construct(list: List<BookmarkViewModel>): String {
        return Jsoup.parse(bookmarkPageReader.provideHtml()).apply {
            title(title)
            body().also { body ->
                val repeatableElement = body.getElementById("repeated").also(Element::remove)
                body.getElementById("content").also { content ->
                    list.forEach {
                        content.appendChild(repeatableElement.clone().apply {
                            getElementsByTag("a").first().attr("href", it.url)
                            getElementsByTag("img").first().attr("src", it.iconUrl)
                            getElementById("title").appendText(it.title)
                        })
                    }
                }
            }
        }.outerHtml()
    }

    private fun Bookmark.asViewModel(): BookmarkViewModel = when (this) {
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

    private fun createViewModelForBookmark(entry: Bookmark.Entry): BookmarkViewModel {
        val bookmarkUri = entry.url.toUri().toValidUri()

        val iconUrl = if (bookmarkUri != null) {
            val faviconFile = FaviconModel.getFaviconCacheFile(application, bookmarkUri)
            if (!faviconFile.exists()) {
                val defaultFavicon = faviconModel.getDefaultBitmapForString(entry.title)
                faviconModel.cacheFaviconForUrl(defaultFavicon, entry.url)
                    .subscribeOn(diskScheduler)
                    .subscribe()
            }

            "$FILE$faviconFile"
        } else {
            "$FILE$defaultIconFile"
        }

        return BookmarkViewModel(
            title = entry.title,
            url = entry.url,
            iconUrl = iconUrl
        )
    }

    fun createBookmarkPage(folder: Bookmark.Folder?): File {
        val prefix = if (folder?.title?.isNotBlank() == true) {
            "${folder.title}-"
        } else {
            ""
        }
        return File(application.filesDir, prefix + FILENAME)
    }


    companion object {

        /**
         * The bookmark page standard suffix
         */
        const val FILENAME = "bookmarks.html"

        private const val FOLDER_ICON = "folder.png"
        private const val DEFAULT_ICON = "default.png"

    }
}
