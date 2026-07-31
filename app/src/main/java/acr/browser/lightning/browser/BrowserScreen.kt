package acr.browser.lightning.browser

import acr.browser.lightning.ThemableActivity
import acr.browser.lightning.browser.compose.BottomTabs
import acr.browser.lightning.browser.compose.CustomView
import acr.browser.lightning.browser.compose.DesktopTabs
import acr.browser.lightning.browser.compose.DrawerTabs
import acr.browser.lightning.browser.ui.TabConfiguration
import acr.browser.lightning.compose.BrowserTheme
import acr.browser.lightning.concurrency.StateProvider
import acr.browser.lightning.search.SuggestionsModel
import android.widget.FrameLayout
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember

@Composable
fun ThemableActivity.BrowserScreen(
    tabConfigurationStateProvider: StateProvider<TabConfiguration>,
    blackStatusStateProvider: StateProvider<Boolean>,
    browserViewState: BrowserComposeState,
    presenter: BrowserPresenter,
    browserFrameLayout: FrameLayout,
    customFrameLayout: FrameLayout,
    suggestionsModel: SuggestionsModel,
) {
    BrowserTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        browserViewState.ephemeral?.let {
            LaunchedEffect(it.message) {
                when (snackbarHostState.showSnackbar(
                    message = it.message,
                    actionLabel = it.actionLabel,
                    duration = SnackbarDuration.Short,
                )) {
                    SnackbarResult.Dismissed -> presenter.onSnackbarDismissed()
                    SnackbarResult.ActionPerformed -> presenter.onSnackbarActionPerformed()
                }
            }
        }
        if (browserViewState.showCustomView) {
            CustomView(
                blackStatusStateProvider,
                browserViewState,
                customFrameLayout,
                snackbarHostState
            )
        } else {
            val tabConfiguration = tabConfigurationStateProvider.state.collectAsState()
            when (tabConfiguration.value) {
                TabConfiguration.DESKTOP -> DesktopTabs(
                    blackStatusStateProvider,
                    browserFrameLayout,
                    browserViewState,
                    presenter,
                    suggestionsModel,
                    snackbarHostState
                )

                TabConfiguration.DRAWER_SIDE -> DrawerTabs(
                    blackStatusStateProvider,
                    browserFrameLayout,
                    browserViewState,
                    presenter,
                    suggestionsModel,
                    snackbarHostState
                )

                TabConfiguration.DRAWER_BOTTOM -> BottomTabs(
                    blackStatusStateProvider,
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
}


