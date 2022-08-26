package acr.browser.lightning.browser.tab

import acr.browser.lightning.browser.image.IconFreeze
import acr.browser.lightning.browser.proxy.Proxy
import acr.browser.lightning.preference.UserPreferences
import android.graphics.Bitmap
import android.webkit.WebView
import javax.inject.Inject
import javax.inject.Provider

/**
 * Constructs a [TabModel].
 */
class TabFactory @Inject constructor(
    private val webViewFactory: WebViewFactory,
    private val userPreferences: UserPreferences,
    @DefaultUserAgent private val defaultUserAgent: String,
    @DefaultTabTitle private val defaultTabTitle: String,
    @IconFreeze private val iconFreeze: Bitmap,
    private val proxy: Proxy,
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
            tabWebViewClient = tabWebViewClientFactory.create(headers),
            tabWebChromeClient = tabWebChromeClientProvider.get(),
            userPreferences = userPreferences,
            defaultUserAgent = defaultUserAgent,
            defaultTabTitle = defaultTabTitle,
            iconFreeze = iconFreeze,
            proxy = proxy
        )
    }
}
