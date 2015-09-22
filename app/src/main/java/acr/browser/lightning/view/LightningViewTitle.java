package acr.browser.lightning.view;

import android.content.Context;
import android.graphics.Bitmap;

import acr.browser.lightning.R;
import acr.browser.lightning.utils.ThemeUtils;
import acr.browser.lightning.utils.Utils;

/**
 * @author Stefano Pacifici base on Anthony C. Restaino's code
 * @date 2015/09/21
 */
class LightningViewTitle {

    private static Bitmap DEFAULT_ICON = null;

    private Bitmap mFavicon;
    private String mTitle;

    public LightningViewTitle(Context context, boolean darkTheme) {
        if (DEFAULT_ICON == null) {
            DEFAULT_ICON = ThemeUtils.getThemedBitmap(context, R.drawable.ic_webpage, darkTheme);
        }
        mFavicon = DEFAULT_ICON;
        mTitle = context.getString(R.string.action_new_tab);
    }

    public void setFavicon(Bitmap favicon) {
        if (favicon == null) {
            mFavicon = DEFAULT_ICON;
        } else {
            mFavicon = Utils.padFavicon(favicon);
        }
    }

    public void setTitle(String title) {
        if (title == null) {
            mTitle = "";
        } else {
            mTitle = title;
        }
    }

    public void setTitleAndFavicon(String title, Bitmap favicon) {
        mTitle = title;

        if (favicon == null) {
            mFavicon = DEFAULT_ICON;
        } else {
            mFavicon = Utils.padFavicon(favicon);
        }
    }

    public String getTitle() {
        return mTitle;
    }

    public Bitmap getFavicon() {
        return mFavicon;
    }

    public Bitmap getDefaultIcon() {
        return DEFAULT_ICON;
    }
}
