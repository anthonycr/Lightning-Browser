package acr.browser.lightning.utils;

import android.content.Context;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;

public final class ResourceUtils {

    private ResourceUtils() {}

    public static int dimen(@NonNull Context context, @DimenRes int res) {
        return Math.round(context.getResources().getDimension(res));
    }
}
