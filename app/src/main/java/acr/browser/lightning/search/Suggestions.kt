package acr.browser.lightning.search

import acr.browser.lightning.preference.IntEnum

/**
 * The suggestion choices.
 */
enum class Suggestions(override val value: Int) : IntEnum {
    NONE(0),
    GOOGLE(1),
    DUCK(2),
    BAIDU(3),
    NAVER(4);
}
