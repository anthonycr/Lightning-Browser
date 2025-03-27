package acr.browser.lightning.browser.tab

import android.app.Application
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader.InternalStoragePathHandler
import java.io.File
import javax.inject.Inject

/**
 * Constructs a [TabModel].
 */
class TabFactory @Inject constructor(
    private val app: Application,
    private val webViewFactory: WebViewFactory,
    private val tabWebViewClientFactory: TabWebViewClient.Factory,
    private val tabAdapterFactory: TabAdapter.Factory
) {

    /**
     * Constructs a tab from the [webView] with the provided [tabInitializer].
     */
    fun constructTab(tabInitializer: TabInitializer, webView: WebView): TabModel {
        val headers = webViewFactory.createRequestHeaders()
        return tabAdapterFactory.create(
            tabInitializer = tabInitializer,
            webView = webView,
            requestHeaders = headers,
            tabWebViewClient = tabWebViewClientFactory.create(
                headers,
                InternalStoragePathHandler(app, File(app.cacheDir, "favicon-cache")),
                InternalStoragePathHandler(app, File(app.filesDir, "generated-html"))
            )
        )
    }
}
