package acr.browser.lightning.browser.tab.bundle

import acr.browser.lightning.tab.TabModel
import acr.browser.lightning.tab.initializer.TabInitializer
import android.webkit.WebView

/**
 * A bundle store implementation that no-ops for incognito mode.
 */
object IncognitoBundleStore : BundleStore<WebView> {
    override suspend fun save(tabs: List<TabModel<WebView>>) = Unit

    override suspend fun retrieve(): List<TabInitializer<WebView>> = emptyList()

    override suspend fun deleteAll() = Unit
}
