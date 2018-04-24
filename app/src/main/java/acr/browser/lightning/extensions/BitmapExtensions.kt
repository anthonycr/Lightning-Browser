package acr.browser.lightning.extensions

import acr.browser.lightning.utils.Utils
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

/**
 * Creates and returns a new favicon which is the same as the provided favicon but with horizontal
 * and vertical padding of 4dp
 *
 * @return the padded bitmap.
 */
fun Bitmap.pad(): Bitmap = let {
    val padding = Utils.dpToPx(4f)
    val width = it.width + padding
    val height = it.height + padding

    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        Canvas(this).apply {
            drawARGB(0x00, 0x00, 0x00, 0x00) // this represents white color
            drawBitmap(it, (padding / 2).toFloat(), (padding / 2).toFloat(), Paint(Paint.FILTER_BITMAP_FLAG))
        }
    }
}