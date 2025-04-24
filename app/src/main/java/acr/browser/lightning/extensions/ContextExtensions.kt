@file:Suppress("NOTHING_TO_INLINE")

package acr.browser.lightning.extensions

import android.content.Context
import android.database.Cursor
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import io.reactivex.rxjava3.core.Maybe
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale


/**
 * Returns the dimension in pixels.
 *
 * @param dimenRes the dimension resource to fetch.
 */
inline fun Context.dimen(@DimenRes dimenRes: Int): Int = resources.getDimensionPixelSize(dimenRes)

/**
 * Returns the [ColorRes] as a [ColorInt]
 */
@ColorInt
inline fun Context.color(@ColorRes colorRes: Int): Int = ContextCompat.getColor(this, colorRes)

/**
 * Shows a toast with the provided [StringRes].
 */
inline fun Context.toast(@StringRes stringRes: Int) =
    Toast.makeText(this, stringRes, Toast.LENGTH_SHORT).show()

/**
 * The [LayoutInflater] available on the [Context].
 */
inline val Context.inflater: LayoutInflater
    get() = LayoutInflater.from(this)

/**
 * Gets a drawable from the context.
 */
inline fun Context.drawable(@DrawableRes drawableRes: Int): Drawable =
    ContextCompat.getDrawable(this, drawableRes)!!

inline fun Context.themedDrawable(
    @DrawableRes drawableRes: Int,
    @ColorInt colorInt: Int
): Drawable {
    val drawable = ContextCompat.getDrawable(this, drawableRes)!!
    drawable.setTint(colorInt)
    return drawable
}

/**
 * The preferred locale of the user.
 */
val Context.preferredLocale: Locale
    get() = resources.configuration.locales[0]

/**
 * Obtain the file name for the provided [Uri].
 */
fun Context.fileName(uri: Uri): String? {
    val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
    val metaCursor: Cursor? = contentResolver.query(uri, projection, null, null, null)
    metaCursor?.use {
        if (it.moveToFirst()) {
            return it.getString(0)
        }
    }
    return null
}

/**
 * Create an [OutputStream] from a [Uri]. If the [Uri] cannot be written to, this function emits a
 * completion signal.
 */
fun Context?.fileOutputStream(uri: Uri): Maybe<OutputStream> = Maybe.create {
    try {
        val outputStream = this?.contentResolver?.openOutputStream(uri)
            ?: return@create it.onComplete()

        return@create it.onSuccess(outputStream)
    } catch (exception: IOException) {
        return@create it.onComplete()
    }
}

/**
 * Create an [InputStream] from a [Uri]. If the [Uri] cannot be read from, this function emits a
 * completion signal.
 */
fun Context?.fileInputStream(uri: Uri): Maybe<InputStream> = Maybe.create {
    try {
        val inputStream = this?.contentResolver?.openInputStream(uri)
            ?: return@create it.onComplete()

        return@create it.onSuccess(inputStream)
    } catch (exception: IOException) {
        return@create it.onComplete()
    }
}
