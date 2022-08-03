package acr.browser.lightning.browser.tab

import acr.browser.lightning.browser.proxy.Proxy
import acr.browser.lightning.adblock.AdBlocker
import acr.browser.lightning.adblock.allowlist.AllowListModel
import acr.browser.lightning.ssl.SslState
import android.annotation.TargetApi
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.webkit.*
import androidx.annotation.RequiresApi
import io.reactivex.subjects.PublishSubject
import java.io.ByteArrayInputStream

/**
 * Created by anthonycr on 9/12/20.
 */
class TabWebViewClient(
    private val adBlocker: AdBlocker,
    private val allowListModel: AllowListModel,
    private val urlHandler: UrlHandler,
    private val headers: Map<String, String>,
    private val proxy: Proxy
) : WebViewClient() {

    val urlObservable: PublishSubject<String> = PublishSubject.create()
    val sslStateObservable: PublishSubject<SslState> = PublishSubject.create()
    val goBackObservable: PublishSubject<Boolean> = PublishSubject.create()
    val goForwardObservable: PublishSubject<Boolean> = PublishSubject.create()

    var sslState: SslState = SslState.None
        private set

    private var currentUrl: String = ""

    private fun shouldBlockRequest(pageUrl: String, requestUrl: String) =
        !allowListModel.isUrlAllowedAds(pageUrl) &&
            adBlocker.isAd(requestUrl)

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        currentUrl = url
        urlObservable.onNext(url)
        sslState = if (URLUtil.isHttpsUrl(url)) {
            SslState.Valid
        } else {
            SslState.None
        }
        sslStateObservable.onNext(sslState)
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        urlObservable.onNext(url)
        goBackObservable.onNext(view.canGoBack())
        goForwardObservable.onNext(view.canGoForward())
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        super.onReceivedSslError(view, handler, error)
        sslState = SslState.Invalid(error)
        sslStateObservable.onNext(sslState)
    }

    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        if (!proxy.isProxyReady()) return true
        return urlHandler.shouldOverrideLoading(view, url, headers) ||
            super.shouldOverrideUrlLoading(view, url)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (!proxy.isProxyReady()) return true
        return urlHandler.shouldOverrideLoading(view, request.url.toString(), headers) ||
            super.shouldOverrideUrlLoading(view, request)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        if (shouldBlockRequest(currentUrl, request.url.toString()) || !proxy.isProxyReady()) {
            val empty = ByteArrayInputStream(emptyResponseByteArray)
            return WebResourceResponse(BLOCKED_RESPONSE_MIME_TYPE, BLOCKED_RESPONSE_ENCODING, empty)
        }
        return null
    }

    @Suppress("OverridingDeprecatedMember")
    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
        if (shouldBlockRequest(currentUrl, url)) {
            val empty = ByteArrayInputStream(emptyResponseByteArray)
            return WebResourceResponse(BLOCKED_RESPONSE_MIME_TYPE, BLOCKED_RESPONSE_ENCODING, empty)
        }
        return null
    }

    companion object {
        private val emptyResponseByteArray: ByteArray = byteArrayOf()

        private const val BLOCKED_RESPONSE_MIME_TYPE = "text/plain"
        private const val BLOCKED_RESPONSE_ENCODING = "utf-8"
    }
}
