package acr.browser.lightning.search.engine

import acr.browser.lightning.R
import acr.browser.lightning.constant.YANDEX_SEARCH

/**
 * The Yandex search engine.
 *
 * See http://upload.wikimedia.org/wikipedia/commons/thumb/9/91/Yandex.svg/600px-Yandex.svg.png
 * for the icon.
 */
class YandexSearch : BaseSearchEngine(
        "file:///android_asset/yandex.png",
        YANDEX_SEARCH,
        R.string.search_engine_yandex
)
