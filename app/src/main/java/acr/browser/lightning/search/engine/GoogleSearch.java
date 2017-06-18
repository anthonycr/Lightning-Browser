package acr.browser.lightning.search.engine;

import acr.browser.lightning.constant.Constants;

/**
 * The Google search engine.
 * <p>
 * See https://www.google.com/images/srpr/logo11w.png for the icon.
 */
public class GoogleSearch extends BaseSearchEngine {

    public GoogleSearch() {
        super("file:///android_asset/google.png", Constants.GOOGLE_SEARCH);
    }
}
