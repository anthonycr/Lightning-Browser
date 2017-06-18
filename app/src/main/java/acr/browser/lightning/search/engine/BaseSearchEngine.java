package acr.browser.lightning.search.engine;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import acr.browser.lightning.utils.Preconditions;

/**
 * A class representative of a search engine.
 * <p>
 * Contains three key pieces of information:
 * <ul>
 * <li>The icon shown for the search engine, should point to a local assets URL.</li>
 * <li>The query URL for the search engine, the query will be appended to the end.</li>
 * <li>The title string resource for the search engine.</li>
 * </ul>
 */
public class BaseSearchEngine {

    @NonNull private final String mIconUrl;
    @NonNull private final String mQueryUrl;
    @StringRes private final int mTitleRes;

    public BaseSearchEngine(@NonNull String iconUrl,
                            @NonNull String queryUrl,
                            @StringRes int titleRes) {

        Preconditions.checkNonNull(iconUrl);
        Preconditions.checkNonNull(queryUrl);

        mIconUrl = iconUrl;
        mQueryUrl = queryUrl;
        mTitleRes = titleRes;
    }

    @NonNull
    public final String getIconUrl() {
        return mIconUrl;
    }

    @NonNull
    public final String getQueryUrl() {
        return mQueryUrl;
    }

    @StringRes
    public final int getTitleRes() {
        return mTitleRes;
    }

}
