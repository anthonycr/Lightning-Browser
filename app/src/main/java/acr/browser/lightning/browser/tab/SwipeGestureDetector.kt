package acr.browser.lightning.browser.tab

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener

/**
 * Created by anthonycr on 2/15/23.
 */
class SwipeGestureDetector(
    context: Context,
    private val onScrollStarted: () -> Unit,
    private val onScrollCompleted: (cumulative: Float) -> Unit,
    onFling: (velocityY: Float, positionY: Float) -> Unit,
    onScroll: (distance: Float) -> Unit
) : OnTouchListener {

    private class ScrollListener(
        private val onScroll: (distance: Float) -> Unit,
        private val onScrollStarted: () -> Unit,
        private val onFling: (velocityY: Float, positionY: Float) -> Unit
    ) : GestureDetector.SimpleOnGestureListener() {

        var scrollTotal = 0f
        var wasFlung = false
        var wasScrolled = false

        override fun onDown(e: MotionEvent): Boolean {
            scrollTotal = 0f
            wasFlung = false
            wasScrolled = false
            return super.onDown(e)
        }

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (!wasScrolled) {
                onScrollStarted()
            }
            wasScrolled = true
            scrollTotal += distanceY
            onScroll(distanceY)
            return true
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            wasFlung = true
            onFling(velocityY, e2.y)
            return true
        }
    }

    private val scrollListener = ScrollListener(onScroll, onScrollStarted, onFling)
    private val gestureDetector = GestureDetector(context, scrollListener)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val consumed = gestureDetector.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_UP && !scrollListener.wasFlung) {
            onScrollCompleted(scrollListener.scrollTotal)
        }
        return consumed
    }
}
