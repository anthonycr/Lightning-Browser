package acr.browser.lightning.search.suggestions

import acr.browser.lightning.database.HistoryItem
import io.reactivex.Single

/**
 * A search suggestions repository that doesn't fetch any results.
 */
class NoOpSuggestionsRepository : SuggestionsRepository {

    private val emptySingle: Single<List<HistoryItem>> = Single.just(emptyList())

    override fun resultsForSearch(rawQuery: String) = emptySingle
}