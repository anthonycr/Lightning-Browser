package acr.browser.lightning.preview

import android.graphics.Bitmap
import androidx.core.graphics.scale
import coil3.annotation.ExperimentalCoilApi
import coil3.decode.DecodeUtils
import coil3.size.Dimension
import coil3.size.Scale
import coil3.size.Size
import coil3.size.isOriginal
import coil3.size.pxOrElse
import coil3.transform.Transformation
import coil3.util.IntPair
import kotlin.math.roundToInt

object TopCropTransformation : Transformation() {
    override val cacheKey: String = "TopCropTransformation"

    @OptIn(ExperimentalCoilApi::class)
    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val outputSize = calculateOutputSize(input, size)
        val targetWidth = outputSize.first
        val targetHeight = outputSize.second

        val scaled = input.scale(
            targetWidth,
            (targetWidth * (input.height / targetWidth.toFloat())).roundToInt(),
            false
        )
        return Bitmap.createBitmap(scaled, 0, 0, targetWidth, targetHeight)
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun calculateOutputSize(input: Bitmap, size: Size): IntPair {
        if (size.isOriginal) {
            return IntPair(input.width, input.height)
        }

        val (dstWidth, dstHeight) = size
        if (dstWidth is Dimension.Pixels && dstHeight is Dimension.Pixels) {
            return IntPair(dstWidth.px, dstHeight.px)
        }

        val multiplier = DecodeUtils.computeSizeMultiplier(
            srcWidth = input.width,
            srcHeight = input.height,
            dstWidth = size.width.pxOrElse(Int.Companion::MIN_VALUE),
            dstHeight = size.height.pxOrElse(Int.Companion::MIN_VALUE),
            scale = Scale.FILL,
            maxSize = Size.ORIGINAL
        )
        val outputWidth = (multiplier * input.width).roundToInt()
        val outputHeight = (multiplier * input.height).roundToInt()
        return IntPair(outputWidth, outputHeight)
    }
}
