package acr.browser.lightning.compose

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith

/**
 * Create a [ContentTransform] that slides in from [initialOffsetX].
 *
 * @param initialOffsetX Using the current width of the content, return the initial offset.
 */
fun slideInFrom(initialOffsetX: (Int) -> Int): ContentTransform =
    slideInTransition(initialOffsetX).togetherWith(fadeOutTransition())

private fun slideInTransition(initialOffsetX: (Int) -> Int): EnterTransition = fadeIn(
    animationSpec = transitionTween()
) + slideInHorizontally(
    animationSpec = transitionTween(),
    initialOffsetX = initialOffsetX
)

private fun fadeOutTransition(): ExitTransition = fadeOut(animationSpec = transitionTween())

private fun <T> transitionTween(): TweenSpec<T> = tween(durationMillis = 220, delayMillis = 90)
