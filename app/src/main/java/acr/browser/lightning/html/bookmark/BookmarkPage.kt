/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.html.bookmark

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.R
import acr.browser.lightning.constant.Constants
import acr.browser.lightning.database.HistoryItem
import acr.browser.lightning.database.bookmark.BookmarkModel
import acr.browser.lightning.favicon.FaviconModel
import acr.browser.lightning.utils.ThemeUtils
import acr.browser.lightning.utils.Utils
import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.text.TextUtils
import com.anthonycr.bonsai.Single
import com.anthonycr.bonsai.SingleOnSubscribe
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileWriter
import javax.inject.Inject

class BookmarkPage(activity: Activity) {

    @Inject internal lateinit var app: Application
    @Inject internal lateinit var bookmarkModel: BookmarkModel
    @Inject internal lateinit var faviconModel: FaviconModel

    private val folderIcon = ThemeUtils.getThemedBitmap(activity, R.drawable.ic_folder, false)

    init {
        BrowserApp.getAppComponent().inject(this)
    }

    fun createBookmarkPage(): Single<String> = Single.create { subscriber ->
        cacheIcon(folderIcon, getFaviconFile(app))
        cacheIcon(faviconModel.getDefaultBitmapForString(null), getDefaultIconFile(app))
        buildBookmarkPage(null)

        val bookmarkWebPage = getBookmarkPage(app, null)

        subscriber.onItem(Constants.FILE + bookmarkWebPage)
        subscriber.onComplete()
    }

    private fun cacheIcon(icon: Bitmap, file: File) {
        var outputStream: FileOutputStream? = null
        try {
            outputStream = FileOutputStream(file)
            icon.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            icon.recycle()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } finally {
            Utils.close(outputStream)
        }
    }

    private fun buildBookmarkPage(folder: String?) {
        bookmarkModel
                .getBookmarksFromFolderSorted(folder)
                .subscribe(object : SingleOnSubscribe<List<HistoryItem>>() {
                    override fun onItem(list: List<HistoryItem>?) {
                        requireNotNull(list)

                        val newList = list!!.toMutableList()

                        if (folder == null) {
                            bookmarkModel.getFoldersSorted()
                                    .subscribe(object : SingleOnSubscribe<List<HistoryItem>>() {
                                        override fun onItem(item: List<HistoryItem>?) {
                                            requireNotNull(item)

                                            newList.addAll(item!!)

                                            buildPageHtml(list, null)
                                        }
                                    })
                        } else {
                            buildPageHtml(list, folder)
                        }
                    }
                })
    }

    private fun buildPageHtml(bookmarksAndFolders: List<HistoryItem>, folder: String?) {
        val bookmarkWebPage = getBookmarkPage(app, folder)

        val builder = BookmarkPageBuilder(faviconModel, app)

        FileWriter(bookmarkWebPage, false).use {
            it.write(builder.buildPage(bookmarksAndFolders))
        }

        bookmarksAndFolders
                .filter { it.isFolder }
                .forEach { buildBookmarkPage(it.title) }
    }

    companion object {

        /**
         * The bookmark page standard suffix
         */
        const val FILENAME = "bookmarks.html"

        private const val FOLDER_ICON = "folder.png"
        private const val DEFAULT_ICON = "default.png"

        @JvmStatic
        fun getBookmarkPage(application: Application, folder: String?): File {
            val prefix = if (!TextUtils.isEmpty(folder)) folder + '-' else ""
            return File(application.filesDir, prefix + FILENAME)
        }

        private fun getFaviconFile(application: Application): File =
                File(application.cacheDir, FOLDER_ICON)

        private fun getDefaultIconFile(application: Application): File =
                File(application.cacheDir, DEFAULT_ICON)
    }

}
