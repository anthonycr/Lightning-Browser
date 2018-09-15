package acr.browser.lightning.search

/**
 * The suggestion choices.
 *
 * Created by anthonycr on 2/19/18.
 */
enum class Suggestions(val index: Int) {
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