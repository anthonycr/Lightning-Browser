package acr.browser.lightning._browser2.tab

import acr.browser.lightning._browser2.di.Browser2Scope
import acr.browser.lightning._browser2.view.WebViewLongPressHandler
import acr.browser.lightning._browser2.view.WebViewScrollCoordinator
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import targetUrl.LongPress
import javax.inject.Inject

/**
 * Created by anthonycr on 9/12/20.
 */
@Browser2Scope
class TabPager @Inject constructor(
    private val container: FrameLayout,
    private val webViewScrollCoordinator: WebViewScrollCoordinator,
    private val webViewLongPressHandler: WebViewLongPressHandler
) {

    private val webViews: MutableList<WebView> = mutableListOf()

    var longPressListener: ((id: Int, longPress: LongPress) -> Unit)? = null

    fun selectTab(id: Int) {
        container.removeAllViews()
        val webView = webViews.forId(id)
        container.addView(
            webView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // TODO: coordinator adds views to the container, which can result in UI flashes.
        webViewScrollCoordinator.configure(webView)
        webViewLongPressHandler.configure(webView, onLongClick = {
            longPressListener?.invoke(id, it)
        })
    }

    fun clearTab() {
        container.removeAllViews()
    }

    fun addTab(webView: WebView) {
        webViews.add(webView)
    }

    private fun List<WebView>.forId(id: Int): WebView = requireNotNull(find { it.id == id })

}
