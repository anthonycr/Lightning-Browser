package acr.browser.lightning.utils;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.widget.ImageView;

import acr.browser.lightning.R;

public class ThemeUtils {

    private static final TypedValue sTypedValue = new TypedValue();

    public static int getPrimaryColor(@NonNull Context context) {
        return getColor(context, R.attr.colorPrimary);
    }

    public static int getPrimaryColorDark(@NonNull Context context) {
        return getColor(context, R.attr.colorPrimaryDark);
    }

    public static int getAccentColor(@NonNull Context context) {
        return getColor(context, R.attr.colorAccent);
    }

    private static int getColor(@NonNull Context context, @AttrRes int resource) {
        TypedArray a = context.obtainStyledAttributes(sTypedValue.data, new int[]{resource});
        int color = a.getColor(0, 0);
        a.recycle();
        return color;
    }

    public static int getIconLightThemeColor(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getResources().getColor(R.color.icon_light_theme, context.getTheme());
        }
        return context.getResources().getColor(R.color.icon_light_theme);
    }

    public static int getIconDarkThemeColor(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getResources().getColor(R.color.icon_dark_theme, context.getTheme());
        }
        return context.getResources().getColor(R.color.icon_dark_theme);
    }

    public static void themeImageView(ImageView icon, Context context, boolean dark) {
        int color = dark ? getIconDarkThemeColor(context) : getIconLightThemeColor(context);
        icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    public static Bitmap getThemedBitmap(Context context, @DrawableRes int res, boolean dark) {
        int color = dark ? getIconDarkThemeColor(context) : getIconLightThemeColor(context);
        Bitmap sourceBitmap = BitmapFactory.decodeResource(context.getResources(), res);
        Bitmap resultBitmap = Bitmap.createBitmap(sourceBitmap.getWidth(), sourceBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Paint p = new Paint();
        ColorFilter filter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN);
        p.setColorFilter(filter);
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(sourceBitmap, 0, 0, p);
        sourceBitmap.recycle();
        return resultBitmap;
    }

    @Nullable
    public static Drawable getThemedDrawable(@NonNull Context context, @DrawableRes int res, boolean dark) {
        int color = dark ? getIconDarkThemeColor(context) : getIconLightThemeColor(context);
        final Drawable drawable;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = context.getResources().getDrawable(res);
        } else {
            drawable = context.getDrawable(res);
        }
        if (drawable == null)
            return null;
        drawable.mutate();
        drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        return drawable;
    }

    @Nullable
    public static Drawable getLightThemedDrawable(@NonNull Context context, @DrawableRes int res) {
        final Drawable drawable;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = context.getResources().getDrawable(res);
        } else {
            drawable = context.getDrawable(res);
        }
        if (drawable == null)
            return null;
        drawable.mutate();
        drawable.setColorFilter(getIconLightThemeColor(context), PorterDuff.Mode.SRC_IN);
        return drawable;
    }

    public static ColorDrawable getSelectedBackground(@NonNull Context context, boolean dark) {
        Resources res = context.getResources();
        int color;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            color = (dark) ? res.getColor(R.color.selected_dark, context.getTheme()) :
                    res.getColor(R.color.selected_light, context.getTheme());
        } else {
            color = (dark) ? res.getColor(R.color.selected_dark) :
                    res.getColor(R.color.selected_light);
        }
        return new ColorDrawable(color);
    }
}
