package acr.browser.lightning.ids

import acr.browser.lightning.di.BrowserScope
import android.view.View
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * Used to generate view IDs.
 */
@BrowserScope
class ViewIdGenerator @Inject constructor() {

    private val mutex = Mutex()
    private val usedViewIds = mutableSetOf<Int>()

    val takenIds: Set<Int> = usedViewIds

    /**
     * Claim the [ids] as taken so that they cannot be generated.
     */
    suspend fun claimViewIds(ids: List<Int>) = mutex.withLock {
        ids.forEach { id ->
            require(!usedViewIds.contains(id)) { "Id [$id] already claimed!" }
            usedViewIds.add(id)
        }
    }

    suspend fun releaseViewId(id: Int) = mutex.withLock {
        usedViewIds.remove(id)
    }

    /**
     * Generate a unique view id.
     */
    suspend fun generateViewId(): Int = mutex.withLock {
        generateSequence { View.generateViewId() }
            .first { !usedViewIds.contains(it) }
            .also(usedViewIds::add)
    }
}
