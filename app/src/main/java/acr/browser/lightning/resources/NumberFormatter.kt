package acr.browser.lightning.resources

import acr.browser.lightning.extensions.preferredLocale
import android.app.Application
import java.text.NumberFormat
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

class DefaultNumberFormatter @Inject constructor(
    application: Application
) : NumberFormatter {

    private val formatter = NumberFormat.getInstance(application.preferredLocale)

    override fun formatNumber(number: Int): String = formatter.format(number)

}
