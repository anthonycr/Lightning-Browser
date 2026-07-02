package acr.browser.lightning.browser

import acr.browser.lightning.browser.download.PendingDownload
import acr.browser.lightning.browser.tab.TabInitializer
import acr.browser.lightning.browser.tab.TabModel
import acr.browser.lightning.database.downloads.DownloadEntry
import android.content.Intent
import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow

/**
 * The contract for the browser.
 */
interface BrowserContract {

    /**
     * The view that renders the browser state.
     */
    interface View {

        /**
         * Show the dialog to add a bookmark for the current page.
         *
         * @param title The current title of the page.
         * @param url The current URL of the page.
         * @param folders The available folders that the bookmark can be moved to.
         */
        fun showAddBookmarkDialog(title: String, url: String, folders: List<String>)

        /**
         * Show the dialog to edit a bookmark.
         *
         * @param title The current title of the bookmark.
         * @param url The current URL of the bookmark.
         * @param folder The current folder the bookmark is in.
         * @param folders The available folders that the bookmark can be moved to.
         */
        fun showEditBookmarkDialog(
            title: String,
            url: String,
            folder: String,
            folders: List<String>
        )

        /**
         * Show the edit folder dialog for the folder with the provided [title].
         */
        fun showEditFolderDialog(title: String)

        /**
         * Show the options dialog for the provided [download].
         */
        fun showDownloadOptionsDialog(download: DownloadEntry)

        /**
         * Show the toolbar/search box if it has been hidden due to scrolling.
         */
        fun showToolbar()

        /**
         * Show the tools dialog that allows the user to toggle ad blocking and user agent for the
         * current page.
         *
         * @param areAdsAllowed True if ads are currently allowed on the page, false otherwise.
         * @param shouldShowAdBlockOption True if ad block toggling is available for the current
         * page.
         */
        fun showToolsDialog(areAdsAllowed: Boolean, shouldShowAdBlockOption: Boolean)

        /**
         * Show a warning to the user that they are about to open a local file in the browser that
         * could be potentially dangerous.
         */
        fun showLocalFileBlockedDialog()

        /**
         * Show the file chooser with the provided [intent].
         */
        fun showFileChooser(intent: Intent)
    }

    /**
     * The model used to manage tabs in the browser.
     */
    interface Model {

        /**
         * Delete the tab with the provided [id].
         */
        suspend fun deleteTab(id: Int)

        /**
         * Delete all open tabs.
         */
        suspend fun deleteAllTabs()

        /**
         * Create a tab that will be initialized with the [tabInitializer].
         */
        suspend fun createTab(
            tabInitializer: TabInitializer,
            tabType: TabModel.Type = TabModel.Type.NORMAL
        ): TabModel

        /**
         * Reopen the most recently closed tab if there is a closed tab to re-open.
         */
        suspend fun reopenTab(): TabModel?

        /**
         * The current selected tab, if there is one.
         */
        val selectedTab: TabModel?

        /**
         * Select the tab with the provide [id] as the currently viewed tab.
         */
        fun selectTab(id: Int): TabModel

        /**
         * Initialize all tabs that were previously frozen when the browser was last open, and
         * initialize any tabs that should be opened from the initial browser action.
         */
        suspend fun initializeTabs(): List<TabModel>

        /**
         * Mark all tabs as being permanent tabs so that they won't be deleted during navigation
         * events.
         */
        fun markAllNonEphemeral()

        /**
         * Notifies the model that all tabs need to be frozen before the browser shuts down.
         */
        suspend fun freeze()

        /**
         * Clean all permanent stored content.
         */
        suspend fun clean()

        /**
         * The current open tabs.
         */
        val tabsList: List<TabModel>

        /**
         * Changes to the current open tabs.
         */
        fun tabsListChanges(): Flow<List<TabModel>>

    }

    /**
     * Used by the browser to navigate between screens and perform other navigation events.
     */
    interface Navigator {

        /**
         * Open the browser settings screen.
         */
        fun openSettings()

        /**
         * Share the web page with the provided [url] and [title].
         */
        fun sharePage(url: String, title: String?)

        /**
         * Copy the page [url] to the clip board.
         */
        fun copyPageLink(url: String)

        /**
         * Close the browser and terminate the session.
         */
        suspend fun closeBrowser()

        /**
         * Add a shortcut to the home screen that opens the [url]. Use the provided [title] and
         * [favicon] to create the shortcut.
         */
        fun addToHomeScreen(url: String, title: String, favicon: Bitmap?)

        /**
         * Download the file provided by the [pendingDownload].
         */
        fun download(pendingDownload: PendingDownload)

        /**
         * Move the browser to the background without terminating the session.
         */
        fun backgroundBrowser()

        /**
         * launch the incognito browser and load the provided [url].
         */
        fun launchIncognito(url: String?)
    }

    /**
     * The options for the close tab menu dialog.
     */
    enum class CloseTabEvent {
        CLOSE_CURRENT,
        CLOSE_OTHERS,
        CLOSE_ALL
    }

    /**
     * The options for the bookmark menu dialog.
     */
    enum class BookmarkOptionEvent {
        NEW_TAB,
        BACKGROUND_TAB,
        INCOGNITO_TAB,
        SHARE,
        COPY_LINK,
        REMOVE,
        EDIT
    }

    /**
     * The options for the history menu dialog.
     */
    enum class HistoryOptionEvent {
        NEW_TAB,
        BACKGROUND_TAB,
        INCOGNITO_TAB,
        SHARE,
        COPY_LINK,
        REMOVE,
    }

    /**
     * The options for the download menu dialog.
     */
    enum class DownloadOptionEvent {
        DELETE,
        DELETE_ALL
    }

    /**
     * The options for the folder menu dialog.
     */
    enum class FolderOptionEvent {
        RENAME,
        REMOVE
    }

    /**
     * The options for the link long press menu dialog.
     */
    enum class LinkLongPressEvent {
        NEW_TAB,
        BACKGROUND_TAB,
        INCOGNITO_TAB,
        SHARE,
        COPY_LINK
    }

    /**
     * The options for the image long press menu dialog.
     */
    enum class ImageLongPressEvent {
        NEW_TAB,
        BACKGROUND_TAB,
        INCOGNITO_TAB,
        SHARE,
        COPY_LINK,
        DOWNLOAD
    }

    /**
     * Supported actions that can be passed to the browser.
     */
    sealed class Action {
        /**
         * The action to load the provided [url].
         */
        data class LoadUrl(val url: String) : Action()

        /**
         * The action to emergency clean the entire browser contents and stored data.
         */
        data object Panic : Action()
    }

}
