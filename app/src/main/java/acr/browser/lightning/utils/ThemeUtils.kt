package acr.browser.lightning.utils;

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue

import acr.browser.lightning.R;
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat


object ThemeUtils {

    private val sTypedValue = TypedValue()

    /**
     * Gets the primary color of the current theme.
     *
     * @param context the context to get the theme from.
     * @return the primary color of the current theme.
     */
    @ColorInt
    fun getPrimaryColor(context: Context): Int {
        return getColor(context, R.attr.colorPrimary)
    }

    /**
     * Gets the primary dark color of the current theme.
     *
     * @param context the context to get the theme from.
     * @return the primary dark color of the current theme.
     */
    @ColorInt
    fun getPrimaryColorDark(context: Context): Int {
        return getColor(context, R.attr.colorPrimaryDark)
    }

    /**
     * Gets the accent color of the current theme.
     *
     * @param context the context to get the theme from.
     * @return the accent color of the current theme.
     */
    @ColorInt
    fun getAccentColor(context: Context): Int {
        return getColor(context, R.attr.colorAccent)
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
    fun getStatusBarColor(context: Context): Int {
        return getColor(context, android.R.attr.statusBarColor)
    }

    /**
     * Gets the color attribute from the current theme.
     *
     * @param context  the context to get the theme from.
     * @param resource the color attribute resource.
     * @return the color for the given attribute.
     */
    @ColorInt
    fun getColor(context: Context, @AttrRes resource: Int): Int {
        val a = context.obtainStyledAttributes(sTypedValue.data, intArrayOf(resource))
        val color = a.getColor(0, 0)
        a.recycle()
        return color
    }

    /**
     * Gets the icon color for the light theme.
     *
     * @param context the context to use.
     * @return the color of the icon.
     */
    @ColorInt
    private fun getIconLightThemeColor(context: Context): Int {
        return ContextCompat.getColor(context, R.color.icon_light_theme)
    }

    /**
     * Gets the icon color for the dark theme.
     *
     * @param context the context to use.
     * @return the color of the icon.
     */
    @ColorInt
    private fun getIconDarkThemeColor(context: Context): Int {
        return ContextCompat.getColor(context, R.color.icon_dark_theme)
    }

    /**
     * Gets the color icon for the light or
     * dark theme.
     *
     * @param context the context to use.
     * @param dark    true for the dark theme,
     * false for the light theme.
     * @return the color of the icon.
     */
    @ColorInt
    fun getIconThemeColor(context: Context, dark: Boolean): Int {
        return if (dark) getIconDarkThemeColor(context) else getIconLightThemeColor(context)
    }

    private fun getVectorDrawable(context: Context, drawableId: Int): Drawable {
        var drawable = ContextCompat.getDrawable(context, drawableId)

        // if it is null we throw a runtime exception
        Preconditions.checkNonNull(drawable)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = DrawableCompat.wrap(drawable!!).mutate()
        }

        return drawable!!
    }

    // http://stackoverflow.com/a/38244327/1499541
    fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap {
        val drawable = getVectorDrawable(context, drawableId)

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth, drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    /**
     * Gets the icon with an applied color filter
     * for the correct theme.
     *
     * @param context the context to use.
     * @param res     the drawable resource to use.
     * @param dark    true for icon suitable for use with a dark theme,
     * false for icon suitable for use with a light theme.
     * @return a themed icon.
     */
    fun createThemedBitmap(context: Context, @DrawableRes res: Int, dark: Boolean): Bitmap {
        val color = if (dark) getIconDarkThemeColor(context) else getIconLightThemeColor(context)

        val sourceBitmap = getBitmapFromVectorDrawable(context, res)
        val resultBitmap = Bitmap.createBitmap(
            sourceBitmap.width, sourceBitmap.height,
            Bitmap.Config.ARGB_8888
        )

        val p = Paint()
        val filter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        p.colorFilter = filter

        val canvas = Canvas(resultBitmap)
        canvas.drawBitmap(sourceBitmap, 0f, 0f, p)
        sourceBitmap.recycle()

        return resultBitmap
    }

    /**
     * Gets the edit text text color for the current theme.
     *
     * @param context the context to use.
     * @return a text color.
     */
    @ColorInt
    fun getTextColor(context: Context): Int {
        return getColor(context, android.R.attr.editTextColor)
    }

}
