package acr.browser.lightning.browser

import acr.browser.lightning.browser.tab.TabViewState
import acr.browser.lightning.browser.view.targetUrl.LongPress
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.HistoryEntry
import acr.browser.lightning.database.downloads.DownloadEntry
import acr.browser.lightning.ssl.SslCertificateInfo
import acr.browser.lightning.ssl.SslState
import acr.browser.lightning.utils.Option

/**
 * The browser view state.
 *
 * @param displayUrl The current text shown in the search box.
 * @param searchQuery The current text shown in the expanded search box.
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
 * @param isIncognito True if we are in incognito mode, false otherwise.
 * @param tabs The state for each currently open tab.
 * @param tabCountText The text to display in the tab count icon.
 * @param dialog The currently open dialog if any.
 * @param openBookmarks True if the bookmark drawer should be open, false otherwise.
 * @param openTabs True if the tab drawer should be open, false otherwise.
 * @param showCustomView True if the custom view from the WebView should be shown, false otherwise.
 */
data class BrowserViewState(
    // search bar
    val displayUrl: String,
    val searchQuery: String,
    val searchQuerySelection: Pair<Int, Int>,
    val sslState: SslState,
    val isRefresh: Boolean,
    val progress: Int,
    val enableFullMenu: Boolean,
    val themeColor: Option<Int>,

    // Tabs
    val isForwardEnabled: Boolean,
    val isBackEnabled: Boolean,

    // Bookmarks
    val bookmarks: List<BookmarkListItem>,
    val isBookmarked: Boolean,
    val isBookmarkEnabled: Boolean,
    val isRootFolder: Boolean,

    // Find
    val findInPage: String?,

    val isIncognito: Boolean,

    // Tabs
    val tabs: List<TabViewState>,
    val tabCountText: String,

    val dialog: Dialogs? = null,

    // Drawers
    val openBookmarks: Boolean = false,
    val openTabs: Boolean = false,

    val showCustomView: Boolean = false,
) {
    sealed interface Dialogs {
        /**
         * Show the dialog to add a bookmark for the current page.
         *
         * @param title The current title of the page.
         * @param url The current URL of the page.
         * @param folders The available folders that the bookmark can be moved to.
         */
        data class AddBookmark(
            val title: String,
            val url: String,
            val folders: List<String>
        ) : Dialogs

        data class BookmarkOptions(val bookmarkOptionsDialog: Bookmark.Entry) : Dialogs

        /**
         * Show the dialog to edit a bookmark.
         *
         * @param title The current title of the bookmark.
         * @param url The current URL of the bookmark.
         * @param folder The current folder the bookmark is in.
         * @param folders The available folders that the bookmark can be moved to.
         */
        data class EditBookmark(
            val title: String,
            val url: String,
            val folder: String,
            val folders: List<String>
        ) : Dialogs

        data class FolderOptions(val folderOptionsDialog: Bookmark.Folder) : Dialogs
        data class EditFolder(val title: String) : Dialogs
        data class DownloadOptions(val downloadOptionsDialog: DownloadEntry) : Dialogs
        data class HistoryOptions(val historyOptionsDialog: HistoryEntry) : Dialogs
        data object FindInPage : Dialogs
        data class LinkLongPress(val linkLongPressDialog: LongPress) : Dialogs
        data class ImageLongPress(val imageLongPressDialog: LongPress) : Dialogs
        data class SslInfo(val sslDialog: SslCertificateInfo) : Dialogs
        data class CloseBrowser(val selectedTab: Int) : Dialogs

        /**
         * Show the tools dialog that allows the user to toggle ad blocking and user agent for the
         * current page.
         *
         * @param areAdsAllowed True if ads are currently allowed on the page, false otherwise.
         * @param shouldShowAdBlockOption True if ad block toggling is available for the current
         * page.
         */
        data class PageTools(
            val areAdsAllowed: Boolean,
            val shouldShowAdBlockOption: Boolean
        ) : Dialogs

        data object LocalFileBlocked : Dialogs
    }

    data class BookmarkListItem(
        val title: String,
        val icon: Icon
    ) {
        sealed interface Icon {
            data object Folder : Icon
            data class Image(val path: String?) : Icon
        }
    }
}
