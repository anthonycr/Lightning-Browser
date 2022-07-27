package acr.browser.lightning._browser2.tab.bundle

import acr.browser.lightning._browser2.tab.TabModel
import acr.browser.lightning.view.TabInitializer

/**
 * A bundle store implementation that no-ops for for incognito mode.
 */
object IncognitoBundleStore : BundleStore {
    override fun save(tabs: List<TabModel>) = Unit

    override fun retrieve(): List<TabInitializer> = emptyList()

    override fun deleteAll() = Unit
}
