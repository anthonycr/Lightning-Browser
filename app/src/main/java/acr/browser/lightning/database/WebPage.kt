package acr.browser.lightning.database

import acr.browser.lightning.constant.FOLDER

/**
 * A data type that represents a page that can be loaded.
 */
sealed class WebPage(
    open val url: String,
    open val title: String
)

/**
 * A data type that represents a page that was visited by the user.
 */
data class HistoryEntry(
    override val url: String,
    override val title: String,
    val lastTimeVisited: Long = System.currentTimeMillis()
) : WebPage(url, title)

/**
 * A data type that represents an entity that has been bookmarked by the user or contains a page
 * that has been bookmarked by the user.
 */
sealed class Bookmark(
    override val url: String,
    override val title: String
) : WebPage(url, title) {

    /**
     * A data type that has been bookmarked by the user.
     */
    data class Entry(
        override val url: String,
        override val title: String,
        val position: Int,
        val folder: Folder
    ) : Bookmark(url, title)

    /**
     * A data type that represents a container for a [Bookmark.Entry].
     */
    sealed class Folder(
        override val url: String,
        override val title: String
    ) : Bookmark(url, title) {

        object Root : Folder("", "")

        data class Entry(
            override val url: String,
            override val title: String
        ) : Folder(url, title)

    }

}

/**
 * A data type that represents a suggestion for a search query.
 */
data class SearchSuggestion(
    override val url: String,
    override val title: String
) : WebPage(url, title)

/**
 * Creates a [Bookmark.Folder] from the provided [String].
 */
fun String?.asFolder(): Bookmark.Folder = this
    ?.takeIf(String::isNotBlank)
    ?.let {
        Bookmark.Folder.Entry(
            url = "$FOLDER$this",
            title = this
        )
    } ?: Bookmark.Folder.Root
