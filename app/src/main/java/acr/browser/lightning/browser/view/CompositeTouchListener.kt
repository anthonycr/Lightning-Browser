package acr.browser.lightning.browser.view

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View

/**
 * A composite [View.OnTouchListener] that delegates touches to multiple listeners.
 *
 * @param delegates The actual listeners we are delegating to.
 */
class CompositeTouchListener(
    val delegates: MutableMap<String, View.OnTouchListener?> = mutableMapOf()
) : View.OnTouchListener {

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        delegates.values.forEach { it?.onTouch(v, event) }
        return false
    }

}
