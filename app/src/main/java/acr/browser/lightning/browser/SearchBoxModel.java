package acr.browser.lightning.browser;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.utils.UrlUtils;
import acr.browser.lightning.utils.Utils;

/**
 * A UI model for the search box.
 */
public class SearchBoxModel {

    @Inject PreferenceManager mPreferences;
    @Inject Application mApplication;

    @NonNull private final String mUntitledTitle;

    @Inject
    public SearchBoxModel() {
        BrowserApp.getAppComponent().inject(this);
        mUntitledTitle = mApplication.getString(R.string.untitled);
    }

    /**
     * Returns the contents of the search box based on a variety of factors.
     * <li>
     * <ul>The user's preference to show either the URL, domain, or page title</ul>
     * <ul>Whether or not the current page is loading</ul>
     * <ul>Whether or not the current page is a Lightning generated page.</ul>
     * </li>
     * This method uses the URL, title, and loading information to determine what
     * should be displayed by the search box.
     *
     * @param url       the URL of the current page.
     * @param title     the title of the current page, if known.
     * @param isLoading whether the page is currently loading or not.
     * @return the string that should be displayed by the search box.
     */
    @NonNull
    public String getDisplayContent(@NonNull String url, @Nullable String title, boolean isLoading) {
        if (UrlUtils.isSpecialUrl(url)) {
            return "";
        } else if (isLoading) {
            return url;
        } else {
            switch (mPreferences.getUrlBoxContentChoice()) {
                default:
                case 0: // Default, show only the domain
                    String domain = Utils.getDomainName(url);
                    return domain != null ? domain : url;
                case 1: // URL, show the entire URL
                    return url;
                case 2: // Title, show the page's title
                    if (!TextUtils.isEmpty(title)) {
                        return title;
                    } else {
                        return mUntitledTitle;
                    }
            }
        }
    }

}
