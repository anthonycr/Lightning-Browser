package acr.browser.lightning.browser.tab

import acr.browser.lightning.browser.di.Browser2Scope
import acr.browser.lightning.browser.view.WebViewLongPressHandler
import acr.browser.lightning.browser.view.WebViewScrollCoordinator
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.core.view.children
import acr.browser.lightning.browser.view.targetUrl.LongPress
import javax.inject.Inject

/**
 * A sort of coordinator that manages the relationship between [WebViews][WebView] and the container
 * the views are placed in.
 */
@Browser2Scope
class TabPager @Inject constructor(
    private val container: FrameLayout,
    private val webViewScrollCoordinator: WebViewScrollCoordinator,
    private val webViewLongPressHandler: WebViewLongPressHandler
) {

    private val webViews: MutableList<WebView> = mutableListOf()

    var longPressListener: ((id: Int, longPress: LongPress) -> Unit)? = null

    /**
     * Select the tab with the provided [id] to be displayed by the pager.
     */
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

    /**
     * Clear the container of the [WebView] currently shown.
     */
    fun clearTab() {
        container.removeWebViews()
    }

    /**
     * Add a [WebView] to the list of views shown by this pager.
     */
    fun addTab(webView: WebView) {
        webViews.add(webView)
    }

    /**
     * Show the toolbar/search box if it is currently hidden.
     */
    fun showToolbar() {
        webViewScrollCoordinator.showToolbar()
    }

    private fun FrameLayout.removeWebViews() {
        children.filterIsInstance<WebView>().forEach(container::removeView)
    }

    private fun List<WebView>.forId(id: Int): WebView = requireNotNull(find { it.id == id })

}
