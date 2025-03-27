package acr.browser.lightning.ids

import android.view.View
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Used to generate view IDs.
 */
@Singleton
class ViewIdGenerator @Inject constructor() {

    private val usedViewIds = mutableSetOf<Int>()

    val takenIds: Set<Int> = usedViewIds

    /**
     * Claim the [id] as taken so that it cannot be generated.
     */
    fun claimViewId(id: Int) {
        usedViewIds.add(id)
    }

    fun releaseViewId(id: Int) {
        usedViewIds.remove(id)
    }

    /**
     * Generate a unique view id.
     */
    fun generateViewId(): Int = generateSequence { View.generateViewId() }
        .first { !usedViewIds.contains(it) }
        .also(usedViewIds::add)
}
