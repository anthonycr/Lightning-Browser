package acr.browser.lightning.utils;

import android.annotation.TargetApi;
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

import acr.browser.lightning.R;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public final class ThemeUtils {

    private static final TypedValue sTypedValue = new TypedValue();

    private ThemeUtils() {}

    /**
     * Gets the primary color of the current theme.
     *
     * @param context the context to get the theme from.
     * @return the primary color of the current theme.
     */
    @ColorInt
    public static int getPrimaryColor(@NonNull Context context) {
        return getColor(context, R.attr.colorPrimary);
    }

    /**
     * Gets the primary dark color of the current theme.
     *
     * @param context the context to get the theme from.
     * @return the primary dark color of the current theme.
     */
    @ColorInt
    public static int getPrimaryColorDark(@NonNull Context context) {
        return getColor(context, R.attr.colorPrimaryDark);
    }

    /**
     * Gets the accent color of the current theme.
     *
     * @param context the context to get the theme from.
     * @return the accent color of the current theme.
     */
    @ColorInt
    public static int getAccentColor(@NonNull Context context) {
        return getColor(context, R.attr.colorAccent);
    }

    /**
     * Gets the color of the status bar as set in styles
     * for the current theme.
     *
     * @param context the context to get the theme from.
     * @return the status bar color of the current theme.
     */
    @ColorInt
    @TargetApi(21)
    public static int getStatusBarColor(@NonNull Context context) {
        return getColor(context, android.R.attr.statusBarColor);
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

    /**
     * Gets the icon color for the light theme.
     *
     * @param context the context to use.
     * @return the color of the icon.
     */
    @ColorInt
    private static int getIconLightThemeColor(@NonNull Context context) {
        return ContextCompat.getColor(context, R.color.icon_light_theme);
    }

    /**
     * Gets the icon color for the dark theme.
     *
     * @param context the context to use.
     * @return the color of the icon.
     */
    @ColorInt
    private static int getIconDarkThemeColor(@NonNull Context context) {
        return ContextCompat.getColor(context, R.color.icon_dark_theme);
    }

    /**
     * Gets the color icon for the light or
     * dark theme.
     *
     * @param context the context to use.
     * @param dark    true for the dark theme,
     *                false for the light theme.
     * @return the color of the icon.
     */
    @ColorInt
    public static int getIconThemeColor(@NonNull Context context, boolean dark) {
        return (dark) ? getIconDarkThemeColor(context) : getIconLightThemeColor(context);
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

    /**
     * Gets the icon with an applied color filter
     * for the correct theme.
     *
     * @param context the context to use.
     * @param res     the drawable resource to use.
     * @param dark    true for icon suitable for use with a dark theme,
     *                false for icon suitable for use with a light theme.
     * @return a themed icon.
     */
    @NonNull
    public static Bitmap createThemedBitmap(@NonNull Context context, @DrawableRes int res, boolean dark) {
        int color = dark ? getIconDarkThemeColor(context) : getIconLightThemeColor(context);

        return createThemedBitmap(context, res, color);
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

    /**
     * Gets the edit text text color for the current theme.
     *
     * @param context the context to use.
     * @return a text color.
     */
    @ColorInt
    public static int getTextColor(@NonNull Context context) {
        return getColor(context, android.R.attr.editTextColor);
    }
}
