package acr.browser.lightning.browser.view

import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.View.OnTouchListener
import android.webkit.WebView

/**
 * A helper for adding a [View.OnTouchListener] to the [CompositeTouchListener] of a [WebView]
 * marked by the unique [key].
 */
fun WebView.setCompositeTouchListener(key: String, onTouchListener: OnTouchListener?) {
    val composite = tag as CompositeTouchListener
    composite.delegates[key] = onTouchListener
}

/**
 * Helper for adding a [View.OnFocusChangeListener] to the [CompositeOnFocusChangeListener] of a
 * [WebView].
 */
fun WebView.setCompositeOnFocusChangeListener(key: String, listener: OnFocusChangeListener) {
    onFocusChangeListener = when (val current = onFocusChangeListener) {
        is CompositeOnFocusChangeListener -> {
            val newMap = current.delegates.toMutableMap()
            newMap[key] = listener
            CompositeOnFocusChangeListener(newMap)
        }

        else -> if (current == null) {
            CompositeOnFocusChangeListener(mapOf(key to listener))
        } else {
            error("WebView already has an onFocusChangeListener")
        }
    }
}

private class CompositeOnFocusChangeListener(val delegates: Map<String, OnFocusChangeListener>) :
    OnFocusChangeListener {
    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        delegates.values.forEach { it.onFocusChange(v, hasFocus) }
    }
}
