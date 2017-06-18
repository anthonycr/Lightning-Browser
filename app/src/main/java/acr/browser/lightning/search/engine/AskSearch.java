package acr.browser.lightning.search.engine;

import acr.browser.lightning.R;
import acr.browser.lightning.constant.Constants;

/**
 * The Ask search engine.
 */
public class AskSearch extends BaseSearchEngine {

    public AskSearch() {
        super("file:///android_asset/ask.png", Constants.ASK_SEARCH, R.string.search_engine_ask);
    }

}
