package acr.browser.lightning.search.suggestions

import acr.browser.lightning.database.SearchSuggestion
import io.reactivex.Single

/**
 * A search suggestions repository that doesn't fetch any results.
 */
class NoOpSuggestionsRepository : SuggestionsRepository {

    private val emptySingle: Single<List<SearchSuggestion>> = Single.just(emptyList())

    override fun resultsForSearch(rawQuery: String) = emptySingle
}
