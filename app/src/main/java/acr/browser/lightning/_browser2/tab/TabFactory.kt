package acr.browser.lightning._browser2.tab

import acr.browser.lightning._browser2.image.IconFreeze
import acr.browser.lightning._browser2.proxy.Proxy
import acr.browser.lightning.adblock.AdBlocker
import acr.browser.lightning.adblock.allowlist.AllowListModel
import acr.browser.lightning._browser2.di.DiskScheduler
import acr.browser.lightning.favicon.FaviconModel
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.view.TabInitializer
import acr.browser.lightning.view.webrtc.WebRtcPermissionsModel
import android.app.Activity
import android.graphics.Bitmap
import android.webkit.WebView
import io.reactivex.Scheduler
import javax.inject.Inject

/**
 * Created by anthonycr on 8/3/22.
 */
class TabFactory @Inject constructor(
    private val webViewFactory: WebViewFactory,
    private val activity: Activity,
    private val adBlocker: AdBlocker,
    private val allowListModel: AllowListModel,
    private val faviconModel: FaviconModel,
    @DiskScheduler private val diskScheduler: Scheduler,
    private val urlHandler: UrlHandler,
    private val userPreferences: UserPreferences,
    @DefaultUserAgent private val defaultUserAgent: String,
    @IconFreeze private val iconFreeze: Bitmap,
    private val proxy: Proxy,
    private val webRtcPermissionsModel: WebRtcPermissionsModel
) {

    fun constructTab(tabInitializer: TabInitializer, webView: WebView): TabModel {
        val headers = webViewFactory.createRequestHeaders()
        return TabAdapter(
            tabInitializer,
            webView,
            headers,
            TabWebViewClient(adBlocker, allowListModel, urlHandler, headers, proxy),
            TabWebChromeClient(
                activity,
                faviconModel,
                diskScheduler,
                userPreferences,
                webRtcPermissionsModel
            ),
            userPreferences,
            defaultUserAgent,
            iconFreeze,
            proxy
        )
    }
}
