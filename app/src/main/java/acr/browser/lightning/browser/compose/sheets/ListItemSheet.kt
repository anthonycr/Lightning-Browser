package acr.browser.lightning.browser.compose.sheets

import acr.browser.lightning.browser.BrowserPresenter
import acr.browser.lightning.dialog.DialogItem
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListItemSheet(
    title: String,
    items: List<DialogItem>,
    presenter: BrowserPresenter,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        sheetState = sheetState,
        dragHandle = {},
        onDismissRequest = { presenter.onDialogDismissed() }
    ) {
        Row(
            modifier = Modifier
                .height(64.dp)
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column {
            items.forEach { item ->
                if (item.isConditionMet) {
                    Row(
                        modifier = Modifier
                            .height(56.dp)
                            .fillMaxWidth()
                            .clickable(onClick = { item.onClick() }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item.icon?.let { icon ->
                            Icon(
                                modifier = Modifier.padding(start = 16.dp),
                                painter = painterResource(icon),
                                contentDescription = "test",
                                tint = item.colorTint?.let {
                                    colorResource(it)
                                } ?: LocalContentColor.current
                            )
                        }
                        Text(
                            modifier = Modifier
                                .padding(16.dp),
                            text = stringResource(item.title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}
