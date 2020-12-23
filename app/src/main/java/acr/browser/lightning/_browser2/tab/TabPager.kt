package acr.browser.lightning._browser2.tab

import acr.browser.lightning._browser2.view.WebViewScrollCoordinator
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import javax.inject.Inject

/**
 * Created by anthonycr on 9/12/20.
 */
class TabPager @Inject constructor(
    private val container: FrameLayout,
    private val webViewScrollCoordinator: WebViewScrollCoordinator
) {

    private val webViews: MutableList<WebView> = mutableListOf()

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
    }

    fun clearTab() {
        container.removeAllViews()
    }

    fun addTab(webView: WebView) {
        webViews.add(webView)
    }

    private fun List<WebView>.forId(id: Int): WebView = requireNotNull(find { it.id == id })

}
