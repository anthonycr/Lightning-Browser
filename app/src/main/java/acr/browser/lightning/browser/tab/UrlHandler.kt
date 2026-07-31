package acr.browser.lightning.browser.tab

import acr.browser.lightning.BuildConfig
import acr.browser.lightning.R
import acr.browser.lightning.constant.FILE
import acr.browser.lightning.di.IncognitoMode
import acr.browser.lightning.extensions.snackbar
import acr.browser.lightning.log.Logger
import acr.browser.lightning.utils.IntentUtils
import acr.browser.lightning.utils.isSpecialUrl
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.MailTo
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.webkit.WebView
import androidx.core.content.FileProvider
import java.io.File
import java.net.URISyntaxException
import javax.inject.Inject

/**
 * Handle URLs loaded by the [WebView] and determine if they should be loaded by the browser or
 * another app.
 */
class UrlHandler @Inject constructor(
    private val activity: Activity,
    private val logger: Logger,
    private val intentUtils: IntentUtils,
    @IncognitoMode private val incognitoMode: Boolean
) {

    /**
     * Return true if the [url] should be loaded by another app or in another way, false if the
     * browser can let the [view] continue loading as it wants.
     */
    fun shouldOverrideLoading(
        view: WebView,
        url: String,
        headers: Map<String, String>
    ): Boolean {
        if (incognitoMode) {
            // If we are in incognito, immediately load, we don't want the url to leave the app
            return continueLoadingUrl(view, url, headers)
        }
        if (URLUtil.isAboutUrl(url)) {
            // If this is an about page, immediately load, we don't need to leave the app
            return continueLoadingUrl(view, url, headers)
        }

        return if (isMailOrIntent(url, view) || intentUtils.startActivityForUrl(view, url)) {
            // If it was a mailto: link, or an intent, or could be launched elsewhere, do that
            true
        } else {
            // If none of the special conditions was met, continue with loading the url
            continueLoadingUrl(view, url, headers)
        }
    }

    private fun continueLoadingUrl(
        webView: WebView,
        url: String,
        headers: Map<String, String>
    ): Boolean {
        if (!URLUtil.isNetworkUrl(url)
            && !URLUtil.isFileUrl(url)
            && !URLUtil.isAboutUrl(url)
            && !URLUtil.isDataUrl(url)
            && !URLUtil.isJavaScriptUrl(url)
        ) {
            webView.stopLoading()
            return true
        }
        return when {
            headers.isEmpty() -> false
            else -> {
                webView.loadUrl(url, headers)
                true
            }
        }
    }

    private fun isMailOrIntent(url: String, view: WebView): Boolean {
        if (url.startsWith("mailto:")) {
            val mailTo = MailTo.parse(url)
            val i = newEmailIntent(mailTo.to, mailTo.subject, mailTo.body, mailTo.cc)
            activity.startActivity(i)
            view.reload()
            return true
        } else if (url.startsWith("intent://")) {
            val intent = try {
                Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            } catch (ignored: URISyntaxException) {
                null
            }

            if (intent != null) {
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                intent.component = null
                intent.selector = null
                try {
                    activity.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    logger.log(TAG, "ActivityNotFoundException")
                }

                return true
            }
        } else if (URLUtil.isFileUrl(url) && !url.isSpecialUrl()) {
            val file = File(url.replace(FILE, ""))

            if (file.exists()) {
                val newMimeType = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(guessFileExtension(file.toString()))

                val intent = Intent(Intent.ACTION_VIEW)
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                val contentUri = FileProvider.getUriForFile(
                    activity,
                    BuildConfig.APPLICATION_ID + ".fileprovider",
                    file
                )
                intent.setDataAndType(contentUri, newMimeType)

                try {
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    logger.log(TAG, "Cannot open downloaded file: $url")
                }

            } else {
                activity.snackbar(R.string.message_open_download_fail)
            }
            return true
        }
        return false
    }

    /**
     * Creates a new intent that can launch the email app with a subject, address, body, and cc. It
     * is used to handle mail:to links.
     *
     * @param address The address to send the email to.
     * @param subject The subject of the email.
     * @param body The body of the email.
     * @param cc Extra addresses to CC.
     * @return A valid intent.
     */
    fun newEmailIntent(
        address: String?,
        subject: String?,
        body: String?,
        cc: String?
    ): Intent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
        putExtra(Intent.EXTRA_TEXT, body)
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_CC, cc)
        type = "message/rfc822"
    }

    fun guessFileExtension(filename: String): String? {
        val lastIndex = filename.lastIndexOf('.') + 1
        if (lastIndex > 0 && filename.length > lastIndex) {
            return filename.substring(lastIndex)
        }
        return null
    }

    companion object {
        private const val TAG = "UrlHandler"
    }
}
