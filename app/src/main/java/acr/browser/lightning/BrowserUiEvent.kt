package acr.browser.lightning

import acr.browser.lightning.browser.BrowserContract
import acr.browser.lightning.browser.keys.KeyCombo
import acr.browser.lightning.browser.menu.MenuSelection
import acr.browser.lightning.browser.view.LongPress
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.HistoryEntry
import acr.browser.lightning.database.WebPage
import acr.browser.lightning.database.downloads.DownloadEntry
import androidx.activity.result.ActivityResult

/**
 * Events triggered by the browser's UI.
 */
sealed interface BrowserUiEvent {

    /**
     * Call when the user clicks on the tab menu located in the tab drawer.
     */
    data object TabMenuClick : BrowserUiEvent

    /**
     * Call when a new action is triggered, such as the user opening a new URL in the browser.
     */
    data class NewAction(val action: BrowserContract.Action) : BrowserUiEvent

    /**
     * Call when the user confirms that they do or do not want to allow a local file to be opened
     * in the browser. This is a security gate to prevent malicious local files from being opened
     * in the browser without the user's knowledge.
     */
    data class ConfirmOpenLocalFile(val allow: Boolean) : BrowserUiEvent

    /**
     * Call when the user selects an option from the menu.
     */
    data class MenuClick(val menuSelection: MenuSelection) : BrowserUiEvent

    /**
     * Call when the user selects a combination of keys to perform a shortcut.
     */
    data class KeyComboClick(val keyCombo: KeyCombo) : BrowserUiEvent

    /**
     * Call when the user selects a tab to switch to at the provided [index].
     */
    data class TabClick(val index: Int) : BrowserUiEvent

    /**
     * Call when the user long presses on a tab at the provided [index].
     */
    data class TabLongClick(val index: Int) : BrowserUiEvent

    /**
     * Call when the user clicks on the close button for the tab at the provided [index]
     */
    data class TabClose(val index: Int) : BrowserUiEvent

    /**
     * Call when the scroll position changes for the tab list.
     */
    data object TabScroll : BrowserUiEvent

    /**
     * Call when the tab drawer is opened or closed.
     *
     * @param isOpen True if the drawer is now open, false if it is now closed.
     */
    data class TabDrawerMoved(val isOpen: Boolean) : BrowserUiEvent

    /**
     * Call when the bookmark drawer is opened or closed.
     *
     * @param isOpen True if the drawer is now open, false if it is now closed.
     */
    data class BookmarkDrawerMoved(val isOpen: Boolean) : BrowserUiEvent

    /**
     * Called when the user clicks on the device back button or swipes to go back. Differentiated
     * from [BackClick] which is called when the user presses the browser's back button.
     */
    data object NavigateBack : BrowserUiEvent

    /**
     * Called when the user presses the browser's back button.
     */
    data object BackClick : BrowserUiEvent

    /**
     * Called when the user presses the browser's forward button.
     */
    data object ForwardClick : BrowserUiEvent

    /**
     * Call when the user clicks on the home button.
     */
    data object HomeClick : BrowserUiEvent

    /**
     * Call when the user clicks on the open new tab button.
     */
    data object NewTabClick : BrowserUiEvent

    /**
     * Call when the user clicks on the refresh (or stop/delete) button that is located in the
     * search bar.
     */
    data object RefreshOrStopClick : BrowserUiEvent

    /**
     * Call when the search [query] is updated by the user so that we can remember it.
     */
    data class SearchQueryChanged(
        val query: String,
        val selectionStart: Int,
        val selectionEnd: Int
    ) : BrowserUiEvent

    /**
     * Call when the user submits a search [query] to the search bar. At this point the user has
     * provided intent to search and is no longer trying to manipulate the query.
     */
    data class SearchConfirmed(val query: String) : BrowserUiEvent

    /**
     * Call when the search bar is expanded or collapsed by the user.
     */
    data class SearchBarExpandedOrCollapsed(val expanded: Boolean) : BrowserUiEvent

    /**
     * Call when the user enters a [query] to look for in the current web page.
     */
    data class FindInPage(val query: String) : BrowserUiEvent

    /**
     * Call when the user selects to move to the next highlighted word in the web page.
     */
    data object FindInPageNext : BrowserUiEvent

    /**
     * Call when the user selects to move to the previous highlighted word in the web page.
     */
    data object FindInPagePrevious : BrowserUiEvent

    /**
     * Call when the user chooses to dismiss the find in page UI component.
     */
    data object FindInPageDismissed : BrowserUiEvent

    /**
     * Call when the user selects a search suggestion that was suggested by the search box.
     */
    data class SearchSuggestionClick(val webPage: WebPage) : BrowserUiEvent

    /**
     * Call when the user clicks the insert button on a search suggestion.
     */
    data class SearchSuggestionInsertClick(val webPage: WebPage) : BrowserUiEvent

    /**
     * Call when a dialog is dismissed.
     */
    data object DialogDismissed : BrowserUiEvent

