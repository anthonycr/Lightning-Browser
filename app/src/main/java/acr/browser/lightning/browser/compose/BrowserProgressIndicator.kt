package acr.browser.lightning.browser.compose

import acr.browser.lightning.browser.BrowserComposeState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BrowserProgressIndicator(browserViewState: BrowserComposeState) {
    if (browserViewState.progress == 100) {
        Spacer(modifier = Modifier.height(4.dp))
    } else {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            trackColor = Color(0x00000000),
            drawStopIndicator = {},
            progress = { browserViewState.progress / 100f }
        )
    }
}
