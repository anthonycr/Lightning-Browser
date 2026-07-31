package acr.browser.lightning.browser.compose

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.tan

val DesktopTabShape: Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radians = Math.PI / 3
        val base = (size.height / tan(radians)).toInt()

        return Outline.Generic(
            Path().apply {
                reset()
                moveTo(0f, size.height)
                lineTo(size.width, size.height)
                lineTo((size.width - base), 0f)
                lineTo(base.toFloat(), 0f)
                close()
            }
        )
    }

}
