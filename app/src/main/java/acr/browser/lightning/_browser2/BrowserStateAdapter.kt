package acr.browser.lightning._browser2

import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.HistoryEntry
import acr.browser.lightning.database.downloads.DownloadEntry
import acr.browser.lightning.ssl.SslCertificateInfo
import acr.browser.lightning.ssl.showSslDialog
import targetUrl.LongPress

/**
 * Created by anthonycr on 9/16/20.
 */
class BrowserStateAdapter(private val browserActivity: BrowserActivity) : BrowserContract.View {

    private var currentState: BrowserViewState? = null

    override fun renderState(viewState: BrowserViewState) {
        val (
            displayUrl,
            sslState,
            isRefresh,
            progress,
            tabs,
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
                tabs = tabs.takeIf { it != currentState?.tabs },
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

    override fun showToolsDialog(areAdsAllowed: Boolean, shouldShowAdBlockOption: Boolean) {
        browserActivity.showToolsDialog(areAdsAllowed, shouldShowAdBlockOption)
    }

}
