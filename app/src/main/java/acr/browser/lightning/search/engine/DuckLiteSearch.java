package acr.browser.lightning.search.engine;

import acr.browser.lightning.constant.Constants;

/**
 * The DuckDuckGo Lite search engine.
 * <p>
 * See https://duckduckgo.com/assets/logo_homepage.normal.v101.png for the icon.
 */
public class DuckLiteSearch extends BaseSearchEngine {

    public DuckLiteSearch() {
        super("file:///android_asset/duckduckgo.png", Constants.DUCK_LITE_SEARCH);
    }

}
