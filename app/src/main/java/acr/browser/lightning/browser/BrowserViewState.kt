package acr.browser.lightning.browser

import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.ssl.SslState
import acr.browser.lightning.utils.Option

/**
 * The browser view state.
 *
 * @param displayUrl The current text shown in the search box.
 * @param sslState The current SSL state shown in the search box.
 * @param isRefresh True if the refresh button shows a refresh icon, false if it shows an X.
 * @param progress The current page loading progress out of 100.
 * @param enableFullMenu True if the full options menu should be shown, false if the limited one
 * should be shown for a local web page.
 * @param themeColor The UI theme as determined from the current web page.
 * @param isForwardEnabled True if the go forward button should be enabled, false otherwise.
 * @param isBackEnabled True if the go back button should be enabled, false otherwise.
 * @param bookmarks The current list of bookmarks that is displayed.
 * @param isBookmarked True if the current page is bookmarked, false otherwise.
 * @param isBookmarkEnabled True if the user should be allowed to bookmark the current page, false
 * otherwise.
 * @param isRootFolder True if the current bookmark folder is the root folder, false if it is a
 * child folder.
 * @param findInPage The text that we are searching the page for.
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

/**
 * A partial copy of [BrowserViewState], where null indicates that the value is unchanged.
 */
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
