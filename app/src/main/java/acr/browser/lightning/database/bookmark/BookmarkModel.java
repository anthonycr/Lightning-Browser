package acr.browser.lightning.database.bookmark;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.anthonycr.bonsai.Completable;
import com.anthonycr.bonsai.Single;

import java.io.File;
import java.util.List;

import acr.browser.lightning.database.HistoryItem;

/**
 * The interface that should be used to
 * communicate with the bookmark database.
 * <p>
 * Created by anthonycr on 5/6/17.
 */
public interface BookmarkModel {

    @NonNull
    Single<HistoryItem> findBookmarkForUrl(@NonNull String url);

    @NonNull
    Single<Boolean> isBookmark(@NonNull String url);

    @NonNull
    Single<Boolean> addBookmarkIfNotExists(@NonNull HistoryItem item);

    @NonNull
    Completable addBookmarkList(@NonNull List<HistoryItem> bookmarkItems);

    @NonNull
    Single<Boolean> deleteBookmark(@NonNull HistoryItem bookmark);

    @NonNull
    Completable renameFolder(@NonNull String oldName, @NonNull String newName);

    @NonNull
    Completable deleteFolder(@NonNull String folderToDelete);

    @NonNull
    Completable deleteAllBookmarks();

    @NonNull
    Completable editBookmark(@NonNull HistoryItem oldBookmark, @NonNull HistoryItem newBookmark);

    @NonNull
    Single<List<HistoryItem>> getAllBookmarks();

    @NonNull
    Single<List<HistoryItem>> getBookmarksFromFolder(@Nullable String folder);

    @NonNull
    Single<List<HistoryItem>> getFolders();

    @NonNull
    Single<List<String>> getFolderNames();
}
