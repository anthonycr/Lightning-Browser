package acr.browser.lightning._browser2.view

import android.view.View
import android.webkit.WebView

/**
 * Created by anthonycr on 12/23/20.
 */


fun WebView.setCompositeTouchListener(key: String, onTouchListener: View.OnTouchListener?) {
    val composite = tag as CompositeTouchListener
    composite.delegates[key] = onTouchListener
}
