package acr.browser.lightning.browser.compose.sheets

import acr.browser.lightning.R
import acr.browser.lightning.browser.BrowserComposeState
import acr.browser.lightning.browser.BrowserContract
import acr.browser.lightning.browser.BrowserPresenter
import acr.browser.lightning.browser.view.targetUrl.LongPress
import acr.browser.lightning.constant.HTTP
import acr.browser.lightning.dialog.DialogItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun LongPressImageLinkSheet(
    browserViewState: BrowserComposeState,
    longPress: LongPress,
    presenter: BrowserPresenter,
    onClick: (BrowserContract.ImageLongPressEvent) -> Unit
) {
    ListItemSheet(
        title = longPress.targetUrl?.replace(HTTP, "").orEmpty(),
        items = listOf(
            DialogItem(title = R.string.dialog_open_new_tab) {
                onClick(BrowserContract.ImageLongPressEvent.NEW_TAB)
            },
            DialogItem(title = R.string.dialog_open_background_tab) {
                onClick(BrowserContract.ImageLongPressEvent.BACKGROUND_TAB)
            },
            DialogItem(
                title = R.string.dialog_open_incognito_tab,
                isConditionMet = !browserViewState.isIncognito
            ) {
                onClick(BrowserContract.ImageLongPressEvent.INCOGNITO_TAB)
            },
            DialogItem(title = R.string.action_share) {
                onClick(BrowserContract.ImageLongPressEvent.SHARE)
            },
            DialogItem(title = R.string.dialog_copy_link) {
                onClick(BrowserContract.ImageLongPressEvent.COPY_LINK)
            },
            DialogItem(title = R.string.dialog_download_image) {
                onClick(BrowserContract.ImageLongPressEvent.DOWNLOAD)
            }
        ),
        presenter = presenter
    )
}

@Composable
fun LongPressLinkSheet(
    browserViewState: BrowserComposeState,
    longPress: LongPress,
    presenter: BrowserPresenter,
    onClick: (BrowserContract.LinkLongPressEvent) -> Unit
) {
    ListItemSheet(
        title = longPress.targetUrl?.replace(HTTP, "").orEmpty(),
        items = listOf(
            DialogItem(title = R.string.dialog_open_new_tab) {
                onClick(BrowserContract.LinkLongPressEvent.NEW_TAB)
            },
            DialogItem(title = R.string.dialog_open_background_tab) {
                onClick(BrowserContract.LinkLongPressEvent.BACKGROUND_TAB)
            },
            DialogItem(
                title = R.string.dialog_open_incognito_tab,
                isConditionMet = !browserViewState.isIncognito
            ) {
                onClick(BrowserContract.LinkLongPressEvent.INCOGNITO_TAB)
            },
            DialogItem(title = R.string.action_share) {
                onClick(BrowserContract.LinkLongPressEvent.SHARE)
            },
            DialogItem(title = R.string.dialog_copy_link) {
                onClick(BrowserContract.LinkLongPressEvent.COPY_LINK)
            }
        ),
        presenter = presenter
    )
}

@Composable
fun LongPressFolderLinkSheet(
    presenter: BrowserPresenter,
    onClick: (BrowserContract.FolderOptionEvent) -> Unit
) {
    ListItemSheet(
        title = stringResource(R.string.action_folder),
        items = listOf(
            DialogItem(title = R.string.dialog_rename_folder) {
                onClick(BrowserContract.FolderOptionEvent.RENAME)
            },
            DialogItem(title = R.string.dialog_remove_folder) {
                onClick(BrowserContract.FolderOptionEvent.REMOVE)
            }
        ),
        presenter = presenter
    )
}

@Composable
fun LongPressBookmarkLinkSheet(
    browserViewState: BrowserComposeState,
    presenter: BrowserPresenter,
    onClick: (BrowserContract.BookmarkOptionEvent) -> Unit
) {
    ListItemSheet(
        title = stringResource(R.string.action_bookmarks),
        items = listOf(
            DialogItem(title = R.string.dialog_open_new_tab) {
                onClick(BrowserContract.BookmarkOptionEvent.NEW_TAB)
            },
            DialogItem(title = R.string.dialog_open_background_tab) {
                onClick(BrowserContract.BookmarkOptionEvent.BACKGROUND_TAB)
            },
            DialogItem(
                title = R.string.dialog_open_incognito_tab,
                isConditionMet = !browserViewState.isIncognito
            ) {
                onClick(BrowserContract.BookmarkOptionEvent.INCOGNITO_TAB)
            },
            DialogItem(title = R.string.action_share) {
                onClick(BrowserContract.BookmarkOptionEvent.SHARE)
            },
            DialogItem(title = R.string.dialog_copy_link) {
                onClick(BrowserContract.BookmarkOptionEvent.COPY_LINK)
            },
            DialogItem(title = R.string.dialog_remove_bookmark) {
                onClick(BrowserContract.BookmarkOptionEvent.REMOVE)
            },
            DialogItem(title = R.string.dialog_edit_bookmark) {
                onClick(BrowserContract.BookmarkOptionEvent.EDIT)
            }
        ),
        presenter = presenter
    )
}

@Composable
fun LongPressHistoryLinkSheet(
    browserViewState: BrowserComposeState,
    presenter: BrowserPresenter,
    onClick: (BrowserContract.HistoryOptionEvent) -> Unit
) {
    ListItemSheet(
        title = stringResource(R.string.action_history),
        items = listOf(
            DialogItem(title = R.string.dialog_open_new_tab) {
                onClick(BrowserContract.HistoryOptionEvent.NEW_TAB)
            },
            DialogItem(title = R.string.dialog_open_background_tab) {
                onClick(BrowserContract.HistoryOptionEvent.BACKGROUND_TAB)
            },
            DialogItem(
                title = R.string.dialog_open_incognito_tab,
                isConditionMet = !browserViewState.isIncognito
            ) {
                onClick(BrowserContract.HistoryOptionEvent.INCOGNITO_TAB)
            },
            DialogItem(title = R.string.action_share) {
                onClick(BrowserContract.HistoryOptionEvent.SHARE)
            },
            DialogItem(title = R.string.dialog_copy_link) {
                onClick(BrowserContract.HistoryOptionEvent.COPY_LINK)
            },
            DialogItem(title = R.string.dialog_remove_from_history) {
                onClick(BrowserContract.HistoryOptionEvent.REMOVE)
            }
        ),
        presenter = presenter
    )
}
