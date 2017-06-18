package acr.browser.lightning.search.engine;

import android.support.annotation.NonNull;

/**
 * A custom search engine.
 */
public class CustomSearch extends BaseSearchEngine {

    public CustomSearch(@NonNull String queryUrl) {
        super("file:///android_asset/lightning.png", queryUrl);
    }

}
