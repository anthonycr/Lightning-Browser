package acr.browser.lightning.download

import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.extensions.fileOutputStream
import acr.browser.lightning.preference.UserPreferencesDataStore
import android.app.Application
import android.app.DownloadManager
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.ByteString.Companion.decodeBase64
import java.io.File
import javax.inject.Inject

/**
 * A [FileDownloader] that can only download images from data: URLs.
 */
class DataImageFileDownloader @Inject constructor(
    private val application: Application,
    private val downloadManager: DownloadManager,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val userPreferencesDataStore: UserPreferencesDataStore,
) : FileDownloader {

    override suspend fun download(pendingDownload: PendingDownload) =
        withContext(coroutineDispatchers.io) {
            val metadata = pendingDownload.url.substringBefore(',').substringAfter(':')
            val mimeType = metadata.substringBefore(';')
            val encoding = metadata.substringAfter(';')
            val mediaType = mimeType.toMediaTypeOrNull()
            val type = mediaType?.type
            val subtype = mediaType?.subtype

            if (!encoding.equals("base64", ignoreCase = true)) {
                // Can only decode base64
                return@withContext
            }

            if (!type.equals("image", ignoreCase = true)) {
                // We only expect images
                return@withContext
            }

            val compressFormat = when (subtype) {
                "jpeg" -> Bitmap.CompressFormat.JPEG
                "png" -> Bitmap.CompressFormat.PNG
                "webp" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSLESS
                } else {
                    Bitmap.CompressFormat.WEBP
                }

                else -> {
                    return@withContext
                }
            }

            val fileName = "data.$subtype"

            val fileSubPath =
                when (val downloadDirectory = userPreferencesDataStore.downloadDirectory.get()) {
                    "" -> fileName
                    else -> "$downloadDirectory/$fileName"
                }

            val uri = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileSubPath
            ).toUri()

            val encoded = pendingDownload.url.substringAfter(',').decodeBase64()?.toByteArray()
                ?: run {
                    return@withContext
                }

            val bitmap = BitmapFactory.decodeByteArray(encoded, 0, encoded.size)

            application.fileOutputStream(uri, coroutineDispatchers.io)?.use { outputStream ->
                bitmap.compress(compressFormat, 100, outputStream)
                bitmap.recycle()
            }

            insertCompletedDataImageDownload(pendingDownload, fileName, fileSubPath, uri)

            return@withContext
        }

    private fun insertCompletedDataImageDownload(
        pendingDownload: PendingDownload,
        fileName: String,
        fileSubPath: String,
        uri: Uri,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            application.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                ContentValues().apply {
                    put(MediaStore.DownloadColumns.DOWNLOAD_URI, pendingDownload.url)
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        fileSubPath.substringBefore(fileName)
                    )
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                }
            )
        } else {
            @Suppress("DEPRECATION")
            downloadManager.addCompletedDownload(
                fileName,
                pendingDownload.url,
                true,
                "image/*",
                uri.path!!,
                pendingDownload.contentLength,
                true
            )
        }
    }
}
