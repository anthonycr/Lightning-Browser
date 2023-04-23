package acr.browser.lightning.view

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import kotlin.math.hypot

/**
 * A layout that clips the contents based on an aspect ratio and the scale of the view.
 */
class ClippingFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val path = Path()

    private var centerX = 0f
    private var centerY = 0f

    private var maxRect = RectF()
    private var minRect = RectF()


    private val rect = RectF()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        centerX = measuredWidth / 2f
        centerY = measuredHeight / 2f

        val ratio = when (context.resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> 1f
            Configuration.ORIENTATION_PORTRAIT -> measuredWidth.toFloat() / measuredHeight
            else -> error("Unsupported orientation")
        }

        val maxHeight = measuredHeight.toFloat()
        val maxWidth = measuredWidth.toFloat()

        maxRect.apply {
            left = 0f
            top = 0f
            right = maxWidth
            bottom = maxHeight
        }

        val minHeight = measuredHeight.toFloat()
        val minWidth = minHeight * ratio

        minRect.apply {
            left = centerX - minWidth / 2
            top = centerY - minHeight / 2
            right = centerX + minWidth / 2
            bottom = centerY + minHeight / 2
        }
    }

    override fun setScaleX(scaleX: Float) {
        super.setScaleX(scaleX)
        invalidate()
    }

    private fun interpolate(max: Float, min: Float): Float {
        return max - ((max - min) * (-2.5f * scaleX + 2.5f))
    }

    override fun draw(canvas: Canvas) {
        val count = canvas.saveCount
        if (scaleX < 1.0f) {

            rect.apply {
                left = interpolate(maxRect.left, minRect.left)
                top = interpolate(maxRect.top, minRect.top)
                right = interpolate(maxRect.right, minRect.right)
                bottom = interpolate(maxRect.bottom, minRect.bottom)
            }

            path.rewind()
            path.addRect(rect, Path.Direction.CW)
            canvas.clipPath(path)
        }

        super.draw(canvas)
        canvas.restoreToCount(count)
    }
}
