package acr.browser.lightning.browser.tab

import acr.browser.lightning.browser.view.CustomGestureListener
import acr.browser.lightning.browser.view.ToggleListener
import acr.browser.lightning.browser.view.TouchListener
import acr.browser.lightning.browser.view.setCompositeOnFocusChangeListener
import acr.browser.lightning.browser.view.setCompositeTouchListener
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.concurrency.TabCoroutineScope
import acr.browser.lightning.connectivity.ConnectivityProvider
import acr.browser.lightning.constant.DESKTOP_USER_AGENT
import acr.browser.lightning.download.PendingDownload
import acr.browser.lightning.ids.ViewIdGenerator
import acr.browser.lightning.preview.PreviewModel
import acr.browser.lightning.ssl.SslCertificateInfo
import acr.browser.lightning.ssl.SslState
import acr.browser.lightning.useragent.UserAgentProvider
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Message
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * Creates the adaptation between a [WebView] and the [TabModel] interface used by the browser.
 */
@SuppressLint("ClickableViewAccessibility")
class TabAdapter @AssistedInject constructor(
    @Assisted override val id: Int,
    @Assisted private val tabInitializer: TabInitializer,
    @Assisted private val webViewLazy: Lazy<WebView>,
    @Assisted private val requestHeaders: Map<String, String>,
    @Assisted private val tabWebViewClient: TabWebViewClient,
    @Assisted override var tabType: TabModel.Type,
    @Assisted private val tabCoroutineScope: TabCoroutineScope,
    private val tabWebChromeClientFactory: TabWebChromeClient.Factory,
    private val userAgentProvider: UserAgentProvider,
    private val viewIdGenerator: ViewIdGenerator,
    private val previewModel: PreviewModel,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val connectivityProvider: ConnectivityProvider,
) : TabModel {

    @AssistedFactory
    interface Factory {

        fun create(
            id: Int,
            tabInitializer: TabInitializer,
            webView: Lazy<WebView>,
            requestHeaders: Map<String, String>,
            tabWebViewClient: TabWebViewClient,
            tabType: TabModel.Type,
            tabCoroutineScope: TabCoroutineScope,
        ): TabAdapter
    }

    private var latentInitializer: FreezableInitializer? = tabInitializer as? FreezableInitializer

    private var findInPageQuery: String? = null
    private var toggleDesktop: Boolean = false
    private val downloadsShareFlow = MutableSharedFlow<PendingDownload>()
    private val focusSharedFlow = MutableSharedFlow<Unit>()
    private val showHideFlow = MutableSharedFlow<Boolean>()

    private val tabWebChromeClient by lazy { tabWebChromeClientFactory.create(tabCoroutineScope) }

    private val webViewLazyWithInitialization: WebView by lazy {
        webViewLazy.value.apply {
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

            setCompositeTouchListener("toggle", createToolbarAwareTouchListener(context))

            setCompositeTouchListener("focus") { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    tabCoroutineScope.launch {
                        focusSharedFlow.emit(Unit)
                    }
                }
                false
            }

            tabCoroutineScope.launch {
                connectivityProvider.hasInternetAccess.collectLatest {
                    setNetworkAvailable(it)
                }
            }
        }
    }

    private val previewPathDeferred = tabCoroutineScope.async {
        previewModel.previewForId(id)
    }

    private val webView: WebView
        get() = webViewLazyWithInitialization

    private val titleStateFlow = MutableStateFlow(
        latentInitializer?.initialTitle
    )

    private val faviconStateFlow = MutableStateFlow(
        latentInitializer?.let { TabModel.Favicon.Frozen } ?: TabModel.Favicon.None
    )

    private val previewStateFlow = MutableStateFlow<TabModel.Preview>(TabModel.Preview.None)

    init {
        if (tabInitializer !is FreezableInitializer) {
            loadFromInitializer(tabInitializer)
        }
        tabCoroutineScope.launch {
            merge(
                tabWebViewClient.startedSharedFlow.map { null },
                tabWebViewClient.finishedSharedFlow.map { webView.title },
                tabWebChromeClient.titleShareFlow
            ).collectLatest { titleStateFlow.emit(it) }
        }
        tabCoroutineScope.launch {
            merge(
                tabWebViewClient.startedSharedFlow.map { TabModel.Favicon.None },
                tabWebChromeClient.faviconSharedFlow
            ).collectLatest { faviconStateFlow.emit(it) }
        }
        tabCoroutineScope.launch {
            @OptIn(FlowPreview::class)
            tabWebViewClient.finishedSharedFlow
                .debounce(100.milliseconds)
                .map { renderViewToBitmap(webView) }
                .flowOn(coroutineDispatchers.main)
                .map { bitmap ->
                    if (bitmap != null) {
                        previewModel.cachePreviewForId(id, bitmap)
                        TabModel.Preview.Image(
                            previewPathDeferred.await(),
                            System.currentTimeMillis()
                        )
                    } else {
                        TabModel.Preview.None
                    }
                }
                .onStart {
                    emit(
                        TabModel.Preview.Image(
                            previewPathDeferred.await(),
                            System.currentTimeMillis()
                        )
                    )
                }
                .flowOn(coroutineDispatchers.io)
                .collectLatest { previewStateFlow.emit(it) }
        }
    }

    override fun loadUrl(url: String) {
        webView.loadUrl(url, requestHeaders)
    }

    override fun loadFromInitializer(tabInitializer: TabInitializer) {
        tabCoroutineScope.launch {
            tabInitializer.initialize(this@TabAdapter)
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
            userAgentProvider.getUserAgent()
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

    override val preview: TabModel.Preview
        get() = previewStateFlow.value

    override fun previewChanges(): StateFlow<TabModel.Preview> = previewStateFlow

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

    override val favicon: TabModel.Favicon
        get() = faviconStateFlow.value

    override fun faviconChanges(): StateFlow<TabModel.Favicon> = faviconStateFlow

    override val themeColor: Int
        get() = tabWebChromeClient.colorChangeStateFlow.value

    // TODO: Do something with theme color or drop it
    override fun themeColorChanges(): Flow<Int> {
        // Treat it like a SharedFlow for consistency on presenter side
        return tabWebChromeClient.colorChangeStateFlow.drop(1)
    }

    override val url: String
        get() = webView.url.orEmpty()

    override fun urlChanges(): Flow<String> = tabWebViewClient.urlSharedFlow

    override val title: String?
        get() = titleStateFlow.value

    override fun titleChanges(): StateFlow<String?> = titleStateFlow

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
        get() = tabWebViewClient.sslStateFlow.value

    override fun sslChanges(): StateFlow<SslState> = tabWebViewClient.sslStateFlow

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

    override fun handleMessage(message: Message) {
        message.apply {
            (obj as WebView.WebViewTransport).webView = webView
        }.sendToTarget()
    }

    override fun createWindowRequests(): Flow<TabInitializer> =
        tabWebChromeClient.createWindowSharedFlow

    override fun closeWindowRequests(): Flow<Unit> = tabWebChromeClient.closeWindowSharedFlow

    override fun focusRequests(): Flow<Unit> = focusSharedFlow
    override fun showHideToolbar(): Flow<Boolean> = showHideFlow

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
                webView.settings.offscreenPreRaster = false
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

    override fun restore(bundle: Bundle) {
        webView.restoreState(bundle)
    }

    override fun freeze(): Bundle = latentInitializer?.bundle
        ?: Bundle(ClassLoader.getSystemClassLoader()).also(webView::saveState)

    private fun createToolbarAwareTouchListener(context: Context): View.OnTouchListener {
        val gestureListener = CustomGestureListener(
            ViewConfiguration.get(context).scaledMaximumFlingVelocity.toFloat()
        )

        val touchListener = TouchListener(context, GestureDetector(context, gestureListener))

        val toggleListener = object : ToggleListener {
            override fun hideToolbar() {
                tabCoroutineScope.launch {
                    showHideFlow.emit(false)
                }
            }

            override fun showToolbar() {
                tabCoroutineScope.launch {
                    showHideFlow.emit(true)
                }
            }
        }

        gestureListener.toggleListener = toggleListener
        touchListener.toggleListener = toggleListener

        return touchListener
    }

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
