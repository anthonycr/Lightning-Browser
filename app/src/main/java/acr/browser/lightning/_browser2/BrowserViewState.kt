package acr.browser.lightning._browser2

import acr.browser.lightning._browser2.tab.TabViewState
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
    val tabs: List<TabViewState>,
    val isForwardEnabled: Boolean,
    val isBackEnabled: Boolean,

    // Bookmarks
    val bookmarks: List<Bookmark>,
    val isBookmarked: Boolean,
    val isBookmarkEnabled: Boolean,
    val isRootFolder: Boolean,

    // find
    val findInPage: String
)

data class PartialBrowserViewState(
    // search bar
    val displayUrl: String?,
    val sslState: SslState?,
    val isRefresh: Boolean?,
    val progress: Int?,

    // Tabs
    val tabs: List<TabViewState>?,
    val isForwardEnabled: Boolean?,
    val isBackEnabled: Boolean?,

    // Bookmarks
    val bookmarks: List<Bookmark>?,
    val isBookmarked: Boolean?,
    val isBookmarkEnabled: Boolean?,
    val isRootFolder: Boolean?,

    // find
    val findInPage: String?
)
