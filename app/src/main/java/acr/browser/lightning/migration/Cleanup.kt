package acr.browser.lightning.migration

import acr.browser.lightning.device.BuildInfo
import acr.browser.lightning.preference.CleanupPreferencesDataStore
import javax.inject.Inject

/**
 * Handle cleanup that should run on upgrade.
 *
 * Note: This requires that all application variants have the same version code scheme.
 */
class Cleanup @Inject constructor(
    private val actions: List<@JvmSuppressWildcards Action>,
    private val cleanupPreferencesDataStore: CleanupPreferencesDataStore,
    private val buildInfo: BuildInfo,
) {

    /**
     * Execute cleanups.
     */
    suspend fun cleanup() {
        val lastInstalledVersion = cleanupPreferencesDataStore.lastInstalledVersion.get()

        actions.filter {
            lastInstalledVersion == null || lastInstalledVersion < it.fixedInVersionCode
        }.forEach { it.execute() }

        cleanupPreferencesDataStore.lastInstalledVersion.set(buildInfo.versionCode)
    }

    /**
     * A cleanup action to be taken.
     */
    interface Action {

        /**
         * Sets the version code in which this action was introduced, and which when upgrading from,
         * this action does not need to run.
         */
        val fixedInVersionCode: Int

        /**
         * Execute the cleanup.
         */
        suspend fun execute()
    }
}
