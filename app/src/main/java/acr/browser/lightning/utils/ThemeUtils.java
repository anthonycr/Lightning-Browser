package acr.browser.lightning.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public final class ThemeUtils {

    private static final TypedValue sTypedValue = new TypedValue();

    private ThemeUtils() {}

    /**
     * Gets the accent color of the current theme.
     *
     * @param context the context to get the theme from.
     * @return the accent color of the current theme.
     */
    @ColorInt
    public static int getAccentColor(@NonNull Context context) {
        return getColor(context, androidx.appcompat.R.attr.colorAccent);
    }

    /**
     * Gets the color attribute from the current theme.
     *
     * @param context  the context to get the theme from.
     * @param resource the color attribute resource.
     * @return the color for the given attribute.
     */
    @ColorInt
    public static int getColor(@NonNull Context context, @AttrRes int resource) {
        TypedArray a = context.obtainStyledAttributes(sTypedValue.data, new int[]{resource});
        int color = a.getColor(0, 0);
        a.recycle();
        return color;
    }

    @NonNull
    private static Drawable getVectorDrawable(@NonNull Context context, int drawableId) {
        return ContextCompat.getDrawable(context, drawableId);
    }

    // http://stackoverflow.com/a/38244327/1499541
    @NonNull
    public static Bitmap getBitmapFromVectorDrawable(@NonNull Context context, int drawableId) {
        Drawable drawable = getVectorDrawable(context, drawableId);

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
            Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    @NonNull
    public static Bitmap createThemedBitmap(@NonNull Context context, @DrawableRes int res, @ColorInt int color) {
        Bitmap sourceBitmap = getBitmapFromVectorDrawable(context, res);
        Bitmap resultBitmap = Bitmap.createBitmap(sourceBitmap.getWidth(), sourceBitmap.getHeight(),
            Bitmap.Config.ARGB_8888);
        Paint p = new Paint();
        ColorFilter filter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN);
        p.setColorFilter(filter);
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(sourceBitmap, 0, 0, p);
        sourceBitmap.recycle();
        return resultBitmap;
    }

}
