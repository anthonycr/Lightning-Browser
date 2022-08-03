package acr.browser.lightning.browser.tab.bundle

import acr.browser.lightning.browser.tab.TabModel
import acr.browser.lightning.view.TabInitializer

/**
 * Created by anthonycr on 7/27/22.
 */
interface BundleStore {

    fun save(tabs: List<TabModel>)

    fun retrieve(): List<TabInitializer>

    /**
     * Synchronously delete all stored tabs.
     */
    fun deleteAll()
}
