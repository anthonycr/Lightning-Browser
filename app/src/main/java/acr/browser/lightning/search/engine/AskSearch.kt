package acr.browser.lightning.search.engine

import acr.browser.lightning.R
import acr.browser.lightning.constant.ASK_SEARCH

/**
 * The Ask search engine.
 */
class AskSearch : BaseSearchEngine(
        "file:///android_asset/ask.png",
        ASK_SEARCH,
        R.string.search_engine_ask
)
