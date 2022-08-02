package acr.browser.lightning.browser

import acr.browser.lightning.extensions.popIfNotEmpty
import android.os.Bundle
import java.util.*
import javax.inject.Inject

/**
 * A model that saves [Bundle] and returns the last returned one.
 */
class RecentTabModel @Inject constructor() {

    private val bundleStack: Stack<Bundle> = Stack()

    /**
     * Return the last closed tab as a [Bundle] or null if there is no previously opened tab.
     * Removes the [Bundle] from the queue after returning it.
     */
    fun lastClosed(): Bundle? = bundleStack.popIfNotEmpty()

    /**
     * Add the [savedBundle] to the queue. The next call to [lastClosed] will return this [Bundle].
     */
    fun addClosedTab(savedBundle: Bundle) = bundleStack.add(savedBundle)

}
