package acr.browser.lightning.search

import acr.browser.lightning.browser.di.IncognitoMode
import acr.browser.lightning.concurrency.AppCoroutineScope
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.WebPage
import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.database.history.HistoryRepository
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
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

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

    fun updateQuery(query: CharSequence) {
        appCoroutineScope.launch(coroutineDispatchers.io) {
            inputFlow.emit(query)
        }
    }

    fun results() = inputFlow
        .map { it.toString().lowercase(Locale.getDefault()).trim() }
        .filter { it.isNotEmpty() }
        .buffer(1, BufferOverflow.DROP_OLDEST)
        .let { sanitizedQuery ->
            val searchEntries: Flow<List<WebPage>> = sanitizedQuery.map {
                suggestionsRepository.await().resultsForSearch(it)
            }
            val bookmarkEntries: Flow<List<WebPage>> = sanitizedQuery.map {
                getBookmarksForQuery(it)
            }
            val historyEntries: Flow<List<WebPage>> = sanitizedQuery.map {
                historyRepository.findHistoryEntriesContaining(it)
            }

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
