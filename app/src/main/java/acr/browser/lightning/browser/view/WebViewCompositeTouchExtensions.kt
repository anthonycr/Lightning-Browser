package acr.browser.lightning.browser.view

import android.view.View
import android.webkit.WebView

/**
 * A helper for adding a [View.OnTouchListener] to the [CompositeTouchListener] of a [WebView]
 * marked by the unique [key].
 */
fun WebView.setCompositeTouchListener(key: String, onTouchListener: View.OnTouchListener?) {
    val composite = tag as CompositeTouchListener
    composite.delegates[key] = onTouchListener
}
