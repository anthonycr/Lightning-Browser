package acr.browser.lightning.settings.framework.compose

import acr.browser.lightning.R
import acr.browser.lightning.settings.framework.SettingsBottomSheetInputState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheetInput(
    innerPadding: PaddingValues,
    state: SettingsBottomSheetInputState,
    onDismiss: () -> Unit,
    onSelected: (CharSequence) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
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
                text = state.title,
                style = MaterialTheme.typography.titleLarge
            )
        }
        state.subtitle?.let { subtitle ->
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
        val textFieldState = rememberTextFieldState(state.currentValue)
        TextField(
            textFieldState,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            placeholder = { Text(state.hint) }
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
                        onSelected(textFieldState.text)
                    }
                }
            ) {
                Text(stringResource(R.string.action_ok))
            }
        }
    }
}
