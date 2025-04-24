package acr.browser.lightning.utils

import javax.inject.Inject

/**
 * No-op version of the utility to setup LeakCanary.
 */
class LeakCanaryUtils @Inject constructor() {

    /**
     * Setup LeakCanary.
     */
    fun setup() = Unit

}
