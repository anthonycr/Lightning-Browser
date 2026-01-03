package acr.browser.lightning.browser.tab.bundle

import acr.browser.lightning.browser.tab.TabModel
import acr.browser.lightning.browser.tab.TabInitializer

/**
 * Used to save tab data for future restoration when the browser goes into hibernation.
 */
interface BundleStore {

    /**
     * Save the tab data for the list of [tabs] and the [selectedTabId].
     */
    fun save(tabs: List<TabModel>, selectedTabId: Int)

    /**
     * Synchronously previously stored tab data and return the tabs and the selected tab ID.
     */
    fun retrieve(): Pair<List<TabInitializer>, Int>

    /**
     * Synchronously delete all stored tabs.
     */
    fun deleteAll()
}
