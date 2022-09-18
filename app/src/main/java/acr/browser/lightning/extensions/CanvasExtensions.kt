package acr.browser.lightning.extensions

import acr.browser.lightning.utils.Utils.mixTwoColors
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import kotlin.math.tan

/**
 * Draws the trapezoid background for the horizontal tabs on a canvas object using the specified
 * color.
 *
 * @param backgroundColor the color to use to draw the tab
 * @param withShadow true if the trapezoid should have a shadow, false otherwise.
 */
fun Canvas.drawTrapezoid(backgroundColor: Int, withShadow: Boolean) {

    val shadowColor = mixTwoColors(Color.BLACK, backgroundColor, 0.5f)

    val paint = Paint().apply {
        color = backgroundColor
        style = Paint.Style.FILL
        // isFilterBitmap = true
        isAntiAlias = true
        isDither = true
        if (withShadow) {
            shader = LinearGradient(
                0f, 0.9f * height, 0f, height.toFloat(),
                backgroundColor, shadowColor,
                Shader.TileMode.CLAMP
            )
        }
    }

    val radians = Math.PI / 3
    val base = (height / tan(radians)).toInt()

    val wallPath = Path().apply {
        reset()
        moveTo(0f, height.toFloat())
        lineTo(width.toFloat(), height.toFloat())
        lineTo((width - base).toFloat(), 0f)
        lineTo(base.toFloat(), 0f)
        close()
    }

    drawPath(wallPath, paint)
}
