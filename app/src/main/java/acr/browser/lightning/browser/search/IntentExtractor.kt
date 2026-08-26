package acr.browser.lightning.browser.search

import acr.browser.lightning.browser.BrowserContract
import android.app.SearchManager
import android.content.Intent
import javax.inject.Inject

/**
 * Extracts data from an [Intent] and into a [BrowserContract.Action].
 */
class IntentExtractor @Inject constructor() {

    /**
     * Extract the action from the [intent] or return null if no data was extracted.
     */
    fun extractUrlFromIntent(intent: Intent?): BrowserContract.Action? {
        return when (intent?.action) {
            INTENT_PANIC_TRIGGER -> BrowserContract.Action.Panic
            Intent.ACTION_WEB_SEARCH -> extractSearchFromIntent(intent)

            Intent.ACTION_VIEW -> intent.dataString?.let(BrowserContract.Action::LoadUrl)
            else -> intent?.dataString?.let(BrowserContract.Action::LoadUrl)
        }
    }

    private fun extractSearchFromIntent(intent: Intent): BrowserContract.Action? {
        val query = intent.getStringExtra(SearchManager.QUERY)
        return if (query?.isNotBlank() == true) {
            BrowserContract.Action.Search(query)
        } else {
            null
        }
    }

    companion object {
        private const val INTENT_PANIC_TRIGGER = "info.guardianproject.panic.action.TRIGGER"
    }
}
