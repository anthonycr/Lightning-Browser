package acr.browser.lightning.browser.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Used to notify when the toolbar should show or hide.
 */
interface ToggleListener {
    /**
     * The toolbar should be hidden.
     */
    fun hideToolbar()

    /**
     * The toolbar should be shown.
     */
    fun showToolbar()
}

/**
 * Handles touches and delegates gestures so that [ToggleListener] is invoked when the toolbar
 * should show or hide.
 */
class TouchListener(
    context: Context,
    private val gestureDetector: GestureDetector
) : View.OnTouchListener {

    private val scrollUpThreshold = with(Density(context)) {
        10.dp.toPx().roundToInt()
    }

    private var location: Float = 0f
    private var y: Float = 0f
    private var action: Int = 0

    /**
     * The listener to notify.
     */
    var toggleListener: ToggleListener? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View?, arg1: MotionEvent): Boolean {
        if (view == null) return false

        if (!view.hasFocus()) {
            view.requestFocus()
        }
        action = arg1.action
        y = arg1.y
        if (action == MotionEvent.ACTION_DOWN) {
            location = y
        } else if (action == MotionEvent.ACTION_UP) {
            val distance = y - location
            if (distance > scrollUpThreshold && view.scrollY < scrollUpThreshold) {
                toggleListener?.showToolbar()
            } else if (distance < -scrollUpThreshold) {
                toggleListener?.hideToolbar()
            }
            location = 0f
        }
        gestureDetector.onTouchEvent(arg1)

        return false
    }
}

/**
 * The SimpleOnGestureListener used by the [TouchListener] in order to delegate show/hide events to
 * the action bar when the user flings the page. Also handles long press events so that we can
 * capture them accurately.
 */
class CustomGestureListener(
    private val maxFling: Float
) : GestureDetector.SimpleOnGestureListener() {

    /**
     * The listener to notify.
     */
    var toggleListener: ToggleListener? = null

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        val power = (velocityY * 100 / maxFling).toInt()
        if (power < -10) {
            toggleListener?.hideToolbar()
        } else if (power > 15) {
            toggleListener?.showToolbar()
        }
        return super.onFling(e1, e2, velocityX, velocityY)
    }
}
