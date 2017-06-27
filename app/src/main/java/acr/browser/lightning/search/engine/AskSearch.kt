package acr.browser.lightning.search.engine

import acr.browser.lightning.R
import acr.browser.lightning.constant.Constants

/**
 * The Ask search engine.
 */
class AskSearch : BaseSearchEngine(
        "file:///android_asset/ask.png",
        Constants.ASK_SEARCH,
        R.string.search_engine_ask
)
