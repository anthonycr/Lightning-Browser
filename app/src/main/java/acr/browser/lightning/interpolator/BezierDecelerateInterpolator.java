package acr.browser.lightning.interpolator;

import android.os.Build;
import android.support.annotation.NonNull;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

/**
 * Bezier decelerate curve similar to iOS.
 * On Kitkat and below, it reverts to a
 * decelerate interpolator.
 */
public class BezierDecelerateInterpolator implements Interpolator {

    @NonNull
    private static final Interpolator PATH_INTERPOLATOR;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PATH_INTERPOLATOR = new PathInterpolator(0.25f, 0.1f, 0.25f, 1);
        } else {
            PATH_INTERPOLATOR = new DecelerateInterpolator();
        }
    }

    @Override
    public float getInterpolation(float input) {
        return PATH_INTERPOLATOR.getInterpolation(input);
    }
}
