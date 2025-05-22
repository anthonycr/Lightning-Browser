package acr.browser.lightning.browser.tab

import acr.browser.lightning.browser.di.DiskScheduler
import acr.browser.lightning.browser.di.MainScheduler
import acr.browser.lightning.browser.download.PendingDownload
import acr.browser.lightning.browser.image.IconFreeze
import acr.browser.lightning.browser.view.setCompositeOnFocusChangeListener
import acr.browser.lightning.browser.view.setCompositeTouchListener
import acr.browser.lightning.constant.DESKTOP_USER_AGENT
import acr.browser.lightning.ids.ViewIdGenerator
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.preference.userAgent
import acr.browser.lightning.preview.PreviewModel
import acr.browser.lightning.ssl.SslCertificateInfo
import acr.browser.lightning.ssl.SslState
import acr.browser.lightning.utils.Option
import acr.browser.lightning.utils.value
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import androidx.activity.result.ActivityResult
import androidx.core.graphics.createBitmap
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.Optional
import java.util.concurrent.TimeUnit


/**
 * Creates the adaptation between a [WebView] and the [TabModel] interface used by the browser.
 */
@SuppressLint("ClickableViewAccessibility")
class TabAdapter @AssistedInject constructor(
    @Assisted tabInitializer: TabInitializer,
    @Assisted private val webView: WebView,
    @Assisted private val requestHeaders: Map<String, String>,
    @Assisted private val tabWebViewClient: TabWebViewClient,
    @Assisted override var tabType: TabModel.Type,
    private val tabWebChromeClient: TabWebChromeClient,
    private val userPreferences: UserPreferences,
    @DefaultUserAgent private val defaultUserAgent: String,
    @DefaultTabTitle private val defaultTabTitle: String,
    @IconFreeze private val iconFreeze: Bitmap,
    private val viewIdGenerator: ViewIdGenerator,
    private val previewModel: PreviewModel,
    @DiskScheduler private val diskScheduler: Scheduler,
    @MainScheduler private val mainScheduler: Scheduler,
) : TabModel {

    @AssistedFactory
    interface Factory {

        fun create(
            tabInitializer: TabInitializer,
            webView: WebView,
            requestHeaders: Map<String, String>,
            tabWebViewClient: TabWebViewClient,
            tabType: TabModel.Type,
        ): TabAdapter
    }

    private var latentInitializer: FreezableBundleInitializer? = null

    private var findInPageQuery: String? = null
    private var toggleDesktop: Boolean = false
    private val downloadsSubject = PublishSubject.create<PendingDownload>()
    private val focusObservable = BehaviorSubject.createDefault(webView.hasFocus())

    private var previewGeneratedTime = System.currentTimeMillis()

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
            webView.id = tabInitializer.id.takeIf { it != -1 } ?: viewIdGenerator.generateViewId()
            viewIdGenerator.claimViewId(tabInitializer.id)
        } else {
            webView.id = viewIdGenerator.generateViewId()
            loadFromInitializer(tabInitializer)
        }

        webView.setCompositeOnFocusChangeListener("focus_change") { _, hasFocus ->
            focusObservable.onNext(hasFocus)
        }

        webView.setCompositeTouchListener("focus") { view, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (!view.hasFocus()) {
                    view.requestFocus()
                }
                focusObservable.onNext(true)
            }
            false
        }
    }

    private var previewPath: String? = null
    private val previewPathSingle = previewModel.previewForId(webView.id).cache()

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

    override val preview: Pair<String?, Long>
        get() = previewPath to previewGeneratedTime

    override fun previewChanges(): Observable<Pair<String?, Long>> =
        tabWebViewClient.finishedObservable
            .debounce(100, TimeUnit.MILLISECONDS)
            .observeOn(diskScheduler)
            .mapOptional { Optional.ofNullable(renderViewToBitmap(webView)) }
            .flatMapSingle { bitmap ->
                previewModel.cachePreviewForId(webView.id, bitmap)
                    .andThen(previewPathSingle)
                    .map<Pair<String?, Long>> { path -> path to System.currentTimeMillis() }
            }
            .startWith(
                previewPathSingle.ignoreElement()
                    .andThen(previewPathSingle)
                    .map { path -> path to System.currentTimeMillis() }
            )
            .doOnNext { (path, time) ->
                previewPath = path
                previewGeneratedTime = time
            }
            .observeOn(mainScheduler)

    override val findQuery: String?
        get() = findInPageQuery

    override val favicon: Bitmap?
        get() = latentInitializer?.let { iconFreeze }
            ?: tabWebChromeClient.faviconObservable.value?.value()

    override fun faviconChanges(): Observable<Option<Bitmap>> = tabWebChromeClient.faviconObservable

    override val themeColor: Int
        get() = requireNotNull(tabWebChromeClient.colorChangeObservable.value)

    override fun themeColorChanges(): Observable<Int> = tabWebChromeClient.colorChangeObservable

    override val url: String
        get() = webView.url.orEmpty()

    override fun urlChanges(): Observable<String> = tabWebViewClient.urlObservable.hide()

    override val title: String
        get() = latentInitializer?.initialTitle ?: webView.title?.takeIf(String::isNotBlank)
        ?: defaultTabTitle

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

    override fun fileChooserRequests(): Observable<Intent> =
        tabWebChromeClient.fileChooserObservable.hide()

    override fun handleFileChooserResult(activityResult: ActivityResult) {
        tabWebChromeClient.onResult(activityResult)
    }

    override fun showCustomViewRequests(): Observable<View> =
        tabWebChromeClient.showCustomViewObservable.hide()

    override fun hideCustomViewRequests(): Observable<Unit> =
        tabWebChromeClient.hideCustomViewObservable.hide()

    override fun hideCustomView() {
        tabWebChromeClient.hideCustomView()
    }

    override fun createWindowRequests(): Observable<TabInitializer> =
        tabWebChromeClient.createWindowObservable.hide()

    override fun closeWindowRequests(): Observable<Unit> =
        tabWebChromeClient.closeWindowObservable.hide()

    override var isForeground: Boolean = false
        set(value) {
            field = value
            if (field) {
                webView.onResume()
                webView.settings.offscreenPreRaster = true
                latentInitializer?.let(::loadFromInitializer)
                latentInitializer = null
            } else {
                webView.onPause()
            }
        }

    override val hasFocus: Boolean
        get() = webView.hasFocus()

    override fun hasFocusChanges(): Observable<Boolean> = focusObservable.hide()

    override fun destroy() {
        viewIdGenerator.releaseViewId(webView.id)
        previewModel.prune()
        webView.stopLoading()
        webView.onPause()
        webView.clearHistory()
        webView.removeAllViews()
        webView.destroy()
    }

    override fun freeze(): Bundle = latentInitializer?.bundle
        ?: Bundle(ClassLoader.getSystemClassLoader()).also(webView::saveState)

    private fun renderViewToBitmap(
        view: View,
        width: Int = view.width,
        height: Int = view.height
    ): Bitmap? {
        // Ensure the view has been laid out
        if (width == 0 || height == 0) {
            return null
        }

        // Create a Bitmap with the specified dimensions and ARGB_8888 configuration
        val bitmap = createBitmap(width / 3, height / 3)

        // Create a Canvas to draw on the Bitmap
        val canvas = Canvas(bitmap)

        canvas.scale(0.33F, 0.33F)

        canvas.translate(-webView.scrollX.toFloat(), -webView.scrollY.toFloat())

        // Layout the view if it hasn't been laid out yet
        view.layout(0, 0, width, height)

        // Draw the view onto the canvas
        view.draw(canvas)

        return bitmap
    }
}
