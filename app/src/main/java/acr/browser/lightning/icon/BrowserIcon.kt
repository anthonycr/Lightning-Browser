package acr.browser.lightning.icon

import acr.browser.lightning.device.BuildInfo
import acr.browser.lightning.preference.IntEnum

/**
 * The available app icons.
 */
enum class BrowserIcon(override val value: Int) : IntEnum {
    ORANGE(0),
    BLUE(1),
}

/**
 * The default icon for the current build.
 */
fun BuildInfo.defaultIcon(): BrowserIcon = if (isPlus) {
    BrowserIcon.ORANGE
} else {
    BrowserIcon.BLUE
}
