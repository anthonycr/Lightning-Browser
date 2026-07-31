package acr.browser.lightning.browser.compose

import acr.browser.lightning.R
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource

@Composable
fun BookmarkIcon(
    isBookmarked: Boolean,
) {
    if (isBookmarked) {
        Icon(
            painter = painterResource(R.drawable.ic_bookmark),
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = ""
        )
    } else {
        Icon(
            painter = painterResource(R.drawable.ic_action_star),
            contentDescription = ""
        )
    }
}
