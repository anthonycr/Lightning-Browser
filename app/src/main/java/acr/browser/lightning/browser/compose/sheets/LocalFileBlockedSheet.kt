package acr.browser.lightning.browser.compose.sheets

import acr.browser.lightning.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
fun LocalFileBlockedSheet(
    onConfirmed: (Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = { onConfirmed(false) },
        dragHandle = {},
        sheetState = sheetState
    ) {
        Row(
            modifier = Modifier.height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.title_warning),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleLarge
            )
        }
        Text(
            text = stringResource(R.string.message_blocked_local),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
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
                        onConfirmed(false)
                    }
                }
            ) {
                Text(stringResource(R.string.action_cancel))
            }
            Button(
                modifier = Modifier
                    .padding(end = 16.dp),
                onClick = {
                    scope.launch {
                        delay(500.milliseconds)
                        sheetState.hide()
                        onConfirmed(true)
                    }
                }
            ) {
                Text(stringResource(R.string.action_open))
            }
        }
    }
}
