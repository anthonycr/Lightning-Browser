package acr.browser.lightning.search

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.R
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.HistoryEntry
import acr.browser.lightning.database.SearchSuggestion
import acr.browser.lightning.database.WebPage
import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.database.history.HistoryRepository
import acr.browser.lightning.di.DatabaseScheduler
import acr.browser.lightning.di.MainScheduler
import acr.browser.lightning.di.NetworkScheduler
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.search.suggestions.NoOpSuggestionsRepository
import acr.browser.lightning.search.suggestions.SuggestionsRepository
import acr.browser.lightning.utils.ThemeUtils
import android.app.Application
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import io.reactivex.Completable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.Executors
import javax.inject.Inject

class SuggestionsAdapter(
    private val context: Context,
    dark: Boolean,
    private val isIncognito: Boolean
) : BaseAdapter(), Filterable {

    private val filterScheduler = Schedulers.from(Executors.newSingleThreadExecutor())
    private val maxSuggestions = 5

    private val filteredList = arrayListOf<WebPage>()

    private val history = arrayListOf<HistoryEntry>()
    private val bookmarks = arrayListOf<Bookmark.Entry>()
    private val suggestions = arrayListOf<SearchSuggestion>()

    private val searchDrawable: Drawable
    private val historyDrawable: Drawable
    private val bookmarkDrawable: Drawable

    private val filterComparator = SuggestionsComparator()

    @Inject internal lateinit var bookmarkManager: BookmarkRepository
    @Inject internal lateinit var userPreferences: UserPreferences
    @Inject internal lateinit var historyModel: HistoryRepository
    @Inject internal lateinit var application: Application
    @Inject @field:DatabaseScheduler internal lateinit var databaseScheduler: Scheduler
    @Inject @field:NetworkScheduler internal lateinit var networkScheduler: Scheduler
    @Inject @field:MainScheduler internal lateinit var mainScheduler: Scheduler
    @Inject internal lateinit var searchEngineProvider: SearchEngineProvider

    private val allBookmarks = arrayListOf<Bookmark.Entry>()
    private val darkTheme: Boolean
    private val searchFilter: SearchFilter

    init {
        BrowserApp.appComponent.inject(this)
        darkTheme = dark || isIncognito

        val suggestionsRepository = if (isIncognito) {
            NoOpSuggestionsRepository()
        } else {
            searchEngineProvider.provideSearchSuggestions()
        }

        searchFilter = SearchFilter(suggestionsRepository,
            this,
            historyModel,
            databaseScheduler,
            networkScheduler,
            mainScheduler
        )

        refreshBookmarks()

        searchDrawable = ThemeUtils.getThemedDrawable(context, R.drawable.ic_search, darkTheme)
        bookmarkDrawable = ThemeUtils.getThemedDrawable(context, R.drawable.ic_bookmark, darkTheme)
        historyDrawable = ThemeUtils.getThemedDrawable(context, R.drawable.ic_history, darkTheme)
    }

    fun refreshPreferences() {
        searchFilter.suggestionsRepository = if (isIncognito) {
            NoOpSuggestionsRepository()
        } else {
            searchEngineProvider.provideSearchSuggestions()
        }
    }

    fun refreshBookmarks() {
        bookmarkManager.getAllBookmarks()
            .subscribeOn(databaseScheduler)
            .subscribe { list ->
                allBookmarks.clear()
                allBookmarks.addAll(list)
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

    private class SuggestionHolder(view: View) {

        val imageView: ImageView = view.findViewById(R.id.suggestionIcon)
        val titleView: TextView = view.findViewById(R.id.title)
        val urlView: TextView = view.findViewById(R.id.url)

    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val holder: SuggestionHolder
        val finalView: View

        if (convertView == null) {
            val inflater = LayoutInflater.from(context)
            finalView = inflater.inflate(R.layout.two_line_autocomplete, parent, false)

            holder = SuggestionHolder(finalView)
            finalView.tag = holder
        } else {
            finalView = convertView
            holder = convertView.tag as SuggestionHolder
        }
        val webPage: WebPage = filteredList[position]

        holder.titleView.text = webPage.title
        holder.urlView.text = webPage.url

        if (darkTheme) {
            holder.titleView.setTextColor(Color.WHITE)
        }

        val image = when (webPage) {
            is Bookmark.Entry -> bookmarkDrawable
            is SearchSuggestion -> searchDrawable
            is HistoryEntry -> historyDrawable
            else -> searchDrawable
        }

        holder.imageView.setImageDrawable(image)

        return finalView
    }

    override fun getFilter(): Filter = searchFilter

    private fun publishResults(list: List<WebPage>) {
        if (list != filteredList) {
            filteredList.clear()
            filteredList.addAll(list)
            notifyDataSetChanged()
        }
    }

    private fun clearSuggestions() {
        Completable
            .fromAction {
                bookmarks.clear()
                history.clear()
                suggestions.clear()
            }
            .subscribeOn(filterScheduler)
            .observeOn(mainScheduler)
            .subscribe()
    }

    private fun combineResults(
        bookmarkList: List<Bookmark.Entry>?,
        historyList: List<HistoryEntry>?,
        suggestionList: List<SearchSuggestion>?
    ) {
        Single.create<List<WebPage>> {
            val list = mutableListOf<WebPage>()
            if (bookmarkList != null) {
                bookmarks.clear()
                bookmarks.addAll(bookmarkList)
            }
            if (historyList != null) {
                history.clear()
                history.addAll(historyList)
            }
            if (suggestionList != null) {
                suggestions.clear()
                suggestions.addAll(suggestionList)
            }
            val bookmark = bookmarks.iterator()
            val history = history.iterator()
            val suggestion = suggestions.listIterator()
            while (list.size < maxSuggestions) {
                if (!bookmark.hasNext() && !suggestion.hasNext() && !history.hasNext()) {
                    break
                }
                if (bookmark.hasNext()) {
                    list.add(bookmark.next())
                }
                if (suggestion.hasNext() && list.size < maxSuggestions) {
                    list.add(suggestion.next())
                }
                if (history.hasNext() && list.size < maxSuggestions) {
                    list.add(history.next())
                }
            }

            Collections.sort(list, filterComparator)
            it.onSuccess(list)
        }
            .subscribeOn(filterScheduler)
            .observeOn(mainScheduler)
            .subscribe(this::publishResults)
    }

    private fun getBookmarksForQuery(query: String): Single<List<Bookmark.Entry>> =
        Single.fromCallable {
            val bookmarks = arrayListOf<Bookmark.Entry>()
            var counter = 0
            for (n in allBookmarks.indices) {
                if (counter >= 5) {
                    break
                }
                if (allBookmarks[n].title.toLowerCase(Locale.getDefault())
                        .startsWith(query)) {
                    bookmarks.add(allBookmarks[n])
                    counter++
                } else if (allBookmarks[n].url.contains(query)) {
                    bookmarks.add(allBookmarks[n])
                    counter++
                }
            }
            return@fromCallable bookmarks
        }

    private class SearchFilter internal constructor(
        var suggestionsRepository: SuggestionsRepository,
        private val suggestionsAdapter: SuggestionsAdapter,
        private val historyModel: HistoryRepository,
        private val databaseScheduler: Scheduler,
        private val networkScheduler: Scheduler,
        private val mainScheduler: Scheduler
    ) : Filter() {

        private var networkDisposable: Disposable? = null
        private var historyDisposable: Disposable? = null
        private var bookmarkDisposable: Disposable? = null

        override fun performFiltering(constraint: CharSequence?): Filter.FilterResults {
            val results = Filter.FilterResults()
            if (constraint == null || constraint.isEmpty()) {
                suggestionsAdapter.clearSuggestions()
                return results
            }
            val query = constraint.toString().toLowerCase(Locale.getDefault()).trim()

            if (networkDisposable?.isDisposed != false) {
                networkDisposable = suggestionsRepository.resultsForSearch(query)
                    .subscribeOn(networkScheduler)
                    .observeOn(mainScheduler)
                    .subscribe { item ->
                        suggestionsAdapter.combineResults(null, null, item)
                    }
            }

            if (bookmarkDisposable?.isDisposed != false) {
                bookmarkDisposable = suggestionsAdapter.getBookmarksForQuery(query)
                    .subscribeOn(databaseScheduler)
                    .observeOn(mainScheduler)
                    .subscribe { list ->
                        suggestionsAdapter.combineResults(list, null, null)
                    }
            }

            if (historyDisposable?.isDisposed != false) {
                historyDisposable = historyModel.findHistoryEntriesContaining(query)
                    .subscribeOn(databaseScheduler)
                    .observeOn(mainScheduler)
                    .subscribe { list ->
                        suggestionsAdapter.combineResults(null, list, null)
                    }
            }

            results.count = 1
            return results
        }

        override fun convertResultToString(resultValue: Any) = (resultValue as WebPage).url

        override fun publishResults(constraint: CharSequence?, results: Filter.FilterResults?) =
            suggestionsAdapter.combineResults(null, null, null)
    }

    private class SuggestionsComparator : Comparator<WebPage> {
        override fun compare(lhs: WebPage, rhs: WebPage): Int {
            if (lhs::class.java == rhs::class.java) {
                return 0
            }
            if (lhs is Bookmark) {
                return -1
            }
            if (rhs is Bookmark) {
                return 1
            }
            if (lhs is HistoryEntry) {
                return -1
            }
            return 1
        }
    }

}
