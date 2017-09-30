package acr.browser.lightning.search.engine

import acr.browser.lightning.R

/**
 * The Yandex search engine.
 *
 * See http://upload.wikimedia.org/wikipedia/commons/thumb/9/91/Yandex.svg/600px-Yandex.svg.png
 * for the icon.
 */
class YandexSearch : BaseSearchEngine(
        "file:///android_asset/yandex.png",
        "https://yandex.ru/yandsearch?lr=21411&text=",
        R.string.search_engine_yandex
)
