package acr.browser.lightning.browser.tab.bundle

import acr.browser.lightning.browser.tab.TabInitializer
import acr.browser.lightning.browser.tab.TabModel

/**
 * A bundle store implementation that no-ops for for incognito mode.
 */
object IncognitoBundleStore : BundleStore {
    override suspend fun save(tabs: List<TabModel>) = Unit

    override suspend fun retrieve(): List<TabInitializer> = emptyList()

    override suspend fun deleteAll() = Unit
}
