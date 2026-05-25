package acr.browser.lightning.search

import acr.browser.lightning.R
import acr.browser.lightning.browser.di.injector
import acr.browser.lightning.concurrency.AppCoroutineScope
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.HistoryEntry
import acr.browser.lightning.database.SearchSuggestion
import acr.browser.lightning.database.WebPage
import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.database.history.HistoryRepository
import acr.browser.lightning.extensions.drawable
import acr.browser.lightning.search.suggestions.NoOpSuggestionsRepository
import acr.browser.lightning.search.suggestions.SuggestionsRepository
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class SuggestionsAdapter(
    context: Context,
    private val isIncognito: Boolean
) : BaseAdapter(), Filterable {

    private var filteredList: List<WebPage> = emptyList()

    @Inject internal lateinit var bookmarkRepository: BookmarkRepository
    @Inject internal lateinit var historyRepository: HistoryRepository
    @Inject internal lateinit var searchEngineProvider: SearchEngineProvider
    @Inject internal lateinit var appCoroutineScope: AppCoroutineScope
    @Inject internal lateinit var coroutineDispatchers: CoroutineDispatchers

    private var allBookmarks: List<Bookmark.Entry> = emptyList()
    private val searchFilter by lazy {
        SearchFilter(this, appCoroutineScope)
    }

    private val searchIcon = context.drawable(R.drawable.ic_search)
    private val webPageIcon = context.drawable(R.drawable.ic_history)
    private val bookmarkIcon = context.drawable(R.drawable.ic_bookmark)
    private var suggestionsRepository: SuggestionsRepository

    /**
     * The listener that is fired when the insert button on a [SearchSuggestion] is clicked.
     */
    var onSuggestionInsertClick: ((WebPage) -> Unit)? = null

    private val onClick = View.OnClickListener {
        onSuggestionInsertClick?.invoke(it.tag as WebPage)
    }

    private val layoutInflater = LayoutInflater.from(context)

    init {
        context.injector.inject(this)

        suggestionsRepository = if (isIncognito) {
            NoOpSuggestionsRepository()
        } else {
            searchEngineProvider.provideSearchSuggestions()
        }

        refreshBookmarks()

        appCoroutineScope.launch(coroutineDispatchers.main) {
            searchFilter.input().results().collectLatest {
                publishResults(it)
            }
        }
    }

    fun refreshPreferences() {
        suggestionsRepository = if (isIncognito) {
            NoOpSuggestionsRepository()
        } else {
            searchEngineProvider.provideSearchSuggestions()
        }
    }

    fun refreshBookmarks() {
        appCoroutineScope.launch {
            allBookmarks = bookmarkRepository.getAllBookmarksSorted()
        }
    }

    override fun getCount(): Int = filteredList.size

    override fun getItem(position: Int): Any? {
        if (position > filteredList.size || position < 0) {
            return null
        }
        return filteredList[position]
    }

    override fun getItemId(position: Int): Long = 0

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: SuggestionViewHolder
        val finalView: View

        if (convertView == null) {
            finalView = layoutInflater.inflate(R.layout.two_line_autocomplete, parent, false)

            holder = SuggestionViewHolder(finalView)
            finalView.tag = holder
        } else {
            finalView = convertView
            holder = convertView.tag as SuggestionViewHolder
        }
        val webPage: WebPage = filteredList[position]

        holder.titleView.text = webPage.title
        holder.urlView.text = webPage.url

        val image = when (webPage) {
            is Bookmark -> bookmarkIcon
            is SearchSuggestion -> searchIcon
            is HistoryEntry -> webPageIcon
        }

        holder.imageView.setImageDrawable(image)

        holder.insertSuggestion.tag = webPage
        holder.insertSuggestion.setOnClickListener(onClick)

        return finalView
    }

    override fun getFilter(): Filter = searchFilter

    private fun publishResults(list: List<WebPage>?) {
        if (list == null) {
            notifyDataSetChanged()
            return
        }
        if (list != filteredList) {
            filteredList = list
            notifyDataSetChanged()
        }
    }

    private fun getBookmarksForQuery(query: String): List<Bookmark.Entry> =
        (allBookmarks.filter {
            it.title.lowercase(Locale.getDefault()).startsWith(query)
        } + allBookmarks.filter {
            it.url.contains(query)
        }).distinct().take(MAX_SUGGESTIONS)

    private fun Flow<CharSequence>.results() = flowOn(coroutineDispatchers.default)
        .map { it.toString().lowercase(Locale.getDefault()).trim() }
        .filter { it.isNotEmpty() }
        .buffer(1, BufferOverflow.DROP_OLDEST)
        .let { sanitizedQuery ->
            val searchEntries: Flow<List<WebPage>> = sanitizedQuery.map {
                suggestionsRepository.resultsForSearch(it)
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
                searchEntries,
                bookmarkEntries,
                historyEntries
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

    private class SearchFilter(
        private val suggestionsAdapter: SuggestionsAdapter,
        private val appCoroutineScope: AppCoroutineScope,
    ) : Filter() {

        private val inputFlow = MutableSharedFlow<CharSequence>()

        fun input(): Flow<CharSequence> = inputFlow.asSharedFlow()

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            if (constraint?.isBlank() != false) {
                return FilterResults()
            }
            appCoroutineScope.launch {
                inputFlow.emit(constraint.trim())
            }

            return FilterResults().apply { count = 1 }
        }

        override fun convertResultToString(resultValue: Any) = (resultValue as WebPage).url

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) =
            suggestionsAdapter.publishResults(null)
    }

}
