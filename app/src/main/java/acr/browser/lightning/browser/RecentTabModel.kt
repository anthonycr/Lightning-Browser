package acr.browser.lightning.browser

import android.os.Bundle
import java.util.*

/**
 * A model that saves [Bundle] and returns the last returned one.
 */
class RecentTabModel {

    private val bundleQueue: Queue<Bundle> = ArrayDeque<Bundle>()

    /**
     * Return the last closed tab as a [Bundle] or null if there is no previously opened tab.
     * Removes the [Bundle] from the queue after returning it.
     */
    fun lastClosed(): Bundle? = bundleQueue.poll()

    /**
     * Add the [savedBundle] to the queue. The next call to [lastClosed] will return this [Bundle].
     */
    fun addClosedTab(savedBundle: Bundle) = bundleQueue.add(savedBundle)

}
