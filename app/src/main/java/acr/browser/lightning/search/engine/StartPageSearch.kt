package acr.browser.lightning.search.engine

import acr.browser.lightning.R
import acr.browser.lightning.constant.STARTPAGE_SEARCH

/**
 * The StartPage search engine.
 */
class StartPageSearch : BaseSearchEngine(
        "file:///android_asset/startpage.png",
        STARTPAGE_SEARCH,
        R.string.search_engine_startpage
)
