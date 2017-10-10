package acr.browser.lightning.database.downloads

import android.support.annotation.WorkerThread
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single

/**
 * The interface that should be used to communicate with the download database.
 *
 * Created by df1e on 29/5/17.
 */
interface DownloadsRepository {

    /**
     * Determines if a URL is associated with a download.
     *
     * @param url the URL to check.
     * @return an observable that will emit true if the URL is a download, false otherwise.
     */
    fun isDownload(url: String): Single<Boolean>

    /**
     * Gets the download associated with the URL.
     *
     * @param url the URL to look for.
     * @return an observable that will emit either the download associated with the URL or null.
     */
    fun findDownloadForUrl(url: String): Maybe<DownloadItem>

    /**
     * Adds a download if one does not already exist with the same URL.
     *
     * @param item the download to add.
     * @return an observable that emits true if the download was added, false otherwise.
     */
    fun addDownloadIfNotExists(item: DownloadItem): Single<Boolean>

    /**
     * Adds a list of downloads to the database.
     *
     * @param downloadItems the downloads to add.
     * @return an observable that emits a complete event when all the downloads have been added.
     */
    fun addDownloadsList(downloadItems: List<DownloadItem>): Completable

    /**
     * Deletes a download from the database.
     *
     * @param url the download url to delete.
     * @return an observable that emits true when the download is deleted, false otherwise.
     */
    fun deleteDownload(url: String): Single<Boolean>

    /**
     * Deletes all downloads in the database.
     *
     * @return an observable that emits a completion event when all downloads have been deleted.
     */
    fun deleteAllDownloads(): Completable

    /**
     * Emits a list of all downloads
     *
     * @return an observable that emits a list of all downloads.
     */
    fun getAllDownloads(): Single<List<DownloadItem>>

    /**
     * A synchronous call to the model that returns the number of downloads. Should be called from a
     * background thread.
     *
     * @return the number of downloads in the database.
     */
    @WorkerThread
    fun count(): Long
}
