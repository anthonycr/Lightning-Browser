package acr.browser.lightning.database.bookmark

import acr.browser.lightning.database.Bookmark

/**
 * The interface that should be used to communicate with the bookmark database.
 *
 * Created by anthonycr on 5/6/17.
 */
interface BookmarkRepository {

    /**
     * Gets the bookmark associated with the URL.
     *
     * @param url the URL to look for.
     * @return an observable that will emit either the bookmark associated with the URL or null.
     */
    suspend fun findBookmarkForUrl(url: String): Bookmark.Entry?

    /**
     * Determines if a URL is associated with a bookmark.
     *
     * @param url the URL to check.
     * @return an observable that will emit true if the URL is a bookmark, false otherwise.
     */
    suspend fun isBookmark(url: String): Boolean

    /**
     * Adds a bookmark if one does not already exist with the same URL.
     *
     * @param entry the bookmark to add.
     * @return an observable that emits true if the bookmark was added, false otherwise.
     */
    suspend fun addBookmarkIfNotExists(entry: Bookmark.Entry): Boolean

    /**
     * Adds a list of bookmarks to the database.
     *
     * @param bookmarkItems the bookmarks to add.
     * @return an observable that emits a complete event when all the bookmarks have been added.
     */
    suspend fun addBookmarkList(bookmarkItems: List<Bookmark.Entry>)

    /**
     * Deletes a bookmark from the database. The [Bookmark.Entry.url] is used to delete the
     * bookmark.
     *
     * @param entry the bookmark to delete.
     * @return an observable that emits true when the entry is deleted, false otherwise.
     */
    suspend fun deleteBookmark(entry: Bookmark.Entry): Boolean

    /**
     * Moves all bookmarks in the old folder to the new folder.
     *
     * @param oldName the name of the old folder.
     * @param newName the name of the new folder.
     * @return an observable that emits a completion event when the folder is renamed.
     */
    suspend fun renameFolder(oldName: String, newName: String)

    /**
     * Deletes a folder from the database, all bookmarks in that folder will be moved to the root
     * level.
     *
     * @param folderToDelete the folder to delete.
     * @return an observable that emits a completion event when the folder has been deleted.
     */
    suspend fun deleteFolder(folderToDelete: String)

    /**
     * Deletes all bookmarks in the database.
     *
     * @return an observable that emits a completion event when all bookmarks have been deleted.
     */
    suspend fun deleteAllBookmarks()

    /**
     * Changes the bookmark with the original URL with all the data from the new bookmark.
     *
     * @param oldBookmark the old bookmark to replace.
     * @param newBookmark the new bookmark.
     * @return an observable that emits a completion event when the bookmark edit is done.
     */
    suspend fun editBookmark(oldBookmark: Bookmark.Entry, newBookmark: Bookmark.Entry)

    /**
     * Emits a list of all bookmarks, sorted by folder, position, title, and url.
     *
     * @return an observable that emits a list of all bookmarks.
     */
    suspend fun getAllBookmarksSorted(): List<Bookmark.Entry>

    /**
     * Emits all bookmarks in a certain folder. If the folder chosen is null, then all bookmarks
     * without a specified folder will be returned.
     *
     * @param folder gets the bookmarks from this folder, may be null.
     * @return an observable that emits a list of bookmarks in the given folder.
     */
    suspend fun getBookmarksFromFolderSorted(folder: String?): List<Bookmark>

    /**
     * Returns all folders as [Bookmark.Folder]. The root folder is omitted.
     *
     * @return an observable that emits a list of folders.
     */
    suspend fun getFoldersSorted(): List<Bookmark.Folder>

    /**
     * Returns the names of all folders. The root folder is omitted.
     *
     * @return an observable that emits a list of folder names.
     */
    suspend fun getFolderNames(): List<String>

    /**
     * A synchronous call to the model that returns the number of bookmarks. Should be called from a
     * background thread.
     *
     * @return the number of bookmarks in the database.
     */
    suspend fun count(): Long
}
