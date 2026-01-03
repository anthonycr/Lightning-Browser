package acr.browser.lightning.browser.tab.bundle

import acr.browser.lightning.browser.tab.TabModel
import acr.browser.lightning.browser.tab.TabInitializer

/**
 * A bundle store implementation that no-ops for for incognito mode.
 */
object IncognitoBundleStore : BundleStore {
    override fun save(tabs: List<TabModel>, selectedTabId: Int) = Unit

    override fun retrieve(): Pair<List<TabInitializer>, Int> = emptyList<TabInitializer>() to -1

    override fun deleteAll() = Unit
}
