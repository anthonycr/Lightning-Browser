package acr.browser.lightning.browser.compose

import acr.browser.lightning.browser.BrowserComposeState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp

@Composable
fun TabCountButton(browserViewState: BrowserComposeState, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        val color = MaterialTheme.colorScheme.onSurface
        val textMeasurer = rememberTextMeasurer()
        val textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = color,
            fontWeight = FontWeight.Bold
        )
        Canvas(Modifier.size(24.dp)) {
            drawRoundRect(
                color = color,
                cornerRadius = CornerRadius(6.dp.toPx()),
                style = Stroke(width = 2.dp.toPx()),
            )
            val textLayout = textMeasurer.measure(
                style = textStyle,
                text = browserViewState.tabCountText
            )
            val textWidth = textLayout.size.width
            val textHeight = textLayout.size.height
            drawText(
                textLayoutResult = textLayout,
                color = color,
                topLeft = Offset(
                    12.dp.toPx() - textWidth / 2,
                    12.dp.toPx() - textHeight / 2
                )
            )
        }
    }
}
