package acr.browser.lightning.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.util.TypedValue;

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

}
