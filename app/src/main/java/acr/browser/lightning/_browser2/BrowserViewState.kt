package acr.browser.lightning._browser2

import acr.browser.lightning._browser2.tab.Tab
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.ssl.SslState

/**
 * Created by anthonycr on 9/11/20.
 */
data class BrowserViewState(
    // search bar
    val displayUrl: String,
    val sslState: SslState,
    val isRefresh: Boolean,
    val progress: Int,

    // Tabs
    val tabs: List<Tab>,
    val isForwardEnabled: Boolean,
    val isBackEnabled: Boolean,

    // Bookmarks
    val bookmarks: List<Bookmark>,
    val isBookmarked: Boolean
)
