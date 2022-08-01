package acr.browser.lightning._browser2.theme

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
