package acr.browser.lightning

import acr.browser.lightning.browser.BrowserViewState
import acr.browser.lightning.browser.tab.TabViewState
import acr.browser.lightning.browser.view.targetUrl.LongPress
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.HistoryEntry
import acr.browser.lightning.database.downloads.DownloadEntry
import acr.browser.lightning.ssl.SslCertificateInfo

data class BrowserScreenState(
    val isIncognito: Boolean,
    val browserViewState: BrowserViewState,
    val tabState: List<TabViewState>,
    val tabCountText: String,
    val dialog: Dialogs? = null,
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
