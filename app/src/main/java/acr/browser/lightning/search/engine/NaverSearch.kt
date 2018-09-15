package acr.browser.lightning.search.engine

import acr.browser.lightning.R

/**
 * The Naver search engine.
 *
 * See https://en.m.wikipedia.org/wiki/File:Naver_Logotype.svg for the icon.
 */
class NaverSearch : BaseSearchEngine(
    "file:///android_asset/naver.png",
    "https://search.naver.com/search.naver?ie=utf8&query=",
    R.string.search_engine_naver
)
