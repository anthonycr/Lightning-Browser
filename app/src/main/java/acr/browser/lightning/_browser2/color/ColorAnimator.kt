package acr.browser.lightning._browser2.color

import acr.browser.lightning.utils.DrawableUtils
import acr.browser.lightning.utils.Utils
import android.graphics.Color
import android.view.animation.Animation
import android.view.animation.Transformation

/**
 * An animator that creates animations between two different colors.
 */
class ColorAnimator(private val defaultColor: Int) {

    private var currentColor: Int? = null

    private fun mixSearchBarColor(requestedColor: Int, defaultColor: Int): Int =
        if (requestedColor == defaultColor) {
            Color.WHITE
        } else {
            DrawableUtils.mixColor(0.25f, requestedColor, Color.WHITE)
        }

    /**
     * Creates an animation that animates from the current color to the new [color].
     */
    fun animateTo(
        color: Int,
        onChange: (mainColor: Int, secondaryColor: Int) -> Unit
    ): Animation {
        val currentUiColor = currentColor ?: defaultColor
        val finalUiColor = if (Utils.isColorGrayscale(color)) {
            defaultColor
        } else {
            color
        }

        val startSearchColor = mixSearchBarColor(currentUiColor, defaultColor)
        val finalSearchColor = mixSearchBarColor(finalUiColor, defaultColor)
        val animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                val mainColor =
                    DrawableUtils.mixColor(interpolatedTime, currentUiColor, finalUiColor)
                val secondaryColor =
                    DrawableUtils.mixColor(interpolatedTime, startSearchColor, finalSearchColor)
                onChange(mainColor, secondaryColor)
                currentColor = mainColor
            }
        }
        animation.duration = 300
        return animation
    }
}
