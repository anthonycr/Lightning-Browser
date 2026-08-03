package acr.browser.lightning.resources

import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

/**
 * Used to format numbers in a locale compatible way.
 */
interface NumberFormatter {

    /**
     * Format the provided [number] as a [String].
     */
    fun formatNumber(number: Int): String

}

/**
 * The default implementation of [NumberFormatter] That delegates to [NumberFormat].
 */
class DefaultNumberFormatter @Inject constructor(
    locale: Locale
) : NumberFormatter {

    private val formatter = NumberFormat.getInstance(locale)

    override fun formatNumber(number: Int): String = formatter.format(number)

}
