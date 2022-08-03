package acr.browser.lightning.browser.view

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View

/**
 * Created by anthonycr on 12/23/20.
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
