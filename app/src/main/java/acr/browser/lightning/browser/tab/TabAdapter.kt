package acr.browser.lightning.browser.tab

import acr.browser.lightning.browser.download.PendingDownload
import acr.browser.lightning.browser.image.IconFreeze
import acr.browser.lightning.browser.view.setCompositeOnFocusChangeListener
import acr.browser.lightning.browser.view.setCompositeTouchListener
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.concurrency.TabCoroutineScope
import acr.browser.lightning.constant.DESKTOP_USER_AGENT
import acr.browser.lightning.ids.ViewIdGenerator
import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.preference.userAgent
import acr.browser.lightning.preview.PreviewModel
import acr.browser.lightning.ssl.SslCertificateInfo
import acr.browser.lightning.ssl.SslState
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * Creates the adaptation between a [WebView] and the [TabModel] interface used by the browser.
 */
@SuppressLint("ClickableViewAccessibility")
class TabAdapter @AssistedInject constructor(
    @Assisted private val tabInitializer: TabInitializer,
    @Assisted private val webViewLazy: Lazy<WebView>,
    @Assisted private val requestHeaders: Map<String, String>,
    @Assisted private val tabWebViewClient: TabWebViewClient,
    @Assisted override var tabType: TabModel.Type,
    @Assisted private val tabCoroutineScope: TabCoroutineScope,
    private val tabWebChromeClientFactory: TabWebChromeClient.Factory,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    @DefaultUserAgent private val defaultUserAgent: String,
    @DefaultTabTitle private val defaultTabTitle: String,
    @IconFreeze private val iconFreeze: Bitmap,
    private val viewIdGenerator: ViewIdGenerator,
    private val previewModel: PreviewModel,
    private val coroutineDispatchers: CoroutineDispatchers,
) : TabModel {

    @AssistedFactory
    interface Factory {

        fun create(
            tabInitializer: TabInitializer,
            webView: Lazy<WebView>,
            requestHeaders: Map<String, String>,
            tabWebViewClient: TabWebViewClient,
            tabType: TabModel.Type,
            tabCoroutineScope: TabCoroutineScope,
        ): TabAdapter
    }

    private var latentInitializer: FreezableInitializer? = null

    private var findInPageQuery: String? = null
    private var toggleDesktop: Boolean = false
    private val downloadsShareFlow = MutableSharedFlow<PendingDownload>()
    private val focusSharedFlow = MutableSharedFlow<Unit>()

    private var previewGeneratedTime = System.currentTimeMillis()

    override val id: Int = if (tabInitializer is FreezableInitializer) {
        latentInitializer = tabInitializer
        val frozenId = tabInitializer.id.takeIf { it != -1 } ?: viewIdGenerator.generateViewId()
        viewIdGenerator.claimViewId(frozenId)
        frozenId
    } else {
        viewIdGenerator.generateViewId()
    }

    private val tabWebChromeClient by lazy { tabWebChromeClientFactory.create(tabCoroutineScope) }

    private val webView: WebView
        get() = webViewLazy.value.apply {
            webViewClient = tabWebViewClient
            webChromeClient = tabWebChromeClient
            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                tabCoroutineScope.launch {
                    downloadsShareFlow.emit(
                        PendingDownload(
                            url = url,
                            userAgent = userAgent,
                            contentDisposition = contentDisposition,
                            mimeType = mimetype,
                            contentLength = contentLength
                        )
                    )
                }
            }
            id = this@TabAdapter.id

            setCompositeOnFocusChangeListener("focus_change") { _, hasFocus ->
                tabCoroutineScope.launch {
                    if (hasFocus) {
                        focusSharedFlow.emit(Unit)
                    }
                }
            }

            setCompositeTouchListener("focus") { view, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    if (!view.hasFocus()) {
                        view.requestFocus()
                    }
                    tabCoroutineScope.launch {
                        focusSharedFlow.emit(Unit)
                    }
                }
                false
            }
        }

    init {
        if (tabInitializer !is FreezableInitializer) {
            loadFromInitializer(tabInitializer)
        }
    }

    private var previewPath: String? = null
    private val previewPathDeferred = tabCoroutineScope.async {
        previewModel.previewForId(id)
    }

    override fun loadUrl(url: String) {
        webView.loadUrl(url, requestHeaders)
    }

    override fun loadFromInitializer(tabInitializer: TabInitializer) {
        tabCoroutineScope.launch {
            tabInitializer.initialize(webView, requestHeaders)
        }
    }

    override fun goBack() {
        webView.goBack()
    }

    override fun canGoBack(): Boolean = webView.canGoBack()

    override fun canGoBackChanges(): Flow<Boolean> = tabWebViewClient.goBackSharedFlow

    override fun goForward() {
        webView.goForward()
    }

    override fun canGoForward(): Boolean = webView.canGoForward()

    override fun canGoForwardChanges(): Flow<Boolean> = tabWebViewClient.goForwardSharedFlow

    override suspend fun toggleDesktopAgent() {
        webView.settings.userAgentString = if (!toggleDesktop) {
            DESKTOP_USER_AGENT
        } else {
            userPreferencesDataStore.userAgent(defaultUserAgent)
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

    @OptIn(FlowPreview::class)
    override fun previewChanges(): Flow<Pair<String?, Long>> =
        tabWebViewClient.finishedSharedFlow
            .debounce(100.milliseconds)
            .map { renderViewToBitmap(webView) }
            .flowOn(coroutineDispatchers.main)
            .map { bitmap ->
                if (bitmap != null) {
                    previewModel.cachePreviewForId(id, bitmap)
                    previewPathDeferred.await() to System.currentTimeMillis()
                } else {
                    null to System.currentTimeMillis()
                }
            }
            .onStart { emit(previewPathDeferred.await() to System.currentTimeMillis()) }
            .onEach { (path, time) ->
                previewPath = path
                previewGeneratedTime = time
            }
            .flowOn(coroutineDispatchers.io)

    override val findQuery: String?
        get() = findInPageQuery

    override var searchQuery: String
        get() = tabWebViewClient.searchQuery
        set(value) {
            tabWebViewClient.searchQuery = value
        }

    override var searchQuerySelection: Pair<Int, Int>
        get() = tabWebViewClient.searchQuerySelection
        set(value) {
            tabWebViewClient.searchQuerySelection = value
        }

    override val favicon: Bitmap?
        get() = latentInitializer?.let { iconFreeze }
            ?: tabWebChromeClient.faviconStateFlow.value

    override fun faviconChanges(): Flow<Bitmap?> {
        // Treat it like a SharedFlow for consistency on presenter side and because frozen tabs have
        // their own icon that the chrome client doesn't know about.
        return tabWebChromeClient.faviconStateFlow.drop(1)
    }

    override val themeColor: Int
        get() = tabWebChromeClient.colorChangeStateFlow.value

    override fun themeColorChanges(): Flow<Int> {
        // Treat it like a SharedFlow for consistency on presenter side
        return tabWebChromeClient.colorChangeStateFlow.drop(1)
    }

    override val url: String
        get() = webView.url.orEmpty()

    override fun urlChanges(): Flow<String> = tabWebViewClient.urlSharedFlow

    override val title: String
        get() = latentInitializer?.initialTitle ?: webView.title?.takeIf(String::isNotBlank)
        ?: defaultTabTitle

    override fun titleChanges(): Flow<String> = tabWebChromeClient.titleShareFlow

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

    override fun sslChanges(): Flow<SslState> = tabWebViewClient.sslStateSharedFlow

    override val loadingProgress: Int
        get() = webView.progress

    override fun loadingProgress(): Flow<Int> = tabWebChromeClient.progressSharedFlow

    override fun downloadRequests(): Flow<PendingDownload> = downloadsShareFlow

    override fun fileChooserRequests(): Flow<Intent> = tabWebChromeClient.fileChooserSharedFlow

    override fun handleFileChooserResult(activityResult: ActivityResult) {
        tabWebChromeClient.onResult(activityResult)
    }

    override fun showCustomViewRequests(): Flow<Unit> = tabWebChromeClient.showCustomViewSharedFlow

    override fun hideCustomViewRequests(): Flow<Unit> = tabWebChromeClient.hideCustomViewObservable

    override fun hideCustomView() {
        tabWebChromeClient.hideCustomView()
    }

    override fun createWindowRequests(): Flow<TabInitializer> =
        tabWebChromeClient.createWindowSharedFlow

    override fun closeWindowRequests(): Flow<Unit> = tabWebChromeClient.closeWindowSharedFlow

    override fun focusRequests(): Flow<Unit> = focusSharedFlow

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

    override fun destroy() {
        viewIdGenerator.releaseViewId(id)
        previewModel.prune()
        webView.stopLoading()
        webView.onPause()
        webView.clearHistory()
        webView.removeAllViews()
        webView.destroy()
        tabCoroutineScope.cancel()
    }

    override fun freeze(): Bundle = latentInitializer?.bundle
        ?: Bundle(ClassLoader.getSystemClassLoader()).also(webView::saveState)

    private suspend fun renderViewToBitmap(
        view: View,
        width: Int = view.width,
        height: Int = view.height
    ): Bitmap? = withContext(coroutineDispatchers.main) {
        // Ensure the view has been laid out
        if (width == 0 || height == 0) {
            return@withContext null
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

        return@withContext bitmap
    }
}
