package acr.browser.lightning.settings.framework.compose

import acr.browser.lightning.settings.framework.SettingsBottomSheetChooserState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheetChooser(
    innerPadding: PaddingValues,
    state: SettingsBottomSheetChooserState,
    onDismiss: () -> Unit,
    onSelected: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var selectedState by remember { mutableIntStateOf(state.selected) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        dragHandle = {},
        modifier = Modifier.padding(innerPadding),
        sheetState = sheetState
    ) {
        Row(
            modifier = Modifier
                .height(64.dp)
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                state.title,
                style = MaterialTheme.typography.titleLarge
            )
        }
        state.values.forEachIndexed { index, value ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(48.dp)
                    .clickable {
                        selectedState = index
                        scope.launch {
                            delay(500.milliseconds)
                            sheetState.hide()
                            onSelected(index)
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    RadioButton(
                        selected = index == selectedState,
                        onClick = {
                            scope.launch {
                                delay(500.milliseconds)
                                sheetState.hide()
                                onSelected(index)
                            }
                        }
                    )
                }
                Column(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(value, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
