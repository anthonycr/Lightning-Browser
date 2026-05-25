package acr.browser.lightning.browser.tab

import acr.browser.lightning.browser.di.CacheDir
import acr.browser.lightning.browser.di.FilesDir
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
import java.io.File
import javax.inject.Inject

/**
 * Constructs a [TabModel].
 */
class TabFactory @Inject constructor(
    private val app: Application,
    private val webViewFactory: WebViewFactory,
    private val tabWebViewClientFactory: TabWebViewClient.Factory,
    private val tabAdapterFactory: TabAdapter.Factory,
    @CacheDir private val cacheDirThreadSafeFileProvider: ThreadSafeFileProvider,
    @FilesDir private val filesDirThreadSafeFileProvider: ThreadSafeFileProvider,
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
                File(cacheDirThreadSafeFileProvider.file.await(), "favicon-cache")
            )
        }
        val htmlHandler = async(coroutineDispatchers.io) {
            InternalStoragePathHandler(
                app,
                File(filesDirThreadSafeFileProvider.file.await(), "generated-html")
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
