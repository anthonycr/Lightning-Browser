package acr.browser.lightning.browser

import acr.browser.lightning.BrowserUiEvent
import acr.browser.lightning.browser.compose.BottomTabs
import acr.browser.lightning.browser.compose.CustomView
import acr.browser.lightning.browser.compose.DesktopTabs
import acr.browser.lightning.browser.compose.DrawerTabs
import acr.browser.lightning.browser.ui.TabConfiguration
import acr.browser.lightning.search.SuggestionsModel
import android.widget.FrameLayout
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.StateFlow

@Composable
fun BrowserScreen(
    tabConfigurationStateProvider: StateFlow<TabConfiguration?>,
    useBlackStatusBarStateFlow: StateFlow<Boolean?>,
    browserViewState: BrowserComposeState,
    presenter: BrowserPresenter,
    browserFrameLayout: FrameLayout,
    customFrameLayout: FrameLayout,
    suggestionsModel: SuggestionsModel,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    browserViewState.ephemeral?.let {
        LaunchedEffect(it.message) {
            when (snackbarHostState.showSnackbar(
                message = it.message,
                actionLabel = it.actionLabel,
                duration = SnackbarDuration.Short,
            )) {
                SnackbarResult.Dismissed -> presenter.onEvent(BrowserUiEvent.SnackbarDismissed)
                SnackbarResult.ActionPerformed -> presenter.onEvent(BrowserUiEvent.SnackbarActionPerformed)
            }
        }
    }
    if (browserViewState.showCustomView) {
        CustomView(
            useBlackStatusBarStateFlow,
            browserViewState,
            customFrameLayout,
            snackbarHostState
        )
    } else {
        val tabConfiguration = tabConfigurationStateProvider.collectAsState()
        when (tabConfiguration.value) {
            TabConfiguration.DESKTOP -> DesktopTabs(
                useBlackStatusBarStateFlow,
                browserFrameLayout,
                browserViewState,
                presenter,
                suggestionsModel,
                snackbarHostState
            )

            TabConfiguration.DRAWER_SIDE -> DrawerTabs(
                useBlackStatusBarStateFlow,
                browserFrameLayout,
                browserViewState,
                presenter,
                suggestionsModel,
                snackbarHostState
            )

            TabConfiguration.DRAWER_BOTTOM -> BottomTabs(
                useBlackStatusBarStateFlow,
                browserFrameLayout,
                browserViewState,
                presenter,
                suggestionsModel,
                snackbarHostState
            )

            null -> Unit
        }
    }
}


