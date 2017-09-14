package acr.browser.lightning.html

import com.anthonycr.mezzanine.FileStream

/**
 * The store for the bookmarks HTML.
 */
@FileStream("app/src/main/html/bookmarks.html")
interface BookmarkPageReader {

    fun provideString(): String

}