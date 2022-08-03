package acr.browser.lightning._browser2.tab

import acr.browser.lightning._browser2.di.Browser2Scope
import acr.browser.lightning._browser2.view.WebViewLongPressHandler
import acr.browser.lightning._browser2.view.WebViewScrollCoordinator
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.core.view.children
import acr.browser.lightning._browser2.view.targetUrl.LongPress
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
        container.removeWebViews()
        val webView = webViews.forId(id)
        container.addView(
            webView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        webViewScrollCoordinator.configure(webView)
        webViewLongPressHandler.configure(webView, onLongClick = {
            longPressListener?.invoke(id, it)
        })
    }

    fun clearTab() {
        container.removeWebViews()
    }

    fun addTab(webView: WebView) {
        webViews.add(webView)
    }

    fun showToolbar() {
        webViewScrollCoordinator.showToolbar()
    }

    private fun FrameLayout.removeWebViews() {
        children.filterIsInstance<WebView>().forEach(container::removeView)
    }

    private fun List<WebView>.forId(id: Int): WebView = requireNotNull(find { it.id == id })

}
