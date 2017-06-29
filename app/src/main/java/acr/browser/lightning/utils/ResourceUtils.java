package acr.browser.lightning.utils;

import android.content.Context;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;

/**
 * Utils related to resources.
 */
public final class ResourceUtils {

    private ResourceUtils() {}

    /**
     * Returns the dimension in pixels.
     *
     * @param context the context needed to get the dimension.
     * @param res     the resource to get.
     * @return the dimension value in pixels.
     */
    public static int dimen(@NonNull Context context, @DimenRes int res) {
        return context.getResources().getDimensionPixelSize(res);
    }
}
