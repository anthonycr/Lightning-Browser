/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.download

import acr.browser.lightning.BuildConfig
import acr.browser.lightning.DefaultBrowserActivity
import acr.browser.lightning.R
import acr.browser.lightning.browser.di.MainScheduler
import acr.browser.lightning.browser.di.NetworkScheduler
import acr.browser.lightning.constant.FILE
import acr.browser.lightning.dialog.BrowserDialog.setDialogSize
import acr.browser.lightning.extensions.snackbar
import acr.browser.lightning.log.Logger
import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.utils.FileUtils.addNecessarySlashes
import acr.browser.lightning.utils.Utils
import android.app.Activity
import android.app.Dialog
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.text.TextUtils
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.functions.Consumer
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handle download requests
 */
@Singleton
class DownloadHandler @Inject constructor(
    private val downloadManager: DownloadManager,
    @NetworkScheduler private val networkScheduler: Scheduler,
    @MainScheduler private val mainScheduler: Scheduler,
    private val logger: Logger
) {
    /**
     * Notify the host application a download should be done, or that the data
     * should be streamed if a streaming viewer is available.
     * 
     * @param context            The context in which the download was requested.
     * @param url                The full url to the content that should be downloaded
     * @param userAgent          User agent of the downloading application.
     * @param contentDisposition Content-disposition http header, if present.
     * @param mimeType           The mimeType of the content reported by the server
     * @param contentSize        The size of the content
     */
    fun onDownloadStart(
        context: Activity,
        manager: UserPreferencesDataStore,
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        contentSize: String
    ) {
        logger.log(TAG, "DOWNLOAD: Trying to download from URL: $url")
        logger.log(TAG, "DOWNLOAD: Content disposition: $contentDisposition")
        logger.log(TAG, "DOWNLOAD: MimeType: $mimeType")
        logger.log(TAG, "DOWNLOAD: User agent: $userAgent")

        // if we're dealing wih A/V content that's not explicitly marked
        // for download, check if it's streamable.
        if (contentDisposition == null
            || !contentDisposition.regionMatches(0, "attachment", 0, 10, ignoreCase = true)
        ) {
            // query the package manager to see if there's a registered handler
            // that matches.
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(url.toUri(), mimeType)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.component = null
            intent.selector = null
            val info = context.packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            if (info != null) {
                // If we resolved to ourselves, we don't want to attempt to
                // load the url only to try and download it again.
                if (BuildConfig.APPLICATION_ID == info.activityInfo.packageName
                    || DefaultBrowserActivity::class.java.name == info.activityInfo.name
                ) {
                    // someone (other than us) knows how to handle this mime
                    // type with this scheme, don't download.
                    try {
                        context.startActivity(intent)
                        return
                    } catch (ex: ActivityNotFoundException) {
                        // Best behavior is to fall back to a download in this
                        // case
                    }
                }
            }
        }
        onDownloadStartNoStream(
            context,
            manager,
            url,
            userAgent,
            contentDisposition,
            mimeType,
            contentSize
        )
    }

    /**
     * Notify the host application a download should be done, even if there is a
     * streaming viewer available for thise type.
     * 
     * @param context            The context in which the download is requested.
     * @param url                The full url to the content that should be downloaded
     * @param userAgent          User agent of the downloading application.
     * @param contentDisposition Content-disposition http header, if present.
     * @param mimetype           The mimetype of the content reported by the server
     * @param contentSize        The size of the content
     */
    /* package */
    private fun onDownloadStartNoStream(
        context: Activity,
        preferences: UserPreferencesDataStore,
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?,
        contentSize: String
    ) {
        val filename = URLUtil.guessFileName(url, contentDisposition, mimetype)

        // Check to see if we have an SDCard
        val status = Environment.getExternalStorageState()
        if (status != Environment.MEDIA_MOUNTED) {
            val title: Int
            val msg: String?

            // Check to see if the SDCard is busy, same as the music app
            if (status == Environment.MEDIA_SHARED) {
                msg = context.getString(R.string.download_sdcard_busy_dlg_msg)
                title = R.string.download_sdcard_busy_dlg_title
            } else {
                msg = context.getString(R.string.download_no_sdcard_dlg_msg)
                title = R.string.download_no_sdcard_dlg_title
            }

            val dialog: Dialog = AlertDialog.Builder(context).setTitle(title)
                .setIcon(android.R.drawable.ic_dialog_alert).setMessage(msg)
                .setPositiveButton(R.string.action_ok, null).show()
            setDialogSize(context, dialog)
            return
        }

        // java.net.URI is a lot stricter than KURL so we have to encode some
        // extra characters. Fix for b 2538060 and b 1634719
        val webAddress: WebAddress?
        try {
            webAddress = WebAddress(url)
            webAddress.path = encodePath(webAddress.path)
        } catch (e: Exception) {
            // This only happens for very bad urls, we want to catch the
            // exception here
            logger.log(TAG, "Exception while trying to parse url '$url'", e)
            context.snackbar(R.string.problem_download)
            return
        }

        val addressString = webAddress.toString()
        val uri = addressString.toUri()
        val request: DownloadManager.Request
        try {
            request = DownloadManager.Request(uri)
        } catch (e: IllegalArgumentException) {
            context.snackbar(R.string.cannot_download)
            return
        }

        // set downloaded file destination to /sdcard/Download.
        // or, should it be set to one of several Environment.DIRECTORY* dirs
        // depending on mimetype?
        var location = "TODO" // TODO: preferences.getDownloadDirectory();
        location = addNecessarySlashes(location)
        val downloadFolder = location.toUri()

        if (!isWriteAccessAvailable(downloadFolder)) {
            context.snackbar(R.string.problem_location_download)
            return
        }
        val newMimeType =
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(Utils.guessFileExtension(filename))
        logger.log(TAG, "New mimetype: $newMimeType")
        request.setMimeType(newMimeType)
        request.setDestinationUri((FILE + location + filename).toUri())
        // let this downloaded file be scanned by MediaScanner - so that it can
        // show up in Gallery app, for example.
        request.setVisibleInDownloadsUi(true)
        request.allowScanningByMediaScanner()
        request.setDescription(webAddress.host)
        // XXX: Have to use the old url since the cookies were stored using the
        // old percent-encoded url.
        val cookies = CookieManager.getInstance().getCookie(url)
        request.addRequestHeader(COOKIE_REQUEST_HEADER, cookies)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        if (mimetype == null) {
            logger.log(TAG, "Mimetype is null")
            if (TextUtils.isEmpty(addressString)) {
                return
            }
            // We must have long pressed on a link or image to download it. We
            // are not sure of the mimetype in this case, so do a head request
            val disposable =
                FetchUrlMimeType(downloadManager, request, addressString, cookies, userAgent)
                    .create()
                    .subscribeOn(networkScheduler)
                    .observeOn(mainScheduler)
                    .subscribe(Consumer { result: FetchUrlMimeType.Result ->
                        when (result) {
                            FetchUrlMimeType.Result.FAILURE_ENQUEUE -> context.snackbar(R.string.cannot_download)
                            FetchUrlMimeType.Result.FAILURE_LOCATION -> context.snackbar(R.string.problem_location_download)
                            FetchUrlMimeType.Result.SUCCESS -> context.snackbar(R.string.download_pending)
                        }
                    })
        } else {
            logger.log(TAG, "Valid mimetype, attempting to download")
            try {
                downloadManager.enqueue(request)
            } catch (e: IllegalArgumentException) {
                // Probably got a bad URL or something
                logger.log(TAG, "Unable to enqueue request", e)
                context.snackbar(R.string.cannot_download)
            } catch (e: SecurityException) {
                // TODO write a download utility that downloads files rather than rely on the system
                // because the system can only handle Environment.getExternal... as a path
                context.snackbar(R.string.problem_location_download)
            }
            context.snackbar(context.getString(R.string.download_pending) + ' ' + filename)
        }
    }

    companion object {
        private const val TAG = "DownloadHandler"

        private const val COOKIE_REQUEST_HEADER = "Cookie"

        // This is to work around the fact that java.net.URI throws Exceptions
        // instead of just encoding URL's properly
        // Helper method for onDownloadStartNoStream
        private fun encodePath(path: String): String {
            val chars = path.toCharArray()

            var needed = false
            for (c in chars) {
                if (c == '[' || c == ']' || c == '|') {
                    needed = true
                    break
                }
            }
            if (!needed) {
                return path
            }

            val sb = StringBuilder()
            for (c in chars) {
                if (c == '[' || c == ']' || c == '|') {
                    sb.append('%')
                    sb.append(Integer.toHexString(c.code))
                } else {
                    sb.append(c)
                }
            }

            return sb.toString()
        }

        private fun isWriteAccessAvailable(fileUri: Uri): Boolean {
            if (fileUri.path == null) {
                return false
            }
            val file = File(fileUri.path)

            if (!file.isDirectory && !file.mkdirs()) {
                return false
            }

            try {
                if (file.createNewFile()) {
                    file.delete()
                }
                return true
            } catch (ignored: IOException) {
                return false
            }
        }
    }
}
