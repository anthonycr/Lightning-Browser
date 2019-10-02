package acr.browser.lightning.ssl

import acr.browser.lightning.R
import acr.browser.lightning.utils.DrawableUtils
import acr.browser.lightning.utils.ThemeUtils
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

/**
 * Creates the proper [Drawable] to represent the [SslState].
 */
fun Context.createSslDrawableForState(sslState: SslState): Drawable? = when (sslState) {
    is SslState.None -> null
    is SslState.Valid -> {
        val drawableBackgroundColor = ThemeUtils.getPrimaryColor(this)
        val bitmap = DrawableUtils.createImageInsetInRoundedSquare(this, R.drawable.ic_secured, drawableBackgroundColor)
        val securedDrawable = BitmapDrawable(resources, bitmap)
        securedDrawable
    }
    is SslState.Invalid -> {
        val drawableBackgroundColor = ThemeUtils.getPrimaryColor(this)
        val bitmap = DrawableUtils.createImageInsetInRoundedSquare(this, R.drawable.ic_unsecured, drawableBackgroundColor)
        val unsecuredDrawable = BitmapDrawable(resources, bitmap)
        unsecuredDrawable
    }
}
