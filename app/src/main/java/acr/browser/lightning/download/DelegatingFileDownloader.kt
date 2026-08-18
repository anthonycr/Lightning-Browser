package acr.browser.lightning.download

import acr.browser.lightning.constant.DATA
import javax.inject.Inject

/**
 * Selects which [FileDownloader] to perform the download.
 */
class DelegatingFileDownloader @Inject constructor(
    private val dataImageFileDownloader: DataImageFileDownloader,
    private val defaultFileDownloader: DefaultFileDownloader
) : FileDownloader {

    override suspend fun download(pendingDownload: PendingDownload) {
        if (pendingDownload.url.startsWith(DATA)) {
            dataImageFileDownloader.download(pendingDownload)
        } else {
            defaultFileDownloader.download(pendingDownload)
        }
    }
}
