package acr.browser.lightning.adblock

import javax.inject.Inject
import javax.inject.Singleton

/**
 * A no-op ad blocker implementation. Always returns false for [isAd].
 */
@Singleton
class NoOpAdBlocker @Inject internal constructor() : AdBlocker {

    override fun isAd(url: String?) = false

}