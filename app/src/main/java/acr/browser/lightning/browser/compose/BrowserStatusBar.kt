package acr.browser.lightning.browser.compose

import acr.browser.lightning.browser.BrowserComposeState
import acr.browser.lightning.browser.BrowserViewState
import acr.browser.lightning.compose.StatusBar
import acr.browser.lightning.concurrency.StateProvider
import androidx.compose.runtime.Composable

@Composable
fun BrowserStatusBar(
    browserComposeState: BrowserComposeState,
    blackStatusStateProvider: StateProvider<Boolean>,
) {
    StatusBar(
        paintSurfaceColor = browserComposeState.toolbarVisibility != BrowserViewState.ToolbarVisibility.FIXED,
        blackStatusStateProvider = blackStatusStateProvider,
    )
}
