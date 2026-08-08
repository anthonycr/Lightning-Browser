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

sealed interface BrowserUiEvent {

    data object TabMenuClick : BrowserUiEvent

    data class NewAction(val action: BrowserContract.Action) : BrowserUiEvent

    data class ConfirmOpenLocalFile(val allow: Boolean) : BrowserUiEvent

    data class MenuClick(val menuSelection: MenuSelection) : BrowserUiEvent

    data class KeyComboClick(val keyCombo: KeyCombo) : BrowserUiEvent

    data class TabClick(val index: Int) : BrowserUiEvent

    data class TabLongClick(val index: Int) : BrowserUiEvent

    data class TabClose(val index: Int) : BrowserUiEvent

    data object TabScroll : BrowserUiEvent

    data class TabDrawerMoved(val isOpen: Boolean) : BrowserUiEvent

    data class BookmarkDrawerMoved(val isOpen: Boolean) : BrowserUiEvent

    data object NavigateBack : BrowserUiEvent

    data object BackClick : BrowserUiEvent

    data object ForwardClick : BrowserUiEvent

    data object HomeClick : BrowserUiEvent

    data object NewTabClick : BrowserUiEvent

    data object RefreshOrStopClick : BrowserUiEvent

    data class SearchQueryChanged(
        val query: String,
        val selectionStart: Int,
        val selectionEnd: Int
    ) : BrowserUiEvent

    data class SearchConfirmed(val query: String) : BrowserUiEvent

    data class SearchBarExpandedOrCollapsed(val expanded: Boolean) : BrowserUiEvent

    data class FindInPage(val query: String) : BrowserUiEvent

    data object FindInPageNext : BrowserUiEvent

    data object FindInPagePrevious : BrowserUiEvent

    data object FindInPageDismissed : BrowserUiEvent

    data class SearchSuggestionClick(val webPage: WebPage) : BrowserUiEvent

    data class SearchSuggestionInsertClick(val webPage: WebPage) : BrowserUiEvent

    data object DialogDismissed : BrowserUiEvent

    data object SslIconClick : BrowserUiEvent

    data class BookmarkClick(val index: Int) : BrowserUiEvent

    data class BookmarkLongClick(val index: Int) : BrowserUiEvent

    data object ToolsClick : BrowserUiEvent

    data object ToggleDesktopAgentClick : BrowserUiEvent

    data object ToggleAdBlockingClick : BrowserUiEvent

    data object StarClick : BrowserUiEvent

    data class BookmarkConfirmed(
        val title: String,
        val url: String,
        val folder: String
    ) : BrowserUiEvent

    data class BookmarkEditConfirmed(
        val title: String,
        val url: String,
        val folder: String
    ) : BrowserUiEvent

    data class BookmarkFolderRenameConfirmed(
        val oldTitle: String,
        val newTitle: String
    ) : BrowserUiEvent

    data class BookmarkOptionClick(
        val bookmark: Bookmark.Entry,
        val optionClick: BrowserContract.BookmarkOptionEvent
    ) : BrowserUiEvent

    data class FolderOptionClick(
        val folder: Bookmark.Folder,
        val optionClick: BrowserContract.FolderOptionEvent
    ) : BrowserUiEvent

    data class DownloadOptionClick(
        val downloadEntry: DownloadEntry,
        val optionClick: BrowserContract.DownloadOptionEvent
    ) : BrowserUiEvent

    data class HistoryOptionClick(
        val historyEntry: HistoryEntry,
        val option: BrowserContract.HistoryOptionEvent
    ) : BrowserUiEvent

    data object TabCountClick : BrowserUiEvent

    data object BookmarkMenuClick : BrowserUiEvent

    data class CloseBrowser(
        val id: Int,
        val closeTabEvent: BrowserContract.CloseTabEvent
    ) : BrowserUiEvent

    data class PageLongPress(
        val id: Int,
        val longPress: LongPress
    ) : BrowserUiEvent

    data class LinkLongPress(
        val longPress: LongPress,
        val linkLongPressEvent: BrowserContract.LinkLongPressEvent
    ) : BrowserUiEvent

    data class ImageLongPress(
        val longPress: LongPress,
        val imageLongPressEvent: BrowserContract.ImageLongPressEvent
    ) : BrowserUiEvent

    data class FileChooserResult(val activityResult: ActivityResult) : BrowserUiEvent

    data object SnackbarDismissed : BrowserUiEvent

    data object SnackbarActionPerformed : BrowserUiEvent
}
