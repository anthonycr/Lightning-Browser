package acr.browser.lightning._browser2.download

import acr.browser.lightning.R
import acr.browser.lightning.database.downloads.DownloadEntry
import acr.browser.lightning.database.downloads.DownloadsRepository
import acr.browser.lightning.di.DatabaseScheduler
import acr.browser.lightning.dialog.BrowserDialog.setDialogSize
import acr.browser.lightning.download.DownloadHandler
import acr.browser.lightning.log.Logger
import acr.browser.lightning.preference.UserPreferences
import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.text.format.Formatter
import android.webkit.URLUtil
import androidx.appcompat.app.AlertDialog
import com.anthonycr.grant.PermissionsManager
import com.anthonycr.grant.PermissionsResultAction
import io.reactivex.Scheduler
import io.reactivex.rxkotlin.subscribeBy
import javax.inject.Inject

/**
 * Created by anthonycr on 7/25/21.
 */
class DownloadPermissionsHelper @Inject constructor(
    private val downloadHandler: DownloadHandler,
    private val userPreferences: UserPreferences,
    private val logger: Logger,
    private val downloadsRepository: DownloadsRepository,
    @DatabaseScheduler private val databaseScheduler: Scheduler
) {

    fun download(
        activity: Activity,
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long
    ) {
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(activity, arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ),
            object : PermissionsResultAction() {
                override fun onGranted() {
                    val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
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
                }

                override fun onDenied(permission: String) {
                    //TODO show message
                    logger.log(TAG, "Download permission denied")
                }
            })
    }

    companion object {
        private const val TAG = "DownloadPermissionsHelper"
    }
}
