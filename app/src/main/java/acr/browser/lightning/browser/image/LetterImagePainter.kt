package acr.browser.lightning.browser.image

import acr.browser.lightning.R
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface

/**
 * Paints a letter using a [Canvas] onto a rounded square of a specific color.
 *
 * @param textSize The size of the text in pixels.
 * @param radius The radius of the rounded square in pixels.
 * @param character The character to draw.
 * @param size The size of the square in pixels.
 * @param color The color of the square, as an ARGB value.
 */
class LetterImagePainter(
    private val textSize: Float,
    private val radius: Float,
    private val character: Char,
    private val size: Int,
    private val color: Int,
) {

    private val paint = Paint().apply {
        color = this@LetterImagePainter.color
        val boldText = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        typeface = boldText
        textSize = this@LetterImagePainter.textSize
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    /**
     * Draws the letter onto the [canvas].
     */
    fun drawOn(canvas: Canvas) {
        val outer = RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawRoundRect(outer, radius, radius, paint)

        val xPos = (size / 2)
        val yPos = ((size / 2) - ((paint.descent() + paint.ascent()) / 2)).toInt()

        paint.color = Color.WHITE
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        canvas.drawText(character.toString(), xPos.toFloat(), yPos.toFloat(), paint)
    }

    /**
     * The possible colors of the letter.
     *
     * @param resource The resource of the color.
     */
    enum class LetterColor(val resource: Int) {
        BLUE(R.color.bookmark_default_blue),
        GREEN(R.color.bookmark_default_green),
        RED(R.color.bookmark_default_red),
        ORANGE(R.color.bookmark_default_orange)
    }

    companion object {
        /**
         * The "algorithm" used to determine which color an icon should be.
         *
         * @param character The character for which the color will be determined.
         */
        fun colorForCharacter(character: Char): LetterColor =
            when (character.lowercaseChar().code % 4) {
                0 -> LetterColor.BLUE
                1 -> LetterColor.GREEN
                2 -> LetterColor.RED
                3 -> LetterColor.ORANGE
                else -> error("Impossible result from modulus 4")
            }
    }
}
