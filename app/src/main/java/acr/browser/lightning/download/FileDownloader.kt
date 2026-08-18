package acr.browser.lightning.download

/**
 * Used to download files of various and unknown types.
 */
interface FileDownloader {

    /**
     * Download the file obtained from the [pendingDownload].
     */
    suspend fun download(pendingDownload: PendingDownload)
}
