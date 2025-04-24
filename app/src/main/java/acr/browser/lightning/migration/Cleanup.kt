package acr.browser.lightning.migration

import javax.inject.Inject

/**
 * Handle cleanup that should run on upgrade.
 */
class Cleanup @Inject constructor(
    private val actions: List<@JvmSuppressWildcards Action>
) {

    suspend fun cleanup() {
        actions.forEach { it.execute() }
    }

    interface Action {
        val versionCode: Int
        suspend fun execute()
    }
}
