package acr.browser.lightning.browser.download

import acr.browser.lightning.R
import acr.browser.lightning.browser.di.DatabaseScheduler
import acr.browser.lightning.database.downloads.DownloadEntry
import acr.browser.lightning.database.downloads.DownloadsRepository
import acr.browser.lightning.dialog.BrowserDialog.setDialogSize
import acr.browser.lightning.download.DownloadHandler
import acr.browser.lightning.log.Logger
import acr.browser.lightning.preference.UserPreferences
import android.Manifest
import android.app.Dialog
import android.content.DialogInterface
import android.text.format.Formatter
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.permissionx.guolindev.PermissionX
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject

/**
 * Wraps [DownloadHandler] for a better download API.
 */
class DownloadPermissionsHelper @Inject constructor(
    private val downloadHandler: DownloadHandler,
    private val userPreferences: UserPreferences,
    private val logger: Logger,
    private val downloadsRepository: DownloadsRepository,
    @DatabaseScheduler private val databaseScheduler: Scheduler
) {

    /**
     * Download a file with the provided [url].
     */
    fun download(
        activity: FragmentActivity,
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long
    ) {
        PermissionX.init(activity)
            .permissions(
                listOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ).request { allGranted, _, _ ->
                if (allGranted) {
                    val fileName = MimeTypeMap.getFileExtensionFromUrl(url)
                        .takeIf(String::isNotBlank)
                        ?: if (MimeTypeMap.getSingleton().hasMimeType(mimeType)) {
                            URLUtil.guessFileName(url, contentDisposition, mimeType)
                        } else {
                            url
                        }
                    val downloadSize: String = if (contentLength > 0) {
                        Formatter.formatFileSize(activity, contentLength)
                    } else {
                        activity.getString(R.string.unknown_size)
                    }
                    val dialogClickListener = DialogInterface.OnClickListener { _, which: Int ->
                        when (which) {
                            DialogInterface.BUTTON_POSITIVE -> {
                                downloadHandler.onDownloadStart(
                                    activity,
                                    userPreferences,
                                    url,
                                    userAgent,
                                    contentDisposition,
                                    mimeType,
                                    downloadSize
                                )
                                downloadsRepository.addDownloadIfNotExists(
                                    DownloadEntry(
                                        url = url,
                                        title = fileName,
                                        contentSize = downloadSize
                                    )
                                ).subscribeOn(databaseScheduler)
                                    .subscribeBy {
                                        if (!it) {
                                            logger.log(TAG, "error saving download to database")
                                        }
                                    }
                            }

                            DialogInterface.BUTTON_NEGATIVE -> {
                            }
                        }
                    }
                    val builder = AlertDialog.Builder(activity) // dialog
                    val message: String = activity.getString(R.string.dialog_download, downloadSize)
                    val dialog: Dialog = builder.setTitle(fileName)
                        .setMessage(message)
                        .setPositiveButton(
                            activity.resources.getString(R.string.action_download),
                            dialogClickListener
                        )
                        .setNegativeButton(
                            activity.resources.getString(R.string.action_cancel),
                            dialogClickListener
                        ).show()
                    setDialogSize(activity, dialog)
                    logger.log(TAG, "Downloading: $fileName")
                } else {
                    //TODO show message
                    logger.log(TAG, "Download permission denied")
                }
            }
    }

    companion object {
        private const val TAG = "DownloadPermissionsHelper"
    }
}
