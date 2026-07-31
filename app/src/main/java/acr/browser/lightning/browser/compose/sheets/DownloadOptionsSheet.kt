package acr.browser.lightning.browser.compose.sheets

import acr.browser.lightning.R
import acr.browser.lightning.browser.BrowserContract
import acr.browser.lightning.browser.BrowserPresenter
import acr.browser.lightning.dialog.DialogItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun DownloadOptionsSheet(
    presenter: BrowserPresenter,
    onClick: (BrowserContract.DownloadOptionEvent) -> Unit
) {
    ListItemSheet(
        title = stringResource(R.string.action_downloads),
        items = listOf(
            DialogItem(title = R.string.dialog_delete_all_downloads) {
                onClick(BrowserContract.DownloadOptionEvent.DELETE_ALL)
            },
            DialogItem(title = R.string.dialog_delete_download) {
                onClick(BrowserContract.DownloadOptionEvent.DELETE)
            },
        ),
        presenter = presenter
    )
}
