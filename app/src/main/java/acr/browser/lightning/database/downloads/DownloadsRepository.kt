package acr.browser.lightning.database.downloads

/**
 * The interface that should be used to communicate with the download database.
 *
 * Created by df1e on 29/5/17.
 */
interface DownloadsRepository {

    /**
     * Determines if a location is associated with a download.
     *
     * @param location the location to check.
     * @return an observable that will emit true if the location is a download, false otherwise.
     */
    suspend fun isDownload(location: String): Boolean

    /**
     * Gets the download associated with the URL.
     *
     * @param location the location to look for.
     * @return an observable that will emit either the download associated with the location or null.
     */
    suspend fun findDownloadForUrl(location: String): DownloadEntry?

    /**
     * Adds a download if one does not already exist with the same URL.
     *
     * @param entry the download to add.
     * @return an observable that emits true if the download was added, false otherwise.
     */
    suspend fun addDownloadIfNotExists(entry: DownloadEntry): Boolean

    /**
     * Adds a list of downloads to the database.
     *
     * @param downloadEntries the downloads to add.
     * @return an observable that emits a complete event when all the downloads have been added.
     */
    suspend fun addDownloadsList(downloadEntries: List<DownloadEntry>)

    /**
     * Deletes a download from the database.
     *
     * @param location the download location to delete.
     * @return an observable that emits true when the download is deleted, false otherwise.
     */
    suspend fun deleteDownload(location: String): Boolean

    /**
     * Deletes all downloads in the database.
     *
     * @return an observable that emits a completion event when all downloads have been deleted.
     */
    suspend fun deleteAllDownloads()

    /**
     * Emits a list of all downloads, sorted by primary key.
     *
     * @return an observable that emits a list of all downloads.
     */
    suspend fun getAllDownloads(): List<DownloadEntry>

    /**
     * A synchronous call to the model that returns the number of downloads. Should be called from a
     * background thread.
     *
     * @return the number of downloads in the database.
     */
    suspend fun count(): Long
}
