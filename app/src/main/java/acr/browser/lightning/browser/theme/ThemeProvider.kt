package acr.browser.lightning.browser.theme

import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

/**
 * Provides themed attributes.
 */
interface ThemeProvider {

    /**
     * Provide a themed color attribute.
     */
    @ColorInt
    fun color(@AttrRes attrRes: Int): Int

}
