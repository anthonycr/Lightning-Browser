package acr.browser.lightning._browser2.tab

import acr.browser.lightning.ssl.SslState
import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.WebView
import android.webkit.WebViewClient
import io.reactivex.subjects.PublishSubject

/**
 * Created by anthonycr on 9/12/20.
 */
class TabWebViewClient(
    private val urlObservable: PublishSubject<String>,
    private val sslStateObservable: PublishSubject<SslState>,
    private val goBackObservable: PublishSubject<Boolean>,
    private val goForwardObservable: PublishSubject<Boolean>
) : WebViewClient() {

    var sslState: SslState = SslState.None
        private set

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
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
}
