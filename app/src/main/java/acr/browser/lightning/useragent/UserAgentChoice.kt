package acr.browser.lightning.useragent

import acr.browser.lightning.preference.IntEnum

/**
 * Potential user-agent values.
 */
enum class UserAgentChoice(override val value: Int) : IntEnum {
    DEFAULT(1),
    DESKTOP(2),
    MOBILE(3),
    CUSTOM(4),
}
