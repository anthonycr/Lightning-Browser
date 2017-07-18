package acr.browser.lightning.search

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.R
import acr.browser.lightning.database.HistoryItem
import acr.browser.lightning.database.bookmark.BookmarkModel
import acr.browser.lightning.database.history.HistoryModel
import acr.browser.lightning.preference.PreferenceManager
import acr.browser.lightning.utils.Preconditions
import acr.browser.lightning.utils.ThemeUtils
import android.app.Application
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.anthonycr.bonsai.*
import java.io.File
import java.io.FilenameFilter
import java.util.*
import javax.inject.Inject

class SuggestionsAdapter(private val context: Context, dark: Boolean, incognito: Boolean) : BaseAdapter(), Filterable {

    private val FILTER_SCHEDULER = Schedulers.newSingleThreadedScheduler()
    private val MAX_SUGGESTIONS = 5

    private val filteredList = ArrayList<HistoryItem>(5)

    private val history = ArrayList<HistoryItem>(5)
    private val bookmarks = ArrayList<HistoryItem>(5)
    private val suggestions = ArrayList<HistoryItem>(5)

    private val searchDrawable: Drawable
    private val historyDrawable: Drawable
    private val bookmarkDrawable: Drawable

    private val filterComparator = SuggestionsComparator()

    @Inject internal lateinit var bookmarkManager: BookmarkModel
    @Inject internal lateinit var preferenceManager: PreferenceManager
    @Inject internal lateinit var historyModel: HistoryModel
    @Inject internal lateinit var application: Application

    private val allBookmarks = ArrayList<HistoryItem>(5)

    private val darkTheme: Boolean
    private var isIncognito = true
    private var suggestionChoice: PreferenceManager.Suggestion? = null

    init {
        BrowserApp.getAppComponent().inject(this)
        darkTheme = dark || incognito
        isIncognito = incognito

        refreshPreferences()

        refreshBookmarks()

        searchDrawable = ThemeUtils.getThemedDrawable(context, R.drawable.ic_search, darkTheme)
        bookmarkDrawable = ThemeUtils.getThemedDrawable(context, R.drawable.ic_bookmark, darkTheme)
        historyDrawable = ThemeUtils.getThemedDrawable(context, R.drawable.ic_history, darkTheme)
    }

    fun refreshPreferences() {
        suggestionChoice = preferenceManager.searchSuggestionChoice
    }

    fun clearCache() {
        // We don't need these cache files anymore
        Schedulers.io().execute(ClearCacheRunnable(application))
    }

    fun refreshBookmarks() {
        bookmarkManager.allBookmarks
                .subscribeOn(Schedulers.io())
                .subscribe(object : SingleOnSubscribe<List<HistoryItem>>() {
                    override fun onItem(item: List<HistoryItem>?) {
                        Preconditions.checkNonNull(item)
                        allBookmarks.clear()
                        allBookmarks.addAll(item!!)
                    }
                })
    }

    override fun getCount(): Int {
        return filteredList.size
    }

