package acr.browser.lightning.html.bookmark

import com.anthonycr.mezzanine.FileStream

/**
 * The store for the bookmarks HTML.
 */
@FileStream("app/src/main/html/bookmarks.html")
interface BookmarkPageReader {

    fun provideHtml(): String

}