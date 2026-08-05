package acr.browser.lightning.browser.compose

import acr.browser.lightning.browser.BrowserComposeState
import acr.browser.lightning.browser.BrowserViewState
import acr.browser.lightning.compose.StatusBar
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

@Composable
fun BrowserStatusBar(
    browserComposeState: BrowserComposeState,
    useBlackStatusBarStateFlow: StateFlow<Boolean?>,
) {
    StatusBar(
        paintSurfaceColor = browserComposeState.toolbarVisibility != BrowserViewState.ToolbarVisibility.FIXED,
        useBlackStatusBarStateFlow = useBlackStatusBarStateFlow,
    )
}
