package acr.browser.lightning.settings.framework.compose

import acr.browser.lightning.settings.framework.SettingsToggleState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SettingsToggle(
    state: SettingsToggleState,
    onToggle: (Boolean) -> Unit
) {
    var toggleState by remember { mutableStateOf(state.isChecked) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = state.enabled) {
                toggleState = !toggleState
                onToggle(toggleState)
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .weight(1f, false),
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
        Column(
            modifier = Modifier
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Switch(
                enabled = state.enabled,
                checked = toggleState,
                onCheckedChange = {
                    toggleState = it
                    onToggle(toggleState)
                }
            )
        }
    }
}
