package acr.browser.lightning.view

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.BuildConfig
import acr.browser.lightning.R
import acr.browser.lightning.adblock.AdBlocker
import acr.browser.lightning.adblock.whitelist.WhitelistModel
import acr.browser.lightning.constant.FILE
import acr.browser.lightning.controller.UIController
import acr.browser.lightning.extensions.resizeAndShow
import acr.browser.lightning.preference.PreferenceManager
import acr.browser.lightning.ssl.SSLState
import acr.browser.lightning.ssl.SslWarningPreferences
import acr.browser.lightning.utils.IntentUtils
import acr.browser.lightning.utils.ProxyUtils
import acr.browser.lightning.utils.UrlUtils
import acr.browser.lightning.utils.Utils
import android.annotation.TargetApi
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.MailTo
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.webkit.*
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import com.anthonycr.mezzanine.MezzanineGenerator
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URISyntaxException
import java.util.*
import javax.inject.Inject

class LightningWebClient(
        private val activity: Activity,
        private val lightningView: LightningView
) : WebViewClient() {

    private val uiController: UIController
    private val intentUtils = IntentUtils(activity)
    private val emptyResponseByteArray: ByteArray = byteArrayOf()

    @Inject internal lateinit var proxyUtils: ProxyUtils
    @Inject internal lateinit var preferences: PreferenceManager
    @Inject internal lateinit var sslWarningPreferences: SslWarningPreferences
    @Inject internal lateinit var whitelistModel: WhitelistModel

    private var adBlock: AdBlocker

    @Volatile private var isRunning = false
    private var zoomScale = 0.0f

    private val textReflowJs = MezzanineGenerator.TextReflow()
    private val invertPageJs = MezzanineGenerator.InvertPage()

    private var currentUrl: String = ""

    var sslState: SSLState = SSLState.None()
        set(value) {
            sslStateObservable.onNext(value)
            field = value
        }

    private val sslStateObservable: PublishSubject<SSLState> = PublishSubject.create()

    init {
        BrowserApp.appComponent.inject(this)
        uiController = activity as UIController
        adBlock = chooseAdBlocker()
    }

    fun sslStateObservable(): Observable<SSLState> = sslStateObservable

    fun updatePreferences() {
        adBlock = chooseAdBlocker()
    }

    private fun chooseAdBlocker(): AdBlocker = if (preferences.adBlockEnabled) {
        BrowserApp.appComponent.provideAssetsAdBlocker()
    } else {
        BrowserApp.appComponent.provideNoOpAdBlocker()
    }

    private fun isAd(pageUrl: String, requestUrl: String) =
            !whitelistModel.isUrlWhitelisted(pageUrl) && adBlock.isAd(requestUrl)

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        val pageUrl = currentUrl
        if (isAd(pageUrl, request.url.toString())) {
            val empty = ByteArrayInputStream(emptyResponseByteArray)
            return WebResourceResponse("text/plain", "utf-8", empty)
        }
        return super.shouldInterceptRequest(view, request)
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
        val pageUrl = currentUrl
        if (isAd(pageUrl, url)) {
            val empty = ByteArrayInputStream(emptyResponseByteArray)
            return WebResourceResponse("text/plain", "utf-8", empty)
        }
        return null
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    override fun onPageFinished(view: WebView, url: String) {
        if (view.isShown) {
            uiController.updateUrl(url, false)
            uiController.setBackButtonEnabled(view.canGoBack())
            uiController.setForwardButtonEnabled(view.canGoForward())
            view.postInvalidate()
        }
        if (view.title == null || view.title.isEmpty()) {
            lightningView.titleInfo.setTitle(activity.getString(R.string.untitled))
        } else {
            lightningView.titleInfo.setTitle(view.title)
        }
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT && lightningView.invertPage) {
            view.evaluateJavascript(invertPageJs.provideJs(), null)
        }
        uiController.tabChanged(lightningView)
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        currentUrl = url
        sslState = if (URLUtil.isHttpsUrl(url)) {
            SSLState.Valid()
        } else {
            SSLState.None()
        }
        lightningView.titleInfo.setFavicon(null)
        if (lightningView.isShown) {
            uiController.updateUrl(url, true)
            uiController.showActionBar()
        }
        uiController.tabChanged(lightningView)
    }

    override fun onReceivedHttpAuthRequest(view: WebView, handler: HttpAuthHandler,
                                           host: String, realm: String) =
            AlertDialog.Builder(activity).apply {
                val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_auth_request, null)

                val realmLabel = dialogView.findViewById<TextView>(R.id.auth_request_realm_textview)
                val name = dialogView.findViewById<EditText>(R.id.auth_request_username_edittext)
                val password = dialogView.findViewById<EditText>(R.id.auth_request_password_edittext)

                realmLabel.text = activity.getString(R.string.label_realm, realm)

                setView(dialogView)
                setTitle(R.string.title_sign_in)
                setCancelable(true)
                setPositiveButton(R.string.title_sign_in) { _, _ ->
                    val user = name.text.toString()
                    val pass = password.text.toString()
                    handler.proceed(user.trim(), pass.trim())
                    Log.d(TAG, "Attempting HTTP Authentication")
                }
                setNegativeButton(R.string.action_cancel) { _, _ ->
                    handler.cancel()
                }
            }.resizeAndShow()

    @TargetApi(Build.VERSION_CODES.KITKAT)
    override fun onScaleChanged(view: WebView, oldScale: Float, newScale: Float) {
        if (view.isShown && lightningView.preferences.textReflowEnabled
                && Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            if (isRunning)
                return
            val changeInPercent = Math.abs(100 - 100 / zoomScale * newScale)
            if (changeInPercent > 2.5f && !isRunning) {
                isRunning = view.postDelayed({
                    zoomScale = newScale
                    view.evaluateJavascript(textReflowJs.provideJs()) { isRunning = false }
                }, 100)
            }

        }
    }

    override fun onReceivedSslError(webView: WebView, handler: SslErrorHandler, error: SslError) {
        sslState = SSLState.Invalid(error)

        when (sslWarningPreferences.recallBehaviorForDomain(webView.url)) {
            SslWarningPreferences.Behavior.PROCEED -> return handler.proceed()
            SslWarningPreferences.Behavior.CANCEL -> return handler.cancel()
            null -> Unit
        }

        val errorCodeMessageCodes = getAllSslErrorMessageCodes(error)

        val stringBuilder = StringBuilder()
        for (messageCode in errorCodeMessageCodes) {
            stringBuilder.append(" - ").append(activity.getString(messageCode)).append('\n')
        }
        val alertMessage = activity.getString(R.string.message_insecure_connection, stringBuilder.toString())

        AlertDialog.Builder(activity).apply {
            val view = LayoutInflater.from(activity).inflate(R.layout.dialog_ssl_warning, null)
            val dontAskAgain = view.findViewById<CheckBox>(R.id.checkBoxDontAskAgain)
            setTitle(activity.getString(R.string.title_warning))
            setMessage(alertMessage)
            setCancelable(true)
            setView(view)
            setOnCancelListener { handler.cancel() }
            setPositiveButton(activity.getString(R.string.action_yes)) { _, _ ->
                if (dontAskAgain.isChecked) {
                    sslWarningPreferences.rememberBehaviorForDomain(webView.url, SslWarningPreferences.Behavior.PROCEED)
                }
                handler.proceed()
            }
            setNegativeButton(activity.getString(R.string.action_no)) { _, _ ->
                if (dontAskAgain.isChecked) {
                    sslWarningPreferences.rememberBehaviorForDomain(webView.url, SslWarningPreferences.Behavior.CANCEL)
                }
                handler.cancel()
            }
        }.resizeAndShow()
    }

    override fun onFormResubmission(view: WebView, dontResend: Message, resend: Message) =
            AlertDialog.Builder(activity).apply {
                setTitle(activity.getString(R.string.title_form_resubmission))
                setMessage(activity.getString(R.string.message_form_resubmission))
                setCancelable(true)
                setPositiveButton(activity.getString(R.string.action_yes)) { _, _ ->
                    resend.sendToTarget()
                }
                setNegativeButton(activity.getString(R.string.action_no)) { _, _ ->
                    dontResend.sendToTarget()
                }
            }.resizeAndShow()

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
            shouldOverrideLoading(view, request.url.toString()) || super.shouldOverrideUrlLoading(view, request)

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
            shouldOverrideLoading(view, url) || super.shouldOverrideUrlLoading(view, url)

    private fun shouldOverrideLoading(view: WebView, url: String): Boolean {
        // Check if configured proxy is available
        if (!proxyUtils.isProxyReady(activity)) {
            // User has been notified
            return true
        }

        val headers = lightningView.requestHeaders

        if (lightningView.isIncognito) {
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
        } else continueLoadingUrl(view, url, headers)

        // If none of the special conditions was met, continue with loading the url
    }

    private fun continueLoadingUrl(webView: WebView, url: String, headers: Map<String, String>) =
            when {
                headers.isEmpty() -> false
                Utils.doesSupportHeaders() -> {
                    webView.loadUrl(url, headers)
                    true
                }
                else -> false
            }

    private fun isMailOrIntent(url: String, view: WebView): Boolean {
        if (url.startsWith("mailto:")) {
            val mailTo = MailTo.parse(url)
            val i = Utils.newEmailIntent(mailTo.to, mailTo.subject,
                    mailTo.body, mailTo.cc)
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    intent.selector = null
                }
                try {
                    activity.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Log.e(TAG, "ActivityNotFoundException")
                }

                return true
            }
        } else if (URLUtil.isFileUrl(url) && !UrlUtils.isSpecialUrl(url)) {
            val file = File(url.replace(FILE, ""))

            if (file.exists()) {
                val newMimeType = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(Utils.guessFileExtension(file.toString()))

                val intent = Intent(Intent.ACTION_VIEW)
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                val contentUri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".fileprovider", file)
                intent.setDataAndType(contentUri, newMimeType)

                try {
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    println("LightningWebClient: cannot open downloaded file")
                }

            } else {
                Utils.showSnackbar(activity, R.string.message_open_download_fail)
            }
            return true
        }
        return false
    }

    private fun getAllSslErrorMessageCodes(error: SslError): List<Int> {
        val errorCodeMessageCodes = ArrayList<Int>(1)

        if (error.hasError(SslError.SSL_DATE_INVALID)) {
            errorCodeMessageCodes.add(R.string.message_certificate_date_invalid)
        }
        if (error.hasError(SslError.SSL_EXPIRED)) {
            errorCodeMessageCodes.add(R.string.message_certificate_expired)
        }
        if (error.hasError(SslError.SSL_IDMISMATCH)) {
            errorCodeMessageCodes.add(R.string.message_certificate_domain_mismatch)
        }
        if (error.hasError(SslError.SSL_NOTYETVALID)) {
            errorCodeMessageCodes.add(R.string.message_certificate_not_yet_valid)
        }
        if (error.hasError(SslError.SSL_UNTRUSTED)) {
            errorCodeMessageCodes.add(R.string.message_certificate_untrusted)
        }
        if (error.hasError(SslError.SSL_INVALID)) {
            errorCodeMessageCodes.add(R.string.message_certificate_invalid)
        }

        return errorCodeMessageCodes
    }

    companion object {

        private const val TAG = "LightningWebClient"

    }
}
