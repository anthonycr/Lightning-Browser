package acr.browser.lightning.browser.tab.settings

import acr.browser.lightning.preference.IntEnum

/**
 * The text sizes supported by the browser.
 */
enum class TextSize(override val value: Int) : IntEnum {
    X_SMALL(5),
    SMALL(4),
    MEDIUM(3),
    LARGE(2),
    X_LARGE(1),
    XX_LARGE(0)
}
