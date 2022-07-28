package acr.browser.lightning._browser2.tab

import acr.browser.lightning._browser2.download.PendingDownload
import acr.browser.lightning.constant.DESKTOP_USER_AGENT
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.preference.userAgent
import acr.browser.lightning.ssl.SslCertificateInfo
import acr.browser.lightning.ssl.SslState
import acr.browser.lightning.utils.Option
import acr.browser.lightning.utils.value
import acr.browser.lightning.view.FreezableBundleInitializer
import acr.browser.lightning.view.TabInitializer
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.WebView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * Created by anthonycr on 9/12/20.
 */
class TabAdapter(
    tabInitializer: TabInitializer,
    private val webView: WebView,
    private val requestHeaders: Map<String, String>,
    private val tabWebViewClient: TabWebViewClient,
    private val tabWebChromeClient: TabWebChromeClient,
    private val userPreferences: UserPreferences,
    private val defaultUserAgent: String,
    private val iconFreeze: Bitmap
) : TabModel {

    private var latentInitializer: FreezableBundleInitializer? = null

    private var findInPageQuery: String? = null
    private var toggleDesktop: Boolean = false
    private val downloadsSubject = PublishSubject.create<PendingDownload>()

    init {
        webView.webViewClient = tabWebViewClient
        webView.webChromeClient = tabWebChromeClient
        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            downloadsSubject.onNext(
                PendingDownload(
                    url = url,
                    userAgent = userAgent,
                    contentDisposition = contentDisposition,
                    mimeType = mimetype,
                    contentLength = contentLength
                )
            )
        }
        if (tabInitializer is FreezableBundleInitializer) {
            latentInitializer = tabInitializer
        } else {
            loadFromInitializer(tabInitializer)
        }
    }

    override val id: Int = webView.id

    override fun loadUrl(url: String) {
        webView.loadUrl(url, requestHeaders)
    }

    override fun loadFromInitializer(tabInitializer: TabInitializer) {
        tabInitializer.initialize(webView, requestHeaders)
    }

    override fun goBack() {
        webView.goBack()
    }

    override fun canGoBack(): Boolean = webView.canGoBack()

    override fun canGoBackChanges(): Observable<Boolean> = tabWebViewClient.goBackObservable.hide()

    override fun goForward() {
        webView.goForward()
    }

    override fun canGoForward(): Boolean = webView.canGoForward()

    override fun canGoForwardChanges(): Observable<Boolean> =
        tabWebViewClient.goForwardObservable.hide()

    override fun toggleDesktopAgent() {
        if (!toggleDesktop) {
            webView.settings.userAgentString = DESKTOP_USER_AGENT
        } else {
            webView.settings.userAgentString = userPreferences.userAgent(defaultUserAgent)

        }

        toggleDesktop = !toggleDesktop
    }

    override fun reload() {
        webView.reload()
    }

    override fun stopLoading() {
        webView.stopLoading()
    }

    override fun find(query: String) {
        webView.findAllAsync(query)
        findInPageQuery = query
    }

    override fun findNext() {
        webView.findNext(true)
    }

    override fun findPrevious() {
        webView.findNext(false)
    }

    override fun clearFindMatches() {
        webView.clearMatches()
        findInPageQuery = null
    }

    override val findQuery: String?
        get() = findInPageQuery

    override val favicon: Bitmap?
        get() = latentInitializer?.let { iconFreeze }
            ?: tabWebChromeClient.faviconObservable.value?.value()

    override fun faviconChanges(): Observable<Option<Bitmap>> = tabWebChromeClient.faviconObservable

    // TODO do we show "new tab"
    override val url: String
        get() = webView.url.orEmpty()

    override fun urlChanges(): Observable<String> = tabWebViewClient.urlObservable.hide()

    override val title: String
        get() = latentInitializer?.initialTitle ?: webView.title.orEmpty()

    override fun titleChanges(): Observable<String> = tabWebChromeClient.titleObservable.hide()

    override val sslCertificateInfo: SslCertificateInfo?
        get() = webView.certificate?.let {
            SslCertificateInfo(
                issuedByCommonName = it.issuedBy.cName,
                issuedToCommonName = it.issuedTo.cName,
                issuedToOrganizationName = it.issuedTo.oName,
                issueDate = it.validNotBeforeDate,
                expireDate = it.validNotAfterDate,
                sslState = sslState
            )
        }

    override val sslState: SslState
        get() = tabWebViewClient.sslState

    override fun sslChanges(): Observable<SslState> = tabWebViewClient.sslStateObservable.hide()

    override val loadingProgress: Int
        get() = webView.progress

    override fun loadingProgress(): Observable<Int> = tabWebChromeClient.progressObservable.hide()

    override fun downloadRequests(): Observable<PendingDownload> = downloadsSubject.hide()

    override var isForeground: Boolean = false
        set(value) {
            field = value
            if (field) {
                webView.onResume()
                latentInitializer?.let(::loadFromInitializer)
                latentInitializer = null
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

    override fun freeze(): Bundle = latentInitializer?.bundle
        ?: Bundle(ClassLoader.getSystemClassLoader()).also(webView::saveState)
}
