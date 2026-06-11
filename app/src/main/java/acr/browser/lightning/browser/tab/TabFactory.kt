package acr.browser.lightning.browser.tab

import acr.browser.lightning.browser.di.FaviconCacheDir
import acr.browser.lightning.browser.di.GeneratedHtmlDir
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.concurrency.TabCoroutineScope
import acr.browser.lightning.utils.ThreadSafeFileProvider
import android.app.Application
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader.InternalStoragePathHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Constructs a [TabModel].
 */
class TabFactory @Inject constructor(
    private val app: Application,
    private val webViewFactory: WebViewFactory,
    private val tabWebViewClientFactory: TabWebViewClient.Factory,
    private val tabAdapterFactory: TabAdapter.Factory,
    @FaviconCacheDir private val faviconCacheDirThreadSafeFileProvider: ThreadSafeFileProvider,
    @GeneratedHtmlDir private val generatedHtmlDirThreadSafeFileProvider: ThreadSafeFileProvider,
    private val coroutineDispatchers: CoroutineDispatchers,
) {

    /**
     * Constructs a tab from the [webView] with the provided [tabInitializer].
     */
    suspend fun constructTab(
        tabInitializer: TabInitializer,
        webView: Lazy<WebView>,
        tabType: TabModel.Type
    ): TabModel = withContext(coroutineDispatchers.main) {
        val faviconHandler = async(coroutineDispatchers.io) {
            InternalStoragePathHandler(
                app,
                faviconCacheDirThreadSafeFileProvider.file.await()
            )
        }
        val htmlHandler = async(coroutineDispatchers.io) {
            InternalStoragePathHandler(
                app,
                generatedHtmlDirThreadSafeFileProvider.file.await()
            )
        }

        val headers = webViewFactory.createRequestHeaders()
        val tabCoroutineScope = TabCoroutineScope(
            CoroutineScope(coroutineDispatchers.main + SupervisorJob())
        )
        tabAdapterFactory.create(
            tabInitializer = tabInitializer,
            webView = webView,
            requestHeaders = headers,
            tabWebViewClient = tabWebViewClientFactory.create(
                headers,
                faviconHandler.await(),
                htmlHandler.await(),
                tabCoroutineScope,
            ),
            tabType = tabType,
            tabCoroutineScope = tabCoroutineScope
        )
    }
}
