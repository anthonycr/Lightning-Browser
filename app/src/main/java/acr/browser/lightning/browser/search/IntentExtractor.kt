package acr.browser.lightning.browser.search

import acr.browser.lightning.browser.BrowserContract
import acr.browser.lightning.device.BuildInfo
import android.app.SearchManager
import android.content.Intent
import javax.inject.Inject

/**
 * Extracts data from an [Intent] and into a [BrowserContract.Action].
 */
class IntentExtractor @Inject constructor(
    private val buildInfo: BuildInfo
) {

    /**
     * Extract the action from the [intent] or return null if no data was extracted.
     */
    fun extractUrlFromIntent(intent: Intent?): BrowserContract.Action? {
        val internalOrigin = intent?.component?.packageName == buildInfo.packageName
        return when (intent?.action) {
            INTENT_PANIC_TRIGGER -> BrowserContract.Action.Panic
            Intent.ACTION_WEB_SEARCH -> extractSearchFromIntent(intent, internalOrigin)

            Intent.ACTION_VIEW -> intent.dataString?.let { url ->
                BrowserContract.Action.LoadUrl(url, internalOrigin)
            }

            else -> null
        }
    }

    private fun extractSearchFromIntent(
        intent: Intent,
        internalOrigin: Boolean,
    ): BrowserContract.Action? {
        val query = intent.getStringExtra(SearchManager.QUERY)
        return if (query?.isNotBlank() == true) {
            BrowserContract.Action.Search(query, internalOrigin)
        } else {
            null
        }
    }

    companion object {
        private const val INTENT_PANIC_TRIGGER = "info.guardianproject.panic.action.TRIGGER"
    }
}
