package acr.browser.lightning.ssl

import acr.browser.lightning.favicon.toValidUri
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An implementation of [SslWarningPreferences] which stores user preferences in memory and does not
 * persist them past an app restart.
 */
@Singleton
class SessionSslWarningPreferences @Inject constructor() : SslWarningPreferences {

    private val ignoredSslWarnings = hashMapOf<String, SslWarningPreferences.Behavior>()

    override fun recallBehaviorForDomain(url: String?): SslWarningPreferences.Behavior? {
        return url?.toValidUri()?.host?.let { ignoredSslWarnings[it] }
    }

    override fun rememberBehaviorForDomain(url: String, behavior: SslWarningPreferences.Behavior) {
        url.toValidUri()?.host?.let { ignoredSslWarnings.put(it, behavior) }
    }
}
