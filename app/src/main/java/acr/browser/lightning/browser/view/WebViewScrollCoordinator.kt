package acr.browser.lightning.browser.view

import acr.browser.lightning.interpolator.BezierDecelerateInterpolator
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.utils.Utils
import android.annotation.SuppressLint
import android.app.Activity
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.LinearLayout
import javax.inject.Inject

/**
 * Coordinates scrolling behavior between a [WebView] and a toolbar/search box.
 */
class WebViewScrollCoordinator @Inject constructor(
    activity: Activity,
    private val browserFrame: FrameLayout,
    private val toolbarRoot: LinearLayout,
    private val toolbar: View,
    private val userPreferences: UserPreferences,
    private val inputMethodManager: InputMethodManager
) {

    private val gestureListener: CustomGestureListener = CustomGestureListener(
        ViewConfiguration.get(activity).scaledMaximumFlingVelocity.toFloat()
    )

    private val touchListener = TouchListener(GestureDetector(activity, gestureListener))

    private var currentToggleListener: ToggleListener? = null

    /**
     * Configure the [webView] to match its scrolling behavior with showing an hiding the toolbar.
     */
    fun configure(webView: WebView) {
        webView.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                inputMethodManager.hideSoftInputFromWindow(v.windowToken, 0)
            }
        }
        if (userPreferences.fullScreenEnabled) {
            if (toolbar.parent != browserFrame) {
                (toolbar.parent as ViewGroup?)?.removeView(toolbar)

                browserFrame.addView(toolbar)
            }

            currentToggleListener?.showToolbar() ?: run {
                toolbar.translationY = 0f
            }

            webView.translationY = toolbar.height.toFloat()
            coordinate(toolbar, webView)
        } else {
            if (toolbar.parent != toolbarRoot) {
                (toolbar.parent as ViewGroup?)?.removeView(toolbar)

                toolbarRoot.addView(toolbar, 0)
            }

            toolbar.translationY = 0f
            webView.translationY = 0f
        }
    }

    /**
     * Show the toolbar if it is hidden via scrolling. Has no effect if the toolbar is already
     * visible.
     */
    fun showToolbar() {
        currentToggleListener?.showToolbar()
    }

    private fun coordinate(toolbar: View, webView: WebView) {
        webView.setCompositeTouchListener("scroll", touchListener)

        val toggleListener = object : ToggleListener {
            override fun hideToolbar() {
                val height = toolbar.height
                if (toolbar.translationY > -0.01f) {
                    val hideAnimation = object : Animation() {
                        override fun applyTransformation(
                            interpolatedTime: Float,
                            t: Transformation
                        ) {
                            val trans = interpolatedTime * height
                            toolbar.translationY = -trans
                            webView.translationY = height - trans
                        }
                    }
                    hideAnimation.duration = 250
                    hideAnimation.interpolator = BezierDecelerateInterpolator()
                    toolbar.startAnimation(hideAnimation)
                }
            }

            override fun showToolbar() {
                var height = toolbar.height
                if (height == 0) {
                    toolbar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                    height = toolbar.measuredHeight
                }

                val totalHeight = height
                if (toolbar.translationY < -(height - 0.01f)) {
                    val show = object : Animation() {
                        override fun applyTransformation(
                            interpolatedTime: Float,
                            t: Transformation
                        ) {
                            val trans = interpolatedTime * totalHeight
                            toolbar.translationY = trans - totalHeight
                            webView.translationY = trans
                        }
                    }
                    show.duration = 250
                    show.interpolator = BezierDecelerateInterpolator()
                    toolbar.startAnimation(show)
                }
            }
        }

        touchListener.toggleListener = toggleListener
        gestureListener.toggleListener = toggleListener
        currentToggleListener = toggleListener
    }

    private interface ToggleListener {
        fun hideToolbar()

        fun showToolbar()
    }

    private class TouchListener(
        private val gestureDetector: GestureDetector
    ) : View.OnTouchListener {

        private var location: Float = 0f
        private var y: Float = 0f
        private var action: Int = 0

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
                if (distance > SCROLL_UP_THRESHOLD && view.scrollY < SCROLL_UP_THRESHOLD) {
                    toggleListener?.showToolbar()
                } else if (distance < -SCROLL_UP_THRESHOLD) {
                    toggleListener?.hideToolbar()
                }
                location = 0f
            }
            gestureDetector.onTouchEvent(arg1)

            return false
        }
    }

    /**
     * The SimpleOnGestureListener used by the [TouchListener]
     * in order to delegate show/hide events to the action bar when
     * the user flings the page. Also handles long press events so
     * that we can capture them accurately.
     */
    private class CustomGestureListener(
        private val maxFling: Float
    ) : GestureDetector.SimpleOnGestureListener() {

        var toggleListener: ToggleListener? = null

        override fun onFling(
            e1: MotionEvent,
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

    companion object {
        private val SCROLL_UP_THRESHOLD = Utils.dpToPx(10f)
    }
}
