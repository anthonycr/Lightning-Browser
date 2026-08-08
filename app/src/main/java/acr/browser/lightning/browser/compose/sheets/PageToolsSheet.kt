package acr.browser.lightning.browser.compose.sheets

import acr.browser.lightning.BrowserUiEvent
import acr.browser.lightning.R
import acr.browser.lightning.browser.BrowserPresenter
import acr.browser.lightning.dialog.DialogItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageToolsSheet(
    areAdsAllowed: Boolean,
    shouldShowAdBlockOption: Boolean,
    presenter: BrowserPresenter,
) {
    ListItemSheet(
        title = stringResource(R.string.dialog_tools_title),
        items = listOf(
            DialogItem(
                icon = R.drawable.ic_action_desktop,
                title = R.string.dialog_toggle_desktop,
                isConditionMet = true,
                onClick = { presenter.onEvent(BrowserUiEvent.ToggleDesktopAgentClick) }
            ),
            DialogItem(
                icon = R.drawable.ic_block,
                colorTint = if (areAdsAllowed) {
                    R.color.error_red
                } else {
                    null
                },
                title = if (areAdsAllowed) {
                    R.string.dialog_adblock_enable_for_site
                } else {
                    R.string.dialog_adblock_disable_for_site
                },
                isConditionMet = shouldShowAdBlockOption,
                onClick = { presenter.onEvent(BrowserUiEvent.ToggleAdBlockingClick) }
            )
        ),
        presenter = presenter
    )
}
