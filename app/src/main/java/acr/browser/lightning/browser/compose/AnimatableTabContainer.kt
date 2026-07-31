package acr.browser.lightning.browser.compose

import android.widget.FrameLayout
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun AnimatableTabContainer(
    frameLayout: FrameLayout,
    offsetDp: Dp,
    fixedOffset: Int,
    mutableOffset: MutableIntState,
) {
    AndroidView(
        factory = { frameLayout },
        modifier = Modifier
            .padding(top = offsetDp * (fixedOffset + mutableOffset.intValue) / fixedOffset.toFloat())
            // TODO: Offset is smoother, but incorrectly includes window insets
            // .offset {
            //     IntOffset(0, fixedOffset + mutableOffset.intValue)
            // }
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceDim),
    )
}

@Composable
fun AnimateMutableOffset(
    shouldShow: Boolean,
    mutableOffset: MutableIntState,
    maxOffset: Int
) {
    LaunchedEffect(shouldShow) {
        if (shouldShow && mutableOffset.intValue != 0) {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) { value, _ ->
                mutableOffset.intValue = ((maxOffset * value) + -maxOffset).toInt()
            }
        } else if (!shouldShow && mutableOffset.intValue == 0) {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) { value, _ ->
                mutableOffset.intValue = (value * -maxOffset).toInt()
            }
        }
    }
}
