package acr.browser.lightning.browser.compose

import acr.browser.lightning.R
import acr.browser.lightning.browser.BrowserComposeState
import acr.browser.lightning.browser.BrowserPresenter
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun BrowserFindInPage(
    browserViewState: BrowserComposeState,
    presenter: BrowserPresenter,
) {
    val findInPage = browserViewState.findInPage ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { presenter.onFindDismiss() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_delete),
                    contentDescription = "test"
                )
            }
            var text by remember { mutableStateOf(findInPage) }
            val interactionSource = remember { MutableInteractionSource() }
            BasicTextField(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth()
                    .weight(1f, false)
                    .indicatorLine(
                        enabled = true,
                        isError = false,
                        interactionSource = interactionSource
                    ),
                value = text,
                onValueChange = {
                    text = it
                    presenter.onFindInPage(it)
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                singleLine = true,
                interactionSource = interactionSource,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurfaceVariant),
                decorationBox = { innerTextField ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.action_find),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )
            IconButton(onClick = { presenter.onFindPrevious() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_collapse),
                    contentDescription = "test"
                )
            }
            IconButton(onClick = { presenter.onFindNext() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_expand),
                    contentDescription = "test"
                )
            }
        }
        HorizontalDivider()
    }
}
