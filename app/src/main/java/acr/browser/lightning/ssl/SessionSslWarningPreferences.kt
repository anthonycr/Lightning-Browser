package acr.browser.lightning.ssl

import acr.browser.lightning.utils.Utils
import javax.inject.Singleton

/**
 * An implementation of [SslWarningPreferences] which stores user preferences in memory and does not
 * persist them past an app restart.
 */
@Singleton
class SessionSslWarningPreferences : SslWarningPreferences {

    private val ignoredSslWarnings = hashMapOf<String, SslWarningPreferences.Behavior>()

    override fun recallBehaviorForDomain(url: String?): SslWarningPreferences.Behavior? {
        Utils.getDomainName(url)?.let {
            return ignoredSslWarnings[it]
        }
        return null
    }

    override fun rememberBehaviorForDomain(url: String, behavior: SslWarningPreferences.Behavior) {
        Utils.getDomainName(url)?.let {
            ignoredSslWarnings.put(it, behavior)
        }
    }
}