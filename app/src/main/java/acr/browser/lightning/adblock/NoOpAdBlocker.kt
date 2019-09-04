package acr.browser.lightning.adblock

import dagger.Reusable
import javax.inject.Inject

/**
 * A no-op ad blocker implementation. Always returns false for [isAd].
 */
@Reusable
class NoOpAdBlocker @Inject constructor() : AdBlocker {

    override fun isAd(url: String) = false

}
