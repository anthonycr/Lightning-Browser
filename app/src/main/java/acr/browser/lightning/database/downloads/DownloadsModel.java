package acr.browser.lightning.database.downloads;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.anthonycr.bonsai.Completable;
import com.anthonycr.bonsai.Single;

import java.util.List;

/**
 * The interface that should be used to
 * communicate with the download database.
 * <p>
 * Created by df1e on 29/5/17.
 */
public interface DownloadsModel {

    /**
     * Determines if a URL is associated with a download.
     *
     * @param url the URL to check.
     * @return an observable that will emit true if
     * the URL is a download, false otherwise.
     */
    @NonNull
    Single<Boolean> isDownload(@NonNull String url);

    /**
     * Gets the download associated with the URL.
     *
     * @param url the URL to look for.
     * @return an observable that will emit either
     * the download associated with the URL or null.
     */
    @NonNull
    Single<DownloadItem> findDownloadForUrl(@NonNull String url);

    /**
     * Adds a download if one does not already exist with
     * the same URL.
     *
     * @param item the download to add.
     * @return an observable that emits true if the download
     * was added, false otherwise.
     */
    @NonNull
    Single<Boolean> addDownloadIfNotExists(@NonNull DownloadItem item);

    /**
     * Adds a list of downloads to the database.
     *
     * @param downloadItems the downloads to add.
     * @return an observable that emits a complete event
     * when all the downloads have been added.
     */
    @NonNull
    Completable addDownloadsList(@NonNull List<DownloadItem> downloadItems);

    /**
     * Deletes a download from the database.
     *
     * @param url the download url to delete.
     * @return an observable that emits true when
     * the download is deleted, false otherwise.
     */
    @NonNull
    Single<Boolean> deleteDownload(@NonNull String url);

    /**
     * Deletes all downloads in the database.
     *
     * @return an observable that emits a completion
     * event when all downloads have been deleted.
     */
    @NonNull
    Completable deleteAllDownloads();

    /**
     * Emits a list of all downloads
     *
     * @return an observable that emits a list
     * of all downloads.
     */
    @NonNull
    Single<List<DownloadItem>> getAllDownloads();

    /**
     * A synchronous call to the model
     * that returns the number of downloads.
     * Should be called from a background thread.
     *
     * @return the number of downloads in the database.
     */
    @WorkerThread
    long count();
}
