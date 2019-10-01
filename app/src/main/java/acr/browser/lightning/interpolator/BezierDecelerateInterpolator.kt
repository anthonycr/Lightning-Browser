package acr.browser.lightning.interpolator

import android.view.animation.Interpolator
import androidx.core.view.animation.PathInterpolatorCompat

/**
 * Smooth bezier curve interpolator.
 */
class BezierDecelerateInterpolator : Interpolator {

    private val interpolator = PathInterpolatorCompat.create(0.25f, 0.1f, 0.25f, 1f)

    override fun getInterpolation(input: Float): Float = interpolator.getInterpolation(input)

}
