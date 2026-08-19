package acr.browser.lightning.settings.framework.compose

import acr.browser.lightning.R
import acr.browser.lightning.settings.framework.SettingsTextSizeChooserState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheetTextSizeChooser(
    innerPadding: PaddingValues,
    state: SettingsTextSizeChooserState,
    onDismiss: () -> Unit,
    onSelected: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var selectedTextSize by remember { mutableIntStateOf(state.textSize) }
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
                text = stringResource(R.string.title_text_size),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = when (selectedTextSize) {
                        0 -> 30.sp
                        1 -> 26.sp
                        2 -> 22.sp
                        3 -> 18.sp
                        4 -> 14.sp
                        5 -> 10.sp
                        else -> error("Impossible selection")
                    }
                ),
            )
        }
        Slider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            value = (5 - selectedTextSize).toFloat(),
            onValueChange = {
                selectedTextSize = 5 - it.toInt()
            },
            valueRange = 0f..5f,
            steps = 4
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                modifier = Modifier
                    .padding(end = 16.dp),
                onClick = {
                    scope.launch {
                        delay(500.milliseconds)
                        sheetState.hide()
                        onSelected(selectedTextSize)
                    }
                }
            ) {
                Text(stringResource(R.string.action_ok))
            }
        }
    }
}
