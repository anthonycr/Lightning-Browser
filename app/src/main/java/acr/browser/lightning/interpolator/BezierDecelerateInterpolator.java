package acr.browser.lightning.interpolator;

import android.support.annotation.NonNull;
import android.support.v4.view.animation.PathInterpolatorCompat;
import android.view.animation.Interpolator;

/**
 * Bezier decelerate curve similar to iOS.
 * On Kitkat and below, it reverts to a
 * decelerate interpolator.
 */
public class BezierDecelerateInterpolator implements Interpolator {

    @NonNull
    private static final Interpolator PATH_INTERPOLATOR;

    static {
        PATH_INTERPOLATOR = PathInterpolatorCompat.create(0.25f, 0.1f, 0.25f, 1);
    }

    @Override
    public float getInterpolation(float input) {
        return PATH_INTERPOLATOR.getInterpolation(input);
    }
}
