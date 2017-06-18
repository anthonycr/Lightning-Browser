package acr.browser.lightning.search.engine;

import acr.browser.lightning.R;
import acr.browser.lightning.constant.Constants;

/**
 * The StartPage mobile search engine.
 */
public class StartPageMobileSearch extends BaseSearchEngine {

    public StartPageMobileSearch() {
        super("file:///android_asset/startpage.png", Constants.STARTPAGE_MOBILE_SEARCH, R.string.search_engine_startpage_mobile);
    }

}
