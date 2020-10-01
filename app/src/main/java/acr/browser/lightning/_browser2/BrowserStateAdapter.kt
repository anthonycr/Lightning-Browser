package acr.browser.lightning._browser2

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
            isBookmarkEnabled
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
                isBookmarkEnabled = isBookmarkEnabled.takeIf { it != currentState?.isBookmarkEnabled }
            )
        )

        currentState = viewState
    }

    override fun showAddBookmarkDialog(title: String, url: String, folders: List<String>) {
        browserActivity.showAddBookmarkDialog(title, url, folders)
    }

    override fun showEditBookmarkDialog(title: String, url: String, folder: String, folders: List<String>) {
        browserActivity.showEditBookmarkDialog(title, url, folder, folders)
    }

    override fun showEditFolderDialog(title: String) {
        browserActivity.showEditFolderDialog(title)
    }

}
