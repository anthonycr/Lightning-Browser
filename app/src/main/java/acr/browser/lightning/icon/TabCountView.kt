package acr.browser.lightning.icon

import acr.browser.lightning.R
import acr.browser.lightning.extensions.preferredLocale
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import java.text.NumberFormat

/**
 * A view that draws a count enclosed by a border. Defaults to drawing zero, draws infinity if the
 * number is greater than 99.
 *
 * Attributes:
 * - [R.styleable.TabCountView_tabIconColor] - The color used to draw the number and border.
 * Defaults to black.
 * - [R.styleable.TabCountView_tabIconTextSize] - The count text size, defaults to 14.
 * - [R.styleable.TabCountView_tabIconBorderRadius] - The radius of the border's corners. Defaults
 * to 0.
 * - [R.styleable.TabCountView_tabIconBorderWidth] - The width of the border. Defaults to 0.
 */
class TabCountView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val numberFormat = NumberFormat.getInstance(context.preferredLocale)
    private val clearMode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    private val overMode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    private val paint: Paint = Paint().apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private var borderRadius: Float = 0F
    private var borderWidth: Float = 0F
    private val workingRect = RectF()

    private var count: Int = 0

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        context.withStyledAttributes(attrs, R.styleable.TabCountView) {
            paint.color = getColor(R.styleable.TabCountView_tabIconColor, Color.BLACK)
            paint.textSize = getDimension(R.styleable.TabCountView_tabIconTextSize, 14F)
            borderRadius = getDimension(R.styleable.TabCountView_tabIconBorderRadius, 0F)
            borderWidth = getDimension(R.styleable.TabCountView_tabIconBorderWidth, 0F)
        }
    }

    /**
     * Update the number count displayed by the view.
     */
    fun updateCount(count: Int) {
        this.count = count
        contentDescription = count.toString()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val text: String = if (count > MAX_DISPLAYABLE_NUMBER) {
            context.getString(R.string.infinity)
        } else {
            numberFormat.format(count)
        }

        paint.xfermode = overMode

        workingRect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(workingRect, borderRadius, borderRadius, paint)

        paint.xfermode = clearMode

        val innerRadius = borderRadius - 1
        workingRect.set(borderWidth, borderWidth, (width - borderWidth), (height - borderWidth))
        canvas.drawRoundRect(workingRect, innerRadius, innerRadius, paint)

        paint.xfermode = overMode

        val xPos = width / 2F
        val yPos = height / 2 - (paint.descent() + paint.ascent()) / 2

        canvas.drawText(text, xPos, yPos, paint)

        super.onDraw(canvas)
    }

    companion object {
        private const val MAX_DISPLAYABLE_NUMBER = 99
    }

}
