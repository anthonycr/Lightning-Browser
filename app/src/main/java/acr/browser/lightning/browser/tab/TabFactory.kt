package acr.browser.lightning.browser.tab

import acr.browser.lightning.browser.image.IconFreeze
import acr.browser.lightning.preference.UserPreferences
import android.app.Application
import android.graphics.Bitmap
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.InternalStoragePathHandler
import java.io.File
import javax.inject.Inject
import javax.inject.Provider

/**
 * Constructs a [TabModel].
 */
class TabFactory @Inject constructor(
    private val app: Application,
    private val webViewFactory: WebViewFactory,
    private val userPreferences: UserPreferences,
    @DefaultUserAgent private val defaultUserAgent: String,
    @DefaultTabTitle private val defaultTabTitle: String,
    @IconFreeze private val iconFreeze: Bitmap,
    private val tabWebViewClientFactory: TabWebViewClient.Factory,
    private val tabWebChromeClientProvider: Provider<TabWebChromeClient>
) {

    /**
     * Constructs a tab from the [webView] with the provided [tabInitializer].
     */
    fun constructTab(tabInitializer: TabInitializer, webView: WebView): TabModel {
        val headers = webViewFactory.createRequestHeaders()
        return TabAdapter(
            tabInitializer = tabInitializer,
            webView = webView,
            requestHeaders = headers,
            tabWebViewClient = tabWebViewClientFactory.create(
                headers,
                WebViewAssetLoader.Builder()
                    .addPathHandler(
                        "/favicon-cache/",
                        InternalStoragePathHandler(app, File(app.cacheDir, "favicon-cache"))
                    )
                    .addPathHandler(
                        "/generated-html/",
                        InternalStoragePathHandler(app, File(app.filesDir, "generated-html"))
                    )
                    .build()
            ),
            tabWebChromeClient = tabWebChromeClientProvider.get(),
            userPreferences = userPreferences,
            defaultUserAgent = defaultUserAgent,
            defaultTabTitle = defaultTabTitle,
            iconFreeze = iconFreeze
        )
    }
}
