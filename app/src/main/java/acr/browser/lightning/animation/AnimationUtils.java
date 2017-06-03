package acr.browser.lightning.animation;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageView;

/**
 * Animation specific helper code.
 */
public class AnimationUtils {

    /**
     * Creates an animation that rotates an {@link ImageView}
     * around the Y axis by 180 degrees and changes the image
     * resource shown when the view is rotated 90 degrees to the user.
     *
     * @param imageView   the view to rotate.
     * @param drawableRes the drawable to set when the view
     *                    is rotated by 90 degrees.
     * @return an animation that will change the image shown by the view.
     */
    @NonNull
    public static Animation createRotationTransitionAnimation(@NonNull final ImageView imageView,
                                                              @DrawableRes final int drawableRes) {
        Animation animation = new Animation() {

            private boolean mSetFinalDrawable;

            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime < 0.5f) {
                    imageView.setRotationY(90 * interpolatedTime * 2f);
                } else {
                    if (!mSetFinalDrawable) {
                        mSetFinalDrawable = true;
                        imageView.setImageResource(drawableRes);
                    }
                    imageView.setRotationY((-90) + (90 * (interpolatedTime - 0.5f) * 2f));
                }
            }
        };

        animation.setDuration(300);
        animation.setInterpolator(new AccelerateDecelerateInterpolator());

        return animation;
    }

}
