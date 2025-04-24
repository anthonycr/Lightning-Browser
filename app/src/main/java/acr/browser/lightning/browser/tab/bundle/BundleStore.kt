package acr.browser.lightning.browser.tab.bundle

import acr.browser.lightning.browser.tab.TabModel
import acr.browser.lightning.browser.tab.TabInitializer

/**
 * Used to save tab data for future restoration when the browser goes into hibernation.
 */
interface BundleStore {

    /**
     * Save the tab data for the list of [tabs].
     */
    fun save(tabs: List<TabModel>)

    /**
     * Synchronously previously stored tab data.
     */
    fun retrieve(): List<TabInitializer>

    /**
     * Synchronously delete all stored tabs.
     */
    fun deleteAll()
}
