package acr.browser.lightning.search.engine

import acr.browser.lightning.R
import acr.browser.lightning.constant.BAIDU_SEARCH

/**
 * The Baidu search engine.
 *
 * See http://www.baidu.com/img/bdlogo.gif for the icon.
 */
class BaiduSearch : BaseSearchEngine(
        "file:///android_asset/baidu.png",
        BAIDU_SEARCH,
        R.string.search_engine_baidu
)
