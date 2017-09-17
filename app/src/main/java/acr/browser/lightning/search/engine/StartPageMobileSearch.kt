package acr.browser.lightning.search.engine

import acr.browser.lightning.R
import acr.browser.lightning.constant.STARTPAGE_MOBILE_SEARCH

/**
 * The StartPage mobile search engine.
 */
class StartPageMobileSearch : BaseSearchEngine(
        "file:///android_asset/startpage.png",
        STARTPAGE_MOBILE_SEARCH,
        R.string.search_engine_startpage_mobile
)
