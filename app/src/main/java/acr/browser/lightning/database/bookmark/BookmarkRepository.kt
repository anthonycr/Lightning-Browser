package acr.browser.lightning.database.bookmark

import acr.browser.lightning.database.Bookmark
import android.support.annotation.WorkerThread
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single

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
    fun findBookmarkForUrl(url: String): Maybe<Bookmark.Entry>

    /**
     * Determines if a URL is associated with a bookmark.
     *
     * @param url the URL to check.
     * @return an observable that will emit true if the URL is a bookmark, false otherwise.
     */
    fun isBookmark(url: String): Single<Boolean>

    /**
     * Adds a bookmark if one does not already exist with the same URL.
     *
     * @param entry the bookmark to add.
     * @return an observable that emits true if the bookmark was added, false otherwise.
     */
    fun addBookmarkIfNotExists(entry: Bookmark.Entry): Single<Boolean>

    /**
     * Adds a list of bookmarks to the database.
     *
     * @param bookmarkItems the bookmarks to add.
     * @return an observable that emits a complete event when all the bookmarks have been added.
     */
    fun addBookmarkList(bookmarkItems: List<Bookmark.Entry>): Completable

    /**
     * Deletes a bookmark from the database.
     *
     * @param bookmark the bookmark to delete.
     * @return an observable that emits true when the bookmark is deleted, false otherwise.
     */
    fun deleteBookmark(bookmark: Bookmark.Entry): Single<Boolean>

    /**
     * Moves all bookmarks in the old folder to the new folder.
     *
     * @param oldName the name of the old folder.
     * @param newName the name of the new folder.
     * @return an observable that emits a completion event when the folder is renamed.
     */
    fun renameFolder(oldName: String, newName: String): Completable

    /**
     * Deletes a folder from the database, all bookmarks in that folder will be moved to the root
     * level.
     *
     * @param folderToDelete the folder to delete.
     * @return an observable that emits a completion event when the folder has been deleted.
     */
    fun deleteFolder(folderToDelete: String): Completable

    /**
     * Deletes all bookmarks in the database.
     *
     * @return an observable that emits a completion event when all bookmarks have been deleted.
     */
    fun deleteAllBookmarks(): Completable

    /**
     * Changes the bookmark with the original URL with all the data from the new bookmark.
     *
     * @param oldBookmark the old bookmark to replace.
     * @param newBookmark the new bookmark.
     * @return an observable that emits a completion event when the bookmark edit is done.
     */
    fun editBookmark(oldBookmark: Bookmark.Entry, newBookmark: Bookmark.Entry): Completable

    /**
     * Emits a list of all bookmarks
     *
     * @return an observable that emits a list of all bookmarks.
     */
    fun getAllBookmarks(): Single<List<Bookmark.Entry>>

    /**
     * Emits all bookmarks in a certain folder. If the folder chosen is null, then all bookmarks
     * without a specified folder will be returned.
     *
     * @param folder gets the bookmarks from this folder, may be null.
     * @return an observable that emits a list of bookmarks in the given folder.
     */
    fun getBookmarksFromFolderSorted(folder: String?): Single<List<Bookmark>>

    /**
     * Returns all folders as [Bookmark.Folder]. The root folder is omitted.
     *
     * @return an observable that emits a list of folders.
     */
    fun getFoldersSorted(): Single<List<Bookmark.Folder>>

    /**
     * Returns the names of all folders. The root folder is omitted.
     *
     * @return an observable that emits a list of folder names.
     */
    fun getFolderNames(): Single<List<String>>

    /**
     * A synchronous call to the model that returns the number of bookmarks. Should be called from a
     * background thread.
     *
     * @return the number of bookmarks in the database.
     */
    @WorkerThread
    fun count(): Long
}
