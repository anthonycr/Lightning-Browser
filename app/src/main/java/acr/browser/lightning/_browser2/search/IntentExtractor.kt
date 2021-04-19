package acr.browser.lightning._browser2.search

import acr.browser.lightning._browser2.BrowserContract
import acr.browser.lightning.search.SearchEngineProvider
import acr.browser.lightning.utils.QUERY_PLACE_HOLDER
import acr.browser.lightning.utils.smartUrlFilter
import android.app.SearchManager
import android.content.Intent
import javax.inject.Inject

/**
 * Created by anthonycr on 9/20/20.
 */
class IntentExtractor @Inject constructor(private val searchEngineProvider: SearchEngineProvider) {

    /**
     * TODO
     */
    fun extractUrlFromIntent(intent: Intent): BrowserContract.Action? {
        return when (intent.action) {
            INTENT_PANIC_TRIGGER -> BrowserContract.Action.Panic
            Intent.ACTION_WEB_SEARCH ->
                extractSearchFromIntent(intent)?.let(BrowserContract.Action::LoadUrl)
            else -> intent.dataString?.let(BrowserContract.Action::LoadUrl)
        }
    }

    private fun extractSearchFromIntent(intent: Intent): String? {
        val query = intent.getStringExtra(SearchManager.QUERY)
        val searchUrl = "${searchEngineProvider.provideSearchEngine().queryUrl}$QUERY_PLACE_HOLDER"

        return if (query?.isNotBlank() == true) {
            smartUrlFilter(query, true, searchUrl)
        } else {
            null
        }
    }

    companion object {
        private const val INTENT_PANIC_TRIGGER = "info.guardianproject.panic.action.TRIGGER"
    }
}
