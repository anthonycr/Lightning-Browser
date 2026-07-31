package acr.browser.lightning.browser.compose

import acr.browser.lightning.browser.BrowserComposeState
import acr.browser.lightning.concurrency.StateProvider
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CustomView(
    blackStatusStateProvider: StateProvider<Boolean>,
    browserViewState: BrowserComposeState,
    frameLayout: FrameLayout,
    snackbarHostState: SnackbarHostState,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        BrowserStatusBar(
            browserComposeState = browserViewState,
            blackStatusStateProvider = blackStatusStateProvider,
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                factory = { frameLayout },
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceDim)
                    .weight(1f, false),
            )
        }
    }
}
