package acr.browser.lightning.utils

import acr.browser.lightning.preference.DeveloperPreferences
import leakcanary.LeakCanary
import javax.inject.Inject

/**
 * Sets up LeakCanary.
 */
class LeakCanaryUtils @Inject constructor(private val developerPreferences: DeveloperPreferences) {

    /**
     * Setup LeakCanary
     */
    fun setup() {
        LeakCanary.config = LeakCanary.config.copy(
            dumpHeap = developerPreferences.useLeakCanary
        )
    }

}
