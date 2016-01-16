package acr.browser.lightning.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import acr.browser.lightning.R;
import acr.browser.lightning.utils.ThemeUtils;
import acr.browser.lightning.utils.Utils;

/**
 * {@link LightningViewTitle} acts as a container class
 * for the favicon and page title, the information used
 * by the tab adapters to show the tabs to the user.
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

    /**
     * Set the current favicon to a new Bitmap.
     * May be null, if null, the default will be used.
     *
     * @param favicon the potentially null favicon to set.
     */
    public void setFavicon(@Nullable Bitmap favicon) {
        if (favicon == null) {
            mFavicon = DEFAULT_ICON;
        } else {
            mFavicon = Utils.padFavicon(favicon);
        }
    }

    /**
     * Set the current title to a new title.
     * Must not be null.
     *
     * @param title the non-null title to set.
     */
    public void setTitle(@Nullable String title) {
        if (title == null) {
            mTitle = "";
        } else {
            mTitle = title;
        }
    }

    /**
     * Gets the current title, which is not null.
     * Can be an empty string.
     *
     * @return the non-null title.
     */
    @NonNull
    public String getTitle() {
        return mTitle;
    }

    /**
     * Gets the favicon of the page, which is not null.
     * Either the favicon, or a default icon.
     *
     * @return the favicon or a default if that is null.
     */
    @NonNull
    public Bitmap getFavicon() {
        return mFavicon;
    }

}
