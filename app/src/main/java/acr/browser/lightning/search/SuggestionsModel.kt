package acr.browser.lightning.search

import acr.browser.lightning.concurrency.AppCoroutineScope
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.WebPage
import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.database.history.HistoryRepository
import acr.browser.lightning.di.IncognitoMode
import acr.browser.lightning.search.suggestions.NoOpSuggestionsRepository
import acr.browser.lightning.search.suggestions.SuggestionsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * Provides search suggestions based on the desired search suggestions choice.
 */
class SuggestionsModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
    private val historyRepository: HistoryRepository,
    searchEngineProvider: SearchEngineProvider,
    private val appCoroutineScope: AppCoroutineScope,
    private val coroutineDispatchers: CoroutineDispatchers,
    @IncognitoMode incognitoMode: Boolean
) {

    private var allBookmarks: List<Bookmark.Entry> = emptyList()
    private var suggestionsRepository: Deferred<SuggestionsRepository>
    private val inputFlow = MutableSharedFlow<CharSequence>(replay = 1)

    init {
        appCoroutineScope.launch {
            allBookmarks = bookmarkRepository.getAllBookmarksSorted()
        }
        suggestionsRepository = if (incognitoMode) {
            CompletableDeferred(NoOpSuggestionsRepository())
        } else {
            appCoroutineScope.async { searchEngineProvider.provideSearchSuggestions() }
        }
    }

    private fun getBookmarksForQuery(query: String): List<Bookmark.Entry> =
        (allBookmarks.filter {
            it.title.lowercase(Locale.getDefault()).startsWith(query)
        } + allBookmarks.filter {
            it.url.contains(query)
        }).distinct().take(MAX_SUGGESTIONS)

    /**
     * Update the current query which is emitting via [results].
     */
    fun updateQuery(query: CharSequence) {
        appCoroutineScope.launch(coroutineDispatchers.io) {
            inputFlow.emit(query)
        }
    }

    /**
     * Emits search suggestions as they are loaded. Each emission is not mutually exclusive and may
     * contain partial results from previous queries based on their importance according to the
     * ranking algorithm.
     *
     * Generally, the algorithm seeks to balance suggestions around
     * - 2 bookmark entries
     * - 2 history entries
     * - 1 search entry
     *
     * Entries are prioritized in that order, meaning that if there are no bookmarks to show in the
     * suggestions, history entries will be added in their place. If there are not enough history
     * entries, then search entries will be shown instead.
     */
    fun results(): Flow<List<WebPage>> = inputFlow
        .map { it.toString().lowercase(Locale.getDefault()).trim() }
        .filter { it.isNotEmpty() }
        .buffer(1, BufferOverflow.DROP_OLDEST)
        .let { sanitizedQuery ->
            val searchEntries: Flow<List<WebPage>> = sanitizedQuery.map {
                suggestionsRepository.await().resultsForSearch(it)
            }.onStart { emit(emptyList()) }
            val bookmarkEntries: Flow<List<WebPage>> = sanitizedQuery.map {
                getBookmarksForQuery(it)
            }.onStart { emit(emptyList()) }
            val historyEntries: Flow<List<WebPage>> = sanitizedQuery.map {
                historyRepository.findHistoryEntriesContaining(it)
            }.onStart { emit(emptyList()) }

            // Entries priority and ideal count:
            // Bookmarks - 2
            // History - 2
            // Search - 1

            combine(
                bookmarkEntries,
                historyEntries,
                searchEntries,
            ) { (bookmarks, history, searches) ->
                val bookmarkCount =
                    MAX_SUGGESTIONS - 2.coerceAtMost(history.size) - 1.coerceAtMost(searches.size)
                val historyCount =
                    MAX_SUGGESTIONS - bookmarkCount.coerceAtMost(bookmarks.size) - 1.coerceAtMost(
                        searches.size
                    )
                val searchCount =
                    MAX_SUGGESTIONS - bookmarkCount.coerceAtMost(bookmarks.size) - historyCount.coerceAtMost(
                        history.size
                    )

                bookmarks.take(bookmarkCount) + history.take(historyCount) + searches.take(
                    searchCount
                )
            }
        }

    companion object {
        private const val MAX_SUGGESTIONS = 5
    }
}