    /**
     * Call when the user clicks on the SSL icon in the search box.
     */
    data object SslIconClick : BrowserUiEvent

    /**
     * Call when the user clicks on a bookmark from the bookmark list at the provided [index].
     */
    data class BookmarkClick(val index: Int) : BrowserUiEvent

    /**
     * Call when the user long presses on a bookmark in the bookmark list at the provided [index].
     */
    data class BookmarkLongClick(val index: Int) : BrowserUiEvent

    /**
     * Call when the user clicks on the page tools button.
     */
    data object ToolsClick : BrowserUiEvent

    /**
     * Call when the user chooses to toggle the desktop user agent on/off.
     */
    data object ToggleDesktopAgentClick : BrowserUiEvent

    /**
     * Call when the user chooses to toggle ad blocking on/off for the current web page.
     */
    data object ToggleAdBlockingClick : BrowserUiEvent

    /**
     * Call when the user clicks on the star icon to add a bookmark for the current page or remove
     * the existing one.
     */
    data object StarClick : BrowserUiEvent

    /**
     * Call when the user confirms the details for adding a bookmark.
     *
     * @param title The title of the bookmark.
     * @param url The URL of the bookmark.
     * @param folder The name of the folder the bookmark is in.
     */
    data class BookmarkConfirmed(
        val title: String,
        val url: String,
        val folder: String
    ) : BrowserUiEvent

    /**
     * Call when the user confirms the details when editing a bookmark.
     *
     * @param title The title of the bookmark.
     * @param url The URL of the bookmark.
     * @param folder The name of the folder the bookmark is in.
     */
    data class BookmarkEditConfirmed(
        val title: String,
        val url: String,
        val folder: String
    ) : BrowserUiEvent

    /**
     * Call when the user confirms a name change to an existing folder.
     *
     * @param oldTitle The previous title of the folder.
     * @param newTitle The new title of the folder.
     */
    data class BookmarkFolderRenameConfirmed(
        val oldTitle: String,
        val newTitle: String
    ) : BrowserUiEvent

    /**
     * Call when the user clicks on a menu [optionClick] for the provided [bookmark].
     */
    data class BookmarkOptionClick(
        val bookmark: Bookmark.Entry,
        val optionClick: BrowserContract.BookmarkOptionEvent
    ) : BrowserUiEvent

    /**
     * Call when the user clicks on a menu [optionClick] for the provided [folder].
     */
    data class FolderOptionClick(
        val folder: Bookmark.Folder,
        val optionClick: BrowserContract.FolderOptionEvent
    ) : BrowserUiEvent

    /**
     * Call when the user clicks on a menu [optionClick] for the provided [downloadEntry].
     */
    data class DownloadOptionClick(
        val downloadEntry: DownloadEntry,
        val optionClick: BrowserContract.DownloadOptionEvent
    ) : BrowserUiEvent

    /**
     * Call when the user clicks on a menu [option] for the provided [historyEntry].
     */
    data class HistoryOptionClick(
        val historyEntry: HistoryEntry,
        val option: BrowserContract.HistoryOptionEvent
    ) : BrowserUiEvent

    /**
     * Call when the user clicks on the tab count button (or home button in desktop mode, or
     * incognito icon in incognito mode).
     */
    data object TabCountClick : BrowserUiEvent

    /**
     * Call when the user clicks on the bookmark menu (star or back arrow) located in the bookmark
     * drawer.
     */
    data object BookmarkMenuClick : BrowserUiEvent

    /**
     * Call when the user selects an option from the close browser menu that can be invoked by long
     * pressing on individual tabs.
     */
    data class CloseBrowser(
        val id: Int,
        val closeTabEvent: BrowserContract.CloseTabEvent
    ) : BrowserUiEvent

    /**
     * Call when the user long presses anywhere on the web page with the provided tab [id].
     */
    data class PageLongPress(
        val id: Int,
        val longPress: LongPress
    ) : BrowserUiEvent

    /**
     * Call when the user long presses on a link within the web page and selects what they want to
     * do with that link.
     */
    data class LinkLongPress(
        val longPress: LongPress,
        val linkLongPressEvent: BrowserContract.LinkLongPressEvent
    ) : BrowserUiEvent

    /**
     * Call when the user long presses on an image within the web page and selects what they want to
     * do with that image.
     */
    data class ImageLongPress(
        val longPress: LongPress,
        val imageLongPressEvent: BrowserContract.ImageLongPressEvent
    ) : BrowserUiEvent

    /**
     * Call when the user has selected a file from the file chooser to upload.
     */
    data class FileChooserResult(val activityResult: ActivityResult) : BrowserUiEvent

    /**
     * Call when a snackbar has been dismissed.
     */
    data object SnackbarDismissed : BrowserUiEvent

    /**
     * Call when the user clicks the action on the snackbar if there is any.
     */
    data object SnackbarActionPerformed : BrowserUiEvent
}
