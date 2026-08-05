package acr.browser.lightning.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.StateFlow

@Composable
fun StatusBar(
    paintSurfaceColor: Boolean,
    useBlackStatusBarStateFlow: StateFlow<Boolean?>,
) {
    val blackStatus by useBlackStatusBarStateFlow.collectAsState()
    if (paintSurfaceColor || blackStatus == true) {
        val topInset = with(LocalDensity.current) {
            val statusBars = WindowInsets.statusBars
            statusBars.getTop(this).toDp()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(topInset)
                .zIndex(1F)
                .background(
                    if (blackStatus == true) {
                        Color.Black
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
        )
    }
}
