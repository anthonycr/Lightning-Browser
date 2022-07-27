package acr.browser.lightning._browser2.tab

import acr.browser.lightning.BuildConfig
import acr.browser.lightning.R
import acr.browser.lightning._browser2.di.IncognitoMode
import acr.browser.lightning.constant.FILE
import acr.browser.lightning.extensions.snackbar
import acr.browser.lightning.log.Logger
import acr.browser.lightning.utils.IntentUtils
import acr.browser.lightning.utils.Utils
import acr.browser.lightning.utils.isSpecialUrl
import acr.browser.lightning.view.LightningWebClient
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
 * Created by anthonycr on 9/24/20.
 */
class UrlHandler @Inject constructor(
    private val activity: Activity,
    private val logger: Logger,
    private val intentUtils: IntentUtils,
    @IncognitoMode private val incognitoMode: Boolean
) {

    fun shouldOverrideLoading(
        view: WebView,
        url: String,
        headers: Map<String, String>
    ): Boolean {
        // TODO: handle proxy
//        // Check if configured proxy is available
//        if (!proxyUtils.isProxyReady(activity)) {
//            // User has been notified
//            return true
//        }

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
            val i = Utils.newEmailIntent(mailTo.to, mailTo.subject, mailTo.body, mailTo.cc)
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
                    .getMimeTypeFromExtension(Utils.guessFileExtension(file.toString()))

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
                    println("LightningWebClient: cannot open downloaded file")
                }

            } else {
                activity.snackbar(R.string.message_open_download_fail)
            }
            return true
        }
        return false
    }

    companion object {
        private const val TAG = "UrlHandler"
    }
}
