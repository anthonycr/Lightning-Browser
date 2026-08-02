package acr.browser.lightning.browser.tab

import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.concurrency.TabCoroutineScope
import acr.browser.lightning.di.FaviconCacheDir
import acr.browser.lightning.di.GeneratedHtmlDir
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader.InternalStoragePathHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Constructs a [TabModel].
 */
class TabFactory @Inject constructor(
    private val webViewFactory: WebViewFactory,
    private val tabWebViewClientFactory: TabWebViewClient.Factory,
    private val tabAdapterFactory: TabAdapter.Factory,
    @FaviconCacheDir private val faviconStorageHandler: Deferred<@JvmSuppressWildcards InternalStoragePathHandler>,
    @GeneratedHtmlDir private val htmlStorageHandler: Deferred<@JvmSuppressWildcards InternalStoragePathHandler>,
    private val coroutineDispatchers: CoroutineDispatchers,
) {

    /**
     * Constructs a tab from the [webView] with the provided [tabInitializer].
     */
    suspend fun constructTab(
        tabInitializer: TabInitializer,
        webView: Lazy<WebView>,
        tabType: TabModel.Type,
        tabSettings: TabSettings,
    ): TabModel = withContext(coroutineDispatchers.main) {
        val headers = webViewFactory.createRequestHeaders()
        val tabCoroutineScope = TabCoroutineScope(
            CoroutineScope(coroutineDispatchers.main + SupervisorJob())
        )
        tabAdapterFactory.create(
            tabInitializer = tabInitializer,
            webView = webView,
            requestHeaders = headers,
            tabWebViewClient = tabWebViewClientFactory.create(
                headers = headers,
                cacheStoragePathHandler = faviconStorageHandler.await(),
                filesStoragePathHandler = htmlStorageHandler.await(),
                tabCoroutineScope = tabCoroutineScope,
                tabSettings = tabSettings,
            ),
            tabType = tabType,
            tabCoroutineScope = tabCoroutineScope
        )
    }
}
