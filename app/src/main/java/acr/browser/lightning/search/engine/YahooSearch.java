package acr.browser.lightning.search.engine;

import acr.browser.lightning.R;
import acr.browser.lightning.constant.Constants;

/**
 * The Yahoo search engine.
 * <p>
 * See http://upload.wikimedia.org/wikipedia/commons/thumb/2/24/Yahoo%21_logo.svg/799px-Yahoo%21_logo.svg.png
 * for the icon.
 */
public class YahooSearch extends BaseSearchEngine {

    public YahooSearch() {
        super("file:///android_asset/yahoo.png", Constants.YAHOO_SEARCH, R.string.search_engine_yahoo);
    }

}