    override fun getItem(position: Int): Any? {
        if (position > filteredList.size || position < 0) {
            return null
        }
        return filteredList[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    private class SuggestionHolder internal constructor(view: View) {

        internal val mImage = view.findViewById<ImageView>(R.id.suggestionIcon)
        internal val mTitle = view.findViewById<TextView>(R.id.title)
        internal val mUrl = view.findViewById<TextView>(R.id.url)

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
        val web: HistoryItem = filteredList[position]

        holder.mTitle.text = web.title
        holder.mUrl.text = web.url

        if (darkTheme) {
            holder.mTitle.setTextColor(Color.WHITE)
        }

        val image: Drawable
        when (web.imageId) {
            R.drawable.ic_bookmark -> {
                image = bookmarkDrawable
            }
            R.drawable.ic_search -> {
                image = searchDrawable
            }
            R.drawable.ic_history -> {
                image = historyDrawable
            }
            else -> image = searchDrawable
        }

        holder.mImage.setImageDrawable(image)

        return finalView
    }

    override fun getFilter(): Filter {
        return SearchFilter(this, historyModel)
    }

    @Synchronized private fun publishResults(list: List<HistoryItem>) {
        filteredList.clear()
        filteredList.addAll(list)
        notifyDataSetChanged()
    }

    private fun clearSuggestions() {
        Completable.create(CompletableAction { subscriber ->
            bookmarks.clear()
            history.clear()
            suggestions.clear()
            subscriber.onComplete()
        }).subscribeOn(FILTER_SCHEDULER)
                .observeOn(Schedulers.main())
                .subscribe()
    }

    private fun combineResults(bookmarkList: List<HistoryItem>?,
                               historyList: List<HistoryItem>?,
                               suggestionList: List<HistoryItem>?) {
        Single.create(SingleAction<List<HistoryItem>> { subscriber ->
            val list = ArrayList<HistoryItem>(5)
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
            while (list.size < MAX_SUGGESTIONS) {
                if (!bookmark.hasNext() && !suggestion.hasNext() && !history.hasNext()) {
                    break
                }
                if (bookmark.hasNext()) {
                    list.add(bookmark.next())
                }
                if (suggestion.hasNext() && list.size < MAX_SUGGESTIONS) {
                    list.add(suggestion.next())
                }
                if (history.hasNext() && list.size < MAX_SUGGESTIONS) {
                    list.add(history.next())
                }
            }

            Collections.sort(list, filterComparator)
            subscriber.onItem(list)
            subscriber.onComplete()
        }).subscribeOn(FILTER_SCHEDULER)
                .observeOn(Schedulers.main())
                .subscribe(object : SingleOnSubscribe<List<HistoryItem>>() {
                    override fun onItem(item: List<HistoryItem>?) {
                        Preconditions.checkNonNull(item)
                        publishResults(item!!)
                    }
                })
    }

    private fun getBookmarksForQuery(query: String): Single<List<HistoryItem>> {
        return Single.create(SingleAction<List<HistoryItem>> { subscriber ->
            val bookmarks = ArrayList<HistoryItem>(5)
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
            subscriber.onItem(bookmarks)
            subscriber.onComplete()
        })
    }

    private fun getSuggestionsForQuery(query: String): Single<List<HistoryItem>> {
        if (suggestionChoice == PreferenceManager.Suggestion.SUGGESTION_GOOGLE) {
            return SuggestionsManager.createGoogleQueryObservable(query, application)
        } else if (suggestionChoice == PreferenceManager.Suggestion.SUGGESTION_DUCK) {
            return SuggestionsManager.createDuckQueryObservable(query, application)
        } else if (suggestionChoice == PreferenceManager.Suggestion.SUGGESTION_BAIDU) {
            return SuggestionsManager.createBaiduQueryObservable(query, application)
        } else {
            return Single.empty<List<HistoryItem>>()
        }
    }

    private fun shouldRequestNetwork(): Boolean {
        return !isIncognito && suggestionChoice != PreferenceManager.Suggestion.SUGGESTION_NONE
    }

    private class SearchFilter internal constructor(private val suggestionsAdapter: SuggestionsAdapter,
                                                    private val historyModel: HistoryModel) : Filter() {

        override fun performFiltering(constraint: CharSequence?): Filter.FilterResults {
            val results = Filter.FilterResults()
            if (constraint == null || constraint.isEmpty()) {
                suggestionsAdapter.clearSuggestions()
                return results
            }
            val query = constraint.toString().toLowerCase(Locale.getDefault()).trim { it <= ' ' }

            if (suggestionsAdapter.shouldRequestNetwork() && !SuggestionsManager.isRequestInProgress) {
                suggestionsAdapter.getSuggestionsForQuery(query)
                        .subscribeOn(Schedulers.worker())
                        .observeOn(Schedulers.main())
                        .subscribe(object : SingleOnSubscribe<List<HistoryItem>>() {
                            override fun onItem(item: List<HistoryItem>?) {
                                suggestionsAdapter.combineResults(null, null, item)
                            }
                        })
            }

            suggestionsAdapter.getBookmarksForQuery(query)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.main())
                    .subscribe(object : SingleOnSubscribe<List<HistoryItem>>() {
                        override fun onItem(item: List<HistoryItem>?) {
                            suggestionsAdapter.combineResults(item, null, null)
                        }
                    })

            historyModel.findHistoryItemsContaining(query)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.main())
                    .subscribe(object : SingleOnSubscribe<List<HistoryItem>>() {
                        override fun onItem(item: List<HistoryItem>?) {
                            suggestionsAdapter.combineResults(null, item, null)
                        }
                    })
            results.count = 1
            return results
        }

        override fun convertResultToString(resultValue: Any): CharSequence {
            return (resultValue as HistoryItem).url
        }

        override fun publishResults(constraint: CharSequence?, results: Filter.FilterResults?) {
            suggestionsAdapter.combineResults(null, null, null)
        }
    }

    private class ClearCacheRunnable internal constructor(private val app: Application) : Runnable {

        override fun run() {
            val dir = File(app.cacheDir.toString())
            val fileList = dir.list(NameFilter())
            fileList.map { File(dir.path + it) }
                    .forEach { it.delete() }
        }

        private class NameFilter : FilenameFilter {

            private val CACHE_FILE_TYPE = ".sgg"

            override fun accept(dir: File, filename: String): Boolean {
                return filename.endsWith(CACHE_FILE_TYPE)
            }
        }
    }

    private class SuggestionsComparator : Comparator<HistoryItem> {

        override fun compare(lhs: HistoryItem, rhs: HistoryItem): Int {
            if (lhs.imageId == rhs.imageId) return 0
            if (lhs.imageId == R.drawable.ic_bookmark) return -1
            if (rhs.imageId == R.drawable.ic_bookmark) return 1
            if (lhs.imageId == R.drawable.ic_history) return -1
            return 1
        }
    }

}
