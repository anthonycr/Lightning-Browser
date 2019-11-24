package acr.browser.lightning.bookmark

import acr.browser.lightning.constant.UTF8
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.asFolder
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.InputStream
import javax.inject.Inject

/**
 * An importer that supports the Netscape Bookmark File Format.
 *
 * See https://msdn.microsoft.com/en-us/ie/aa753582(v=vs.94)
 */
class NetscapeBookmarkFormatImporter @Inject constructor() : BookmarkImporter {

    override fun importBookmarks(inputStream: InputStream): List<Bookmark.Entry> {
        val document = Jsoup.parse(inputStream, UTF8, "")

        val rootList = document.body().children().first { it.isTag(LIST_TAG) }

        return rootList.processFolder(ROOT_FOLDER_NAME)
    }

    /**
     * @return The [List] of [Bookmark.Entry] held by [Element] with the provided [folderName].
     */
    private fun Element.processFolder(folderName: String): List<Bookmark.Entry> {
        return children()
            .filter { it.isTag(ITEM_TAG) }
            .flatMap {
                val immediateChild = it.child(0)
                when {
                    immediateChild.isTag(FOLDER_TAG) ->
                        immediateChild.nextElementSibling()
                            .processFolder(computeFolderName(folderName, immediateChild.text()))
                    immediateChild.isTag(BOOKMARK_TAG) ->
                        listOf(Bookmark.Entry(
                            url = immediateChild.attr(HREF),
                            title = immediateChild.text(),
                            position = 0,
                            folder = folderName.asFolder()
                        ))
                    else -> emptyList()
                }
            }
    }

    /**
     * @return True if the element's tag name matches the [tagName], case insentitive, false
     * otherwise.
     */
    private fun Element.isTag(tagName: String): Boolean {
        return tagName().equals(tagName, ignoreCase = true)
    }

    /**
     * @return The [currentFolder] if the [parentFolder] is empty, otherwise prepend the
     * [parentFolder] to the [currentFolder] and return that.
     */
    private fun computeFolderName(parentFolder: String, currentFolder: String): String =
        if (parentFolder.isEmpty()) {
            currentFolder
        } else {
            "$parentFolder/${currentFolder}"
        }

    companion object {
        const val ITEM_TAG = "DT"
        const val LIST_TAG = "DL"
        const val BOOKMARK_TAG = "A"
        const val FOLDER_TAG = "H3"
        const val HREF = "HREF"
        const val ROOT_FOLDER_NAME = ""
    }

}
