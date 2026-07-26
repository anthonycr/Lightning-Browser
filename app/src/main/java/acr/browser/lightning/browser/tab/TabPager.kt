package acr.browser.lightning.browser.tab

import acr.browser.lightning.browser.di.BrowserFrame
import acr.browser.lightning.browser.di.BrowserScope
import acr.browser.lightning.browser.view.WebViewLongPressHandler
import acr.browser.lightning.browser.view.targetUrl.LongPress
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.core.view.children
import javax.inject.Inject

/**
 * A sort of coordinator that manages the relationship between [WebViews][WebView] and the container
 * the views are placed in.
 */
@BrowserScope
class TabPager @Inject constructor(
    @BrowserFrame private val container: FrameLayout,
    private val webViewLongPressHandler: WebViewLongPressHandler
) {

    private val webViews: MutableMap<Int, Lazy<WebView>> = mutableMapOf()

    var longPressListener: ((id: Int, longPress: LongPress) -> Unit)? = null

    /**
     * Select the tab with the provided [id] to be displayed by the pager.
     */
    fun selectTab(id: Int) {
        container.removeWebViews(excludeId = id)
        val webView = webViews[id]!!.value
        if (webView.parent != container) {
            container.addView(
                webView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

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
    fun addTab(id: Int, webView: Lazy<WebView>) {
        webViews[id] = webView
    }

    private fun FrameLayout.removeWebViews(excludeId: Int = -1) {
        children
            .filterIsInstance<WebView>()
            .filter { it.id != excludeId }
            .forEach(container::removeView)
    }

}
