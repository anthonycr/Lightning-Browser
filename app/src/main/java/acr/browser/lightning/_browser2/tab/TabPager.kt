package acr.browser.lightning._browser2.tab

import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout

/**
 * Created by anthonycr on 9/12/20.
 */
class TabPager(private val container: FrameLayout) {

    private val webViews: MutableList<WebView> = mutableListOf()

    fun selectTab(id: Int) {
        container.removeAllViews()
        container.addView(
            webViews.forId(id),
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    fun clearTab() {
        container.removeAllViews()
    }

    fun addTab(webView: WebView) {
        webViews.add(webView)
    }

    private fun List<WebView>.forId(id: Int): WebView = requireNotNull(find { it.id == id })

}
