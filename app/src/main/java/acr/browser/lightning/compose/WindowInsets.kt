package acr.browser.lightning.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Alternative to [imePadding] that accounts for bottom navigation and content insets.
 */
@Composable
fun Modifier.imeInsetsPadding(
    extraOffset: Dp = 0.dp,
    innerPadding: PaddingValues,
): Modifier = windowInsetsPadding(
    WindowInsets.ime
        .add(WindowInsets(bottom = extraOffset))
        .add(WindowInsets(bottom = -(innerPadding.calculateBottomPadding())))
)
