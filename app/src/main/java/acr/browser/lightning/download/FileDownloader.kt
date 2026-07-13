package acr.browser.lightning.download

import acr.browser.lightning.R
import acr.browser.lightning.browser.download.PendingDownload
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.database.downloads.DownloadEntry
import acr.browser.lightning.database.downloads.DownloadsRepository
import acr.browser.lightning.log.Logger
import acr.browser.lightning.resources.ResourceProvider
import android.app.Application
import android.app.DownloadManager
import android.os.Environment
import android.text.format.Formatter
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import androidx.core.net.toUri
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

/**
 * Default implementation of [FileDownloader] backed by [DownloadManager].
 */
class DefaultFileDownloader @Inject constructor(
    private val application: Application,
    private val logger: Logger,
    private val downloadsRepository: DownloadsRepository,
    private val resourceProvider: ResourceProvider,
    private val downloadManager: DownloadManager,
    private val coroutineDispatchers: CoroutineDispatchers,
) : FileDownloader {
    override suspend fun download(pendingDownload: PendingDownload) =
        withContext(coroutineDispatchers.io) {
            logger.log("DefaultFileDownloader", "Pending download: $pendingDownload")

            val cookie = CookieManager.getInstance().getCookie(pendingDownload.url)

            val normalizedPendingDownload = fetchFileInfo(cookie, pendingDownload)

            val guessExtension = normalizedPendingDownload.mimeType?.let {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(it)
            } ?: MimeTypeMap.getFileExtensionFromUrl(normalizedPendingDownload.url)

            val guessMimeType = normalizedPendingDownload.mimeType
                ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(guessExtension)
                    ?.takeIf { it.isNotEmpty() }
                ?: "text/plain"

            val guessFileName = URLUtil.guessFileName(
                normalizedPendingDownload.url,
                normalizedPendingDownload.contentDisposition,
                guessMimeType
            )

            val request = DownloadManager.Request(normalizedPendingDownload.url.toUri())
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setTitle(guessFileName)
                .setDescription(
                    normalizedPendingDownload.contentDisposition ?: normalizedPendingDownload.url
                )
                .setMimeType(guessMimeType)
                .addRequestHeader("Cookie", cookie)
                .addRequestHeader("User-Agent", normalizedPendingDownload.userAgent)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    guessFileName
                )

            val contentSize = if (normalizedPendingDownload.contentLength > 0) {
                Formatter.formatFileSize(application, normalizedPendingDownload.contentLength)
            } else {
                resourceProvider.stringResource(R.string.unknown_size)
            }

            downloadsRepository.addDownloadIfNotExists(
                DownloadEntry(
                    url = normalizedPendingDownload.url,
                    title = guessFileName,
                    contentSize = contentSize
                )
            )

            // TODO: Save download id to delete from downloads
            downloadManager.enqueue(request)
            Unit
        }

    private suspend fun fetchFileInfo(
        cookie: String?,
        pendingDownload: PendingDownload,
    ): PendingDownload = withContext(coroutineDispatchers.network) {
        if (pendingDownload.mimeType != null && pendingDownload.contentLength != 0L) {
            return@withContext pendingDownload
        }
        val client = OkHttpClient()

        val response = client.newCall(
            Request.Builder()
                .url(pendingDownload.url)
                .head()
                .addHeader("Cookie", cookie.orEmpty())
                .addHeader("User-Agent", pendingDownload.userAgent.orEmpty())
                .build()
        ).execute()

        logger.log(TAG, "HEAD: ${response.headers}")

        pendingDownload.copy(
            mimeType = response.header("content-type") ?: pendingDownload.mimeType,
            contentLength = response.header("content-length")?.toLong()
                ?: pendingDownload.contentLength
        )
    }

    companion object {
        private const val TAG = "DefaultFileDownloader"
    }
}

/**
 * Used to download files of various and unknown types.
 */
interface FileDownloader {
    /**
     * Download the file obtained from the [pendingDownload].
     */
    suspend fun download(pendingDownload: PendingDownload)
}
