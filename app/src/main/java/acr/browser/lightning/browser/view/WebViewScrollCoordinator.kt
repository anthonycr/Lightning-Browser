package acr.browser.lightning.browser.view

import acr.browser.lightning.browser.tab.SwipeGestureDetector
import acr.browser.lightning.databinding.BrowserActivity2Binding
import acr.browser.lightning.interpolator.BezierDecelerateInterpolator
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.utils.Utils
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.DecelerateInterpolator
import android.view.animation.Transformation
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.core.view.doOnLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import javax.inject.Inject
import kotlin.math.hypot
import kotlin.math.roundToLong
import kotlin.math.sqrt

/**
 * Coordinates scrolling behavior between a [WebView] and a toolbar/search box.
 */
class WebViewScrollCoordinator @Inject constructor(
    private val activity: Activity,
    private val binding: BrowserActivity2Binding,
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
        if (true) {
            coordinateModernUi()
        } else if (userPreferences.fullScreenEnabled) {
            if (toolbar.parent != binding.contentFrame) {
                (toolbar.parent as ViewGroup?)?.removeView(toolbar)

                binding.contentFrame.addView(toolbar)
            }

            currentToggleListener?.showToolbar() ?: run {
                toolbar.translationY = 0f
            }

            toolbar.doOnLayout {
                webView.translationY = toolbar.height.toFloat()
                coordinate(toolbar, webView)
            }
        } else {
            if (toolbar.parent != toolbarRoot) {
                (toolbar.parent as ViewGroup?)?.removeView(toolbar)

                toolbarRoot.addView(toolbar, 0)
            }

            toolbar.translationY = 0f
            webView.translationY = 0f
        }
    }

    fun showBottomToolbar() {
        show()
    }

    private var isContentShown = true

    private fun show() {
        val startingScale = binding.contentFrame.scaleX
        val percent = (MAX_SCALE - startingScale) / SCALE_DIFFERENCE
        binding.contentFrame.startAnimation(createScaleAnimation(MAX_SCALE).apply {
            duration = (percent * ANIMATION_DURATION).roundToLong()
            interpolator = AccelerateDecelerateInterpolator()
            setAnimationListener(object : AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    binding.contentFrame.isVisible = true
                    isContentShown = true
                }

                override fun onAnimationEnd(animation: Animation?) {
                    binding.drawerList.isVisible = false
                }

                override fun onAnimationRepeat(animation: Animation?) = Unit
            })
        })

//        toolbar.animate()
//            .translationY(0f)
//            .setDuration((percent * ANIMATION_DURATION).roundToLong())
//            .setInterpolator(AccelerateDecelerateInterpolator())
//            .start()
    }

    private fun createScaleAnimation(toScale: Float) = object : Animation() {
        val currentScale = binding.contentFrame.scaleX
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            binding.contentFrame.scaleX = currentScale + (toScale - currentScale) * interpolatedTime
            binding.contentFrame.scaleY = currentScale + (toScale - currentScale) * interpolatedTime
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun coordinateModernUi() {


        fun hide() {
            val startingScale = binding.contentFrame.scaleX
            val percent = (startingScale - MIN_SCALE) / SCALE_DIFFERENCE
            binding.contentFrame.startAnimation(createScaleAnimation(MIN_SCALE).apply {
                duration = (percent * ANIMATION_DURATION).roundToLong()
                interpolator = DecelerateInterpolator()
                setAnimationListener(object : AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {
                        binding.drawerList.isVisible = true
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        binding.contentFrame.isInvisible = true
                        isContentShown = false
                    }

                    override fun onAnimationRepeat(animation: Animation?) = Unit
                })
            })

//            toolbar.animate()
//                .translationY(toolbar.measuredHeight.toFloat())
//                .setDuration((percent * ANIMATION_DURATION).roundToLong())
//                .setInterpolator(AccelerateDecelerateInterpolator())
//                .start()
        }



        binding.homeButton.setOnTouchListener(SwipeGestureDetector(
            activity,
            onScrollCompleted = {
                val currentScale = binding.contentFrame.scaleX
                if (currentScale > SCALE_TIPPING) {
                    show()
                } else {
                    hide()
                }
            },
            onScrollStarted = {
                binding.drawerList.isVisible = true
            },
            onFling = { velocity, _ ->
                if (velocity < 0) {
                    hide()
                } else {
                    show()
                }
            }
        ) {
            val toolbarHeight = toolbar.measuredHeight.toFloat()
            val totalHeight = binding.contentFrame.measuredHeight / 5
            val percent = it / totalHeight

            val currentScale = binding.contentFrame.scaleX
            val newScale =
                minOf(maxOf(currentScale - percent * SCALE_DIFFERENCE, MIN_SCALE), MAX_SCALE)
            binding.contentFrame.scaleX = newScale
            binding.contentFrame.scaleY = newScale
            val currentTranslation = toolbar.translationY
//            toolbar.translationY =
//                minOf(toolbarHeight, maxOf(0f, currentTranslation + percent * toolbarHeight))
        })
        binding.homeButton.setOnClickListener {
            // presenter.onTabCountViewClick()
            if (isContentShown) {
                hide()
            } else {
                show()
            }
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
                    hideAnimation.duration = ANIMATION_DURATION
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
        private const val ANIMATION_DURATION = 250L
        private const val MAX_SCALE = 1.0f
        private const val MIN_SCALE = 0.6f
        private const val SCALE_TIPPING = 0.8f
        private const val SCALE_DIFFERENCE = MAX_SCALE - MIN_SCALE
    }
}
