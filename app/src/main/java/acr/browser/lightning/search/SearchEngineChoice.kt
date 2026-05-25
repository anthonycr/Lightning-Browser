package acr.browser.lightning.search

import acr.browser.lightning.preference.IntEnum

enum class SearchEngineChoice(override val value: Int) : IntEnum {
    CUSTOM(0),
    GOOGLE(1),
    ASK(2),
    BING(3),
    YAHOO(4),
    START_PAGE(5),
    START_PAGE_MOBILE(6),
    DUCK(7),
    DUCK_LITE(8),
    BAIDU(9),
    YANDEX(10),
    NAVER(11),
}
