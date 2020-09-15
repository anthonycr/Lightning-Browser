package acr.browser.lightning._browser2.tab

import acr.browser.lightning.ssl.SslState
import acr.browser.lightning.view.TabInitializer
import android.webkit.WebView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * Created by anthonycr on 9/12/20.
 */
class TabAdapter(
    private val tabInitializer: TabInitializer,
    private val webView: WebView,
) : TabModel {

    private val urlObservable = PublishSubject.create<String>()
    private val sslStateObservable = PublishSubject.create<SslState>()
    private val progressObservable = PublishSubject.create<Int>()
    private val titleObservable = PublishSubject.create<String>()
    private val goBackObservable = PublishSubject.create<Boolean>()
    private val goForwardObservable = PublishSubject.create<Boolean>()
    private val tabWebViewClient = TabWebViewClient(
        urlObservable = urlObservable,
        sslStateObservable = sslStateObservable,
        goBackObservable = goBackObservable,
        goForwardObservable = goForwardObservable
    )

    init {
        webView.webViewClient = tabWebViewClient
        webView.webChromeClient = TabWebChromeClient(
            progressObservable = progressObservable,
            titleObservable = titleObservable
        )
        tabInitializer.initialize(webView, emptyMap())
    }

    override val id: Int = webView.id

    override fun loadUrl(url: String) {
        webView.loadUrl(url)
    }

    override fun goBack() {
        webView.goBack()
    }

    override fun canGoBack(): Boolean = webView.canGoBack()

    override fun canGoBackChanges(): Observable<Boolean> = goBackObservable.hide()

    override fun goForward() {
        webView.goForward()
    }

    override fun canGoForward(): Boolean = webView.canGoForward()

    override fun canGoForwardChanges(): Observable<Boolean> = goForwardObservable.hide()

    override fun reload() {
        webView.reload()
    }

    override fun stopLoading() {
        webView.stopLoading()
    }

    override val url: String
        get() = webView.url.orEmpty()

    override fun urlChanges(): Observable<String> = urlObservable.hide()

    override val title: String
        get() = webView.title.orEmpty()

    override fun titleChanges(): Observable<String> = titleObservable.hide()

    override val sslState: SslState
        get() = tabWebViewClient.sslState

    override fun sslChanges(): Observable<SslState> = sslStateObservable.hide()

    override val loadingProgress: Int
        get() = webView.progress

    override fun loadingProgress(): Observable<Int> = progressObservable.hide()

    override var isForeground: Boolean = false
        set(value) {
            field = value
            if (field) {
                webView.onResume()
            } else {
                webView.onPause()
            }
        }

    override fun destroy() {
        webView.stopLoading()
        webView.onPause()
        webView.clearHistory()
        webView.removeAllViews()
        webView.destroy()
    }
}
