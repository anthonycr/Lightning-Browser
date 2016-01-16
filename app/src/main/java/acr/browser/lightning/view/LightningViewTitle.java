package acr.browser.lightning.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import acr.browser.lightning.R;
import acr.browser.lightning.utils.ThemeUtils;
import acr.browser.lightning.utils.Utils;

/**
 * @author Stefano Pacifici base on Anthony C. Restaino's code
 * @date 2015/09/21
 */
class LightningViewTitle {

    private static Bitmap DEFAULT_ICON = null;

    @NonNull
    private Bitmap mFavicon;
    @NonNull
    private String mTitle;

    public LightningViewTitle(@NonNull Context context, boolean darkTheme) {
        if (DEFAULT_ICON == null) {
            DEFAULT_ICON = ThemeUtils.getThemedBitmap(context, R.drawable.ic_webpage, darkTheme);
        }
        mFavicon = DEFAULT_ICON;
        mTitle = context.getString(R.string.action_new_tab);
    }

    public void setFavicon(@Nullable Bitmap favicon) {
        if (favicon == null) {
            mFavicon = DEFAULT_ICON;
        } else {
            mFavicon = Utils.padFavicon(favicon);
        }
    }

    public void setTitle(@Nullable String title) {
        if (title == null) {
            mTitle = "";
        } else {
            mTitle = title;
        }
    }

    public void setTitleAndFavicon(@Nullable String title, @Nullable Bitmap favicon) {
        setTitle(title);
        setFavicon(favicon);
    }

    @NonNull
    public String getTitle() {
        return mTitle;
    }

    @NonNull
    public Bitmap getFavicon() {
        return mFavicon;
    }

    public static Bitmap getDefaultIcon() {
        return DEFAULT_ICON;
    }
}
