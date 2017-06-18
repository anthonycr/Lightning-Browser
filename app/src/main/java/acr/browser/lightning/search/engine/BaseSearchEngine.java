package acr.browser.lightning.search.engine;

import android.support.annotation.NonNull;

import acr.browser.lightning.utils.Preconditions;

/**
 * A class representative of a search engine.
 */
public class BaseSearchEngine {

    @NonNull private final String mIconUrl;
    @NonNull private final String mQueryUrl;

    public BaseSearchEngine(@NonNull String iconUrl, @NonNull String queryUrl) {
        Preconditions.checkNonNull(iconUrl);
        Preconditions.checkNonNull(queryUrl);

        mIconUrl = iconUrl;
        mQueryUrl = queryUrl;
    }

    @NonNull
    public final String getIconUrl() {
        return mIconUrl;
    }

    @NonNull
    public final String getQueryUrl() {
        return mQueryUrl;
    }

}
