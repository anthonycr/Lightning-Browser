package acr.browser.lightning.search.engine

import acr.browser.lightning.R

/**
 * The Kagi search engine.
 *
 * See TODO: for the icon.
 */
class KagiSearch : BaseSearchEngine(
    "file:///android_asset/kagi.png",
    "https://kagi.com/search?&q=",
    R.string.search_engine_kagi
)
