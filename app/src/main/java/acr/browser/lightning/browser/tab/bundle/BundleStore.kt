package acr.browser.lightning.browser.tab.bundle

import acr.browser.lightning.tab.TabModel
import acr.browser.lightning.tab.initializer.TabInitializer

/**
 * Used to save tab data for future restoration when the browser goes into hibernation.
 *
 * @param T The type of tab this store supports saving and retrieving.
 */
interface BundleStore<T> {

    /**
     * Save the tab data for the list of [tabs].
     */
    suspend fun save(tabs: List<TabModel<T>>)

    /**
     * Synchronously previously stored tab data.
     */
    suspend fun retrieve(): List<TabInitializer<T>>

    /**
     * Synchronously delete all stored tabs.
     */
    suspend fun deleteAll()
}
