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

    companion object {
        fun from(value: Int): Suggestions {
            return when (value) {
                0 -> NONE
                1 -> GOOGLE
                2 -> DUCK
                3 -> BAIDU
                4 -> NAVER
                else -> GOOGLE
            }
        }
    }
}
