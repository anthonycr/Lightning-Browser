package acr.browser.lightning.adblock.source

import acr.browser.lightning.preference.IntEnum

/**
 * The available hosts source options.
 */
enum class HostsSourcePreference(override val value: Int) : IntEnum {
    DEFAULT(0),
    LOCAL(1),
    REMOTE(2)
}
