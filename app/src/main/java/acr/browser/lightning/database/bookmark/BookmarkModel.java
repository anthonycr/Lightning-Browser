package acr.browser.lightning.database.bookmark;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import com.anthonycr.bonsai.Completable;
import com.anthonycr.bonsai.Single;

import java.util.List;

import acr.browser.lightning.database.HistoryItem;

/**
 * The interface that should be used to
 * communicate with the bookmark database.
 * <p>
 * Created by anthonycr on 5/6/17.
 */
public interface BookmarkModel {

    /**
     * Gets the bookmark associated with the URL.
     *
     * @param url the URL to look for.
     * @return an observable that will emit either
     * the bookmark associated with the URL or null.
     */
    @NonNull
    Single<HistoryItem> findBookmarkForUrl(@NonNull String url);

    /**
     * Determines if a URL is associated with a bookmark.
     *
     * @param url the URL to check.
     * @return an observable that will emit true if
     * the URL is a bookmark, false otherwise.
     */
    @NonNull
    Single<Boolean> isBookmark(@NonNull String url);

    /**
     * Adds a bookmark if one does not already exist with
     * the same URL.
     *
     * @param item the bookmark to add.
     * @return an observable that emits true if the bookmark
     * was added, false otherwise.
     */
    @NonNull
    Single<Boolean> addBookmarkIfNotExists(@NonNull HistoryItem item);

    /**
     * Adds a list of bookmarks to the database.
     *
     * @param bookmarkItems the bookmarks to add.
     * @return an observable that emits a complete event
     * when all the bookmarks have been added.
     */
    @NonNull
    Completable addBookmarkList(@NonNull List<HistoryItem> bookmarkItems);

    /**
     * Deletes a bookmark from the database.
     *
     * @param bookmark the bookmark to delete.
     * @return an observable that emits true when
     * the bookmark is deleted, false otherwise.
     */
    @NonNull
    Single<Boolean> deleteBookmark(@NonNull HistoryItem bookmark);

    /**
     * Moves all bookmarks in the old folder to the new folder.
     *
     * @param oldName the name of the old folder.
     * @param newName the name of the new folder.
     * @return an observable that emits a completion
     * event when the folder is renamed.
     */
    @NonNull
    Completable renameFolder(@NonNull String oldName, @NonNull String newName);

    /**
     * Deletes a folder from the database, all bookmarks
     * in that folder will be moved to the root level.
     *
     * @param folderToDelete the folder to delete.
     * @return an observable that emits a completion
     * event when the folder has been deleted.
     */
    @NonNull
    Completable deleteFolder(@NonNull String folderToDelete);

    /**
     * Deletes all bookmarks in the database.
     *
     * @return an observable that emits a completion
     * event when all bookmarks have been deleted.
     */
    @NonNull
    Completable deleteAllBookmarks();

    /**
     * Changes the bookmark with the original URL
     * with all the data from the new bookmark.
     *
     * @param oldBookmark the old bookmark to replace.
     * @param newBookmark the new bookmark.
     * @return an observable that emits a completion event
     * when the bookmark edit is done.
     */
    @NonNull
    Completable editBookmark(@NonNull HistoryItem oldBookmark, @NonNull HistoryItem newBookmark);

    /**
     * Emits a list of all bookmarks
     *
     * @return an observable that emits a list
     * of all bookmarks.
     */
    @NonNull
    Single<List<HistoryItem>> getAllBookmarks();

    /**
     * Emits all bookmarks in a certain folder.
     * If the folder chosen is null, then all bookmarks
     * without a specified folder will be returned.
     *
     * @param folder gets the bookmarks from this folder, may be null.
     * @return an observable that emits a list of bookmarks
     * in the given folder.
     */
    @NonNull
    Single<List<HistoryItem>> getBookmarksFromFolderSorted(@Nullable String folder);

    /**
     * Returns all folders as {@link HistoryItem}.
     * The root folder is omitted.
     *
     * @return an observable that emits a list of folders.
     */
    @NonNull
    Single<List<HistoryItem>> getFoldersSorted();

    /**
     * Returns the names of all folders.
     * The root folder is omitted.
     *
     * @return an observable that emits a list of folder names.
     */
    @NonNull
    Single<List<String>> getFolderNames();

    /**
     * A synchronous call to the model
     * that returns the number of bookmarks.
     * Should be called from a background thread.
     *
     * @return the number of bookmarks in the database.
     */
    @WorkerThread
    long count();
}
