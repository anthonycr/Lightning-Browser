package acr.browser.lightning._browser2.tab.bundle

import acr.browser.lightning._browser2.tab.TabModel
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
