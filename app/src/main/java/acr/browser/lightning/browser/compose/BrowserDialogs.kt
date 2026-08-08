package acr.browser.lightning.browser.compose

import acr.browser.lightning.BrowserUiEvent
import acr.browser.lightning.browser.BrowserComposeState
import acr.browser.lightning.browser.BrowserPresenter
import acr.browser.lightning.browser.BrowserViewState
import acr.browser.lightning.browser.compose.sheets.BookmarkAddOrEditSheet
import acr.browser.lightning.browser.compose.sheets.BookmarkFolderRenameSheet
import acr.browser.lightning.browser.compose.sheets.CloseBrowserSheet
import acr.browser.lightning.browser.compose.sheets.DownloadOptionsSheet
import acr.browser.lightning.browser.compose.sheets.LocalFileBlockedSheet
import acr.browser.lightning.browser.compose.sheets.LongPressBookmarkLinkSheet
import acr.browser.lightning.browser.compose.sheets.LongPressFolderLinkSheet
import acr.browser.lightning.browser.compose.sheets.LongPressHistoryLinkSheet
import acr.browser.lightning.browser.compose.sheets.LongPressImageLinkSheet
import acr.browser.lightning.browser.compose.sheets.LongPressLinkSheet
import acr.browser.lightning.browser.compose.sheets.PageToolsSheet
import acr.browser.lightning.browser.compose.sheets.SslInfoSheet
import androidx.compose.runtime.Composable

@Composable
fun BrowserDialogs(
    browserViewState: BrowserComposeState,
    browserPresenter: BrowserPresenter,
) {
    when (val dialog = browserViewState.dialog) {
        is BrowserViewState.Dialogs.AddBookmark -> BookmarkAddOrEditSheet(
            edit = false,
            title = dialog.title,
            url = dialog.url,
            folder = "",
            folders = dialog.folders,
            presenter = browserPresenter,
            onConfirmed = { title, url, folder ->
                browserPresenter.onEvent(BrowserUiEvent.BookmarkConfirmed(title, url, folder))
            }
        )

        is BrowserViewState.Dialogs.BookmarkOptions -> LongPressBookmarkLinkSheet(
            browserViewState = browserViewState,
            presenter = browserPresenter,
            onClick = {
                browserPresenter.onEvent(
                    BrowserUiEvent.BookmarkOptionClick(dialog.bookmarkOptionsDialog, it)
                )
            }
        )

        is BrowserViewState.Dialogs.CloseBrowser -> CloseBrowserSheet(
            presenter = browserPresenter,
            onClick = {
                browserPresenter.onEvent(
                    BrowserUiEvent.CloseBrowser(dialog.selectedTab, it)
                )
            }
        )

        is BrowserViewState.Dialogs.DownloadOptions -> DownloadOptionsSheet(
            presenter = browserPresenter,
            onClick = {
                browserPresenter.onEvent(
                    BrowserUiEvent.DownloadOptionClick(dialog.downloadOptionsDialog, it)
                )
            }
        )

        is BrowserViewState.Dialogs.EditBookmark -> BookmarkAddOrEditSheet(
            edit = true,
            title = dialog.title,
            url = dialog.url,
            folder = dialog.folder,
            folders = dialog.folders,
            presenter = browserPresenter,
            onConfirmed = { title, url, folder ->
                browserPresenter.onEvent(BrowserUiEvent.BookmarkEditConfirmed(title, url, folder))
            }
        )

        is BrowserViewState.Dialogs.EditFolder -> BookmarkFolderRenameSheet(
            oldTitle = dialog.title,
            presenter = browserPresenter,
            onSelected = {
                browserPresenter.onEvent(
                    BrowserUiEvent.BookmarkFolderRenameConfirmed(dialog.title, it.toString())
                )
            }
        )

        is BrowserViewState.Dialogs.FolderOptions -> LongPressFolderLinkSheet(
            presenter = browserPresenter,
            onClick = {
                browserPresenter.onEvent(
                    BrowserUiEvent.FolderOptionClick(dialog.folderOptionsDialog, it)
                )
            }
        )

        is BrowserViewState.Dialogs.HistoryOptions -> LongPressHistoryLinkSheet(
            browserViewState = browserViewState,
            presenter = browserPresenter,
            onClick = {
                browserPresenter.onEvent(
                    BrowserUiEvent.HistoryOptionClick(dialog.historyOptionsDialog, it)
                )
            }
        )

        is BrowserViewState.Dialogs.ImageLongPress -> LongPressImageLinkSheet(
            browserViewState = browserViewState,
            longPress = dialog.imageLongPressDialog,
            presenter = browserPresenter,
            onClick = {
                browserPresenter.onEvent(
                    BrowserUiEvent.ImageLongPress(dialog.imageLongPressDialog, it)
                )
            }
        )

        is BrowserViewState.Dialogs.LinkLongPress -> LongPressLinkSheet(
            browserViewState = browserViewState,
            longPress = dialog.linkLongPressDialog,
            presenter = browserPresenter,
            onClick = {
                browserPresenter.onEvent(
                    BrowserUiEvent.LinkLongPress(dialog.linkLongPressDialog, it)
                )
            }
        )

        BrowserViewState.Dialogs.LocalFileBlocked -> LocalFileBlockedSheet {
            browserPresenter.onEvent(BrowserUiEvent.ConfirmOpenLocalFile(it))
        }

        is BrowserViewState.Dialogs.PageTools -> PageToolsSheet(
            areAdsAllowed = dialog.areAdsAllowed,
            shouldShowAdBlockOption = dialog.shouldShowAdBlockOption,
            presenter = browserPresenter,
        )

        is BrowserViewState.Dialogs.SslInfo -> SslInfoSheet(dialog.sslDialog, browserPresenter)
        null -> Unit // No dialog
    }
}
