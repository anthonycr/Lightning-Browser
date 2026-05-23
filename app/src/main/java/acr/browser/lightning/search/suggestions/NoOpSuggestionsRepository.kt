package acr.browser.lightning.search.suggestions

import acr.browser.lightning.database.SearchSuggestion

/**
 * A search suggestions repository that doesn't fetch any results.
 */
class NoOpSuggestionsRepository : SuggestionsRepository {

    override suspend fun resultsForSearch(rawQuery: String) = emptyList<SearchSuggestion>()
}
