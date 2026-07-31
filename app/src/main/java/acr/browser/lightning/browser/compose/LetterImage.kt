package acr.browser.lightning.browser.compose

import acr.browser.lightning.graphics.LetterImagePainter
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.Canvas
import coil3.Image

class LetterImage(
    private val letterImagePainter: LetterImagePainter,
    override val width: Int,
    override val height: Int,
) : Image {

    override val size: Long = 0
    override val shareable: Boolean = false

    override fun draw(canvas: Canvas) {
        letterImagePainter.drawOn(canvas)
    }

    companion object {
        @Composable
        fun create(
            density: Density,
            character: Char,
            size: Int,
        ) = LetterImage(
            letterImagePainter = LetterImagePainter(
                textSize = with(density) { 14.sp.toPx() },
                radius = with(density) { 6.dp.toPx() },
                character = character,
                size = size,
                color = colorResource(LetterImagePainter.colorForCharacter(character).resource).toArgb()
            ),
            width = size,
            height = size,
        )
    }
}
