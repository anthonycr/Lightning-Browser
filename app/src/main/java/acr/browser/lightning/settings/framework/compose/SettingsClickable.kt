package acr.browser.lightning.settings.framework.compose

import acr.browser.lightning.settings.framework.SettingsClickableState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SettingsClickable(
    state: SettingsClickableState,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = state.enabled) { onClick() },
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                state.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (state.enabled) {
                    Color.Unspecified
                } else {
                    ListItemDefaults.colors().disabledHeadlineColor
                }
            )
            state.summary?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.enabled) {
                        Color.Unspecified
                    } else {
                        ListItemDefaults.colors().disabledHeadlineColor
                    }
                )
            }
        }
    }
}
