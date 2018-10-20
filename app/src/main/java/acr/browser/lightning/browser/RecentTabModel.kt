package acr.browser.lightning.browser

import android.os.Bundle
import java.util.*

/**
 * A model that saves [Bundle] and returns the last returned one.
 */
class RecentTabModel {

    private val bundleQueue: Queue<Bundle> = ArrayDeque<Bundle>()

    fun lastClosed(): Bundle? = bundleQueue.poll()

    fun addClosedTab(savedBundle: Bundle) = bundleQueue.add(savedBundle)

}
