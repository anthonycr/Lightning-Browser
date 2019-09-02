package acr.browser.lightning.browser

import acr.browser.lightning.preference.IntEnum

/**
 * An enum representing what detail level should be displayed in the search box.
 */
enum class SearchBoxDisplayChoice(override val value: Int) : IntEnum {
    DOMAIN(0),
    URL(1),
    TITLE(2)
}
