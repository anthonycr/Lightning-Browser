package acr.browser.lightning.browser

import acr.browser.lightning.browser.tab.TabViewState
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.HistoryEntry
import acr.browser.lightning.database.downloads.DownloadEntry
import acr.browser.lightning.ssl.SslCertificateInfo
import acr.browser.lightning.ssl.showSslDialog
import android.content.Intent
import android.view.View
import acr.browser.lightning.browser.view.targetUrl.LongPress

/**
 * An adapter between [BrowserContract.View] and the [BrowserActivity] that creates partial states
 * to render in the activity.
 */
class BrowserStateAdapter(private val browserActivity: BrowserActivity) : BrowserContract.View {

    private var currentState: BrowserViewState? = null
    private var currentTabs: List<TabViewState>? = null

    override fun renderState(viewState: BrowserViewState) {
        val (
            displayUrl,
            sslState,
            isRefresh,
            progress,
            enableFullMenu,
            themeColor,
            isForwardEnabled,
            isBackEnabled,
            bookmarks,
            isBookmarked,
            isBookmarkEnabled,
            isRootFolder,
            findInPage
        ) = viewState

        browserActivity.renderState(
            PartialBrowserViewState(
                displayUrl = displayUrl.takeIf { it != currentState?.displayUrl },
                sslState = sslState.takeIf { it != currentState?.sslState },
                isRefresh = isRefresh.takeIf { it != currentState?.isRefresh },
                progress = progress.takeIf { it != currentState?.progress },
                enableFullMenu = enableFullMenu.takeIf { it != currentState?.enableFullMenu },
                themeColor = themeColor.takeIf { it != currentState?.themeColor },
                isForwardEnabled = isForwardEnabled.takeIf { it != currentState?.isForwardEnabled },
                isBackEnabled = isBackEnabled.takeIf { it != currentState?.isBackEnabled },
                bookmarks = bookmarks.takeIf { it != currentState?.bookmarks },
                isBookmarked = isBookmarked.takeIf { it != currentState?.isBookmarked },
                isBookmarkEnabled = isBookmarkEnabled.takeIf { it != currentState?.isBookmarkEnabled },
                isRootFolder = isRootFolder.takeIf { it != currentState?.isRootFolder },
                findInPage = findInPage.takeIf { it != currentState?.findInPage }
            )
        )

        currentState = viewState
    }

    override fun renderTabs(tabs: List<TabViewState>) {
        tabs.takeIf { it != currentTabs }?.let(browserActivity::renderTabs)
    }

    override fun showAddBookmarkDialog(title: String, url: String, folders: List<String>) {
        browserActivity.showAddBookmarkDialog(title, url, folders)
    }

    override fun showBookmarkOptionsDialog(bookmark: Bookmark.Entry) {
        browserActivity.showBookmarkOptionsDialog(bookmark)
    }

    override fun showEditBookmarkDialog(
        title: String,
        url: String,
        folder: String,
        folders: List<String>
    ) {
        browserActivity.showEditBookmarkDialog(title, url, folder, folders)
    }

    override fun showFolderOptionsDialog(folder: Bookmark.Folder) {
        browserActivity.showFolderOptionsDialog(folder)
    }

    override fun showEditFolderDialog(title: String) {
        browserActivity.showEditFolderDialog(title)
    }

    override fun showDownloadOptionsDialog(download: DownloadEntry) {
        browserActivity.showDownloadOptionsDialog(download)
    }

    override fun showHistoryOptionsDialog(historyEntry: HistoryEntry) {
        browserActivity.showHistoryOptionsDialog(historyEntry)
    }

    override fun showFindInPageDialog() {
        browserActivity.showFindInPageDialog()
    }

    override fun showLinkLongPressDialog(longPress: LongPress) {
        browserActivity.showLinkLongPressDialog(longPress)
    }

    override fun showImageLongPressDialog(longPress: LongPress) {
        browserActivity.showImageLongPressDialog(longPress)
    }

    override fun showSslDialog(sslCertificateInfo: SslCertificateInfo) {
        browserActivity.showSslDialog(sslCertificateInfo)
    }

    override fun showCloseBrowserDialog(id: Int) {
        browserActivity.showCloseBrowserDialog(id)
    }

    override fun openBookmarkDrawer() {
        browserActivity.openBookmarkDrawer()
    }

    override fun closeBookmarkDrawer() {
        browserActivity.closeBookmarkDrawer()
    }

    override fun openTabDrawer() {
        browserActivity.openTabDrawer()
    }

    override fun closeTabDrawer() {
        browserActivity.closeTabDrawer()
    }

    override fun showToolbar() {
        browserActivity.showToolbar()
    }

    override fun showToolsDialog(areAdsAllowed: Boolean, shouldShowAdBlockOption: Boolean) {
        browserActivity.showToolsDialog(areAdsAllowed, shouldShowAdBlockOption)
    }

    override fun showLocalFileBlockedDialog() {
        browserActivity.showLocalFileBlockedDialog()
    }

    override fun showFileChooser(intent: Intent) {
        browserActivity.showFileChooser(intent)
    }

    override fun showCustomView(view: View) {
        browserActivity.showCustomView(view)
    }

    override fun hideCustomView() {
        browserActivity.hideCustomView()
    }

    override fun clearSearchFocus() {
        browserActivity.clearSearchFocus()
    }
}
