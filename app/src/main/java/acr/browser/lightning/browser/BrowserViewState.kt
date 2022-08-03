package acr.browser.lightning.browser

import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.ssl.SslState
import acr.browser.lightning.utils.Option

/**
 * Created by anthonycr on 9/11/20.
 */
data class BrowserViewState(
    // search bar
    val displayUrl: String,
    val sslState: SslState,
    val isRefresh: Boolean,
    val progress: Int,
    val enableFullMenu: Boolean,
    val themeColor: Option<Int>,

    // Tabs
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
    val enableFullMenu: Boolean?,
    val themeColor: Option<Int>?,

    // Tabs
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
