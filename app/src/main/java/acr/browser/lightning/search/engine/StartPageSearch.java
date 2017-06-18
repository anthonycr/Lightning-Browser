package acr.browser.lightning.search.engine;

import acr.browser.lightning.constant.Constants;

/**
 * The StartPage search engine.
 */
public class StartPageSearch extends BaseSearchEngine {

    public StartPageSearch() {
        super("file:///android_asset/startpage.png", Constants.STARTPAGE_SEARCH);
    }

}
