package acr.browser.lightning._browser2

import acr.browser.lightning.R
import acr.browser.lightning._browser2.bookmark.BookmarkRecyclerViewAdapter
import acr.browser.lightning._browser2.search.SearchListener
import acr.browser.lightning._browser2.tab.TabPager
import acr.browser.lightning._browser2.tab.TabRecyclerViewAdapter
import acr.browser.lightning._browser2.tab.TabsRepository
import acr.browser.lightning._browser2.tab.WebViewFactory
import acr.browser.lightning.browser.activity.ThemableBrowserActivity
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.HistoryEntry
import acr.browser.lightning.database.SearchSuggestion
import acr.browser.lightning.database.WebPage
import acr.browser.lightning.database.bookmark.BookmarkDatabase
import acr.browser.lightning.databinding.BrowserActivityBinding
import acr.browser.lightning.device.ScreenSize
import acr.browser.lightning.log.AndroidLogger
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.search.SuggestionsAdapter
import acr.browser.lightning.ssl.createSslDrawableForState
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.widget.AdapterView
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by anthonycr on 9/11/20.
 */
class BrowserActivity : ThemableBrowserActivity(), BrowserContract.View {

    private lateinit var binding: BrowserActivityBinding
    private lateinit var presenter: BrowserPresenter
    private lateinit var tabsAdapter: TabRecyclerViewAdapter
    private lateinit var bookmarksAdapter: BookmarkRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BrowserActivityBinding.inflate(LayoutInflater.from(this))

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        presenter = BrowserPresenter(
            bookmarkRepository = BookmarkDatabase(application),
            model = TabsRepository(
                webViewFactory = WebViewFactory(
                    activity = this,
                    logger = AndroidLogger(),
                    userPreferences = UserPreferences(
                        preferences = application.getSharedPreferences("settings", 0),
                        screenSize = ScreenSize(this)
                    )
                ),
                tabPager = TabPager(binding.contentFrame)
            ),
            mainScheduler = AndroidSchedulers.mainThread(),
            databaseScheduler = Schedulers.single()
        )

        tabsAdapter = TabRecyclerViewAdapter(
            onClick = presenter::onTabClick,
            onCloseClick = presenter::onTabClose,
            onLongClick = {}
        )
        binding.tabsList.adapter = tabsAdapter
        binding.tabsList.layoutManager = LinearLayoutManager(this)

        bookmarksAdapter = BookmarkRecyclerViewAdapter(
            onClick = presenter::onBookmarkClick,
            onLongClick = presenter::onBookmarkLongClick
        )
        binding.bookmarkListView.adapter = bookmarksAdapter
        binding.bookmarkListView.layoutManager = LinearLayoutManager(this)

        presenter.onViewAttached(this)

        val suggestionsAdapter = SuggestionsAdapter(this, isIncognito = false).apply {
            onSuggestionInsertClick = {
                if (it is SearchSuggestion) {
                    binding.search.setText(it.title)
                    binding.search.setSelection(it.title.length)
                } else {
                    binding.search.setText(it.url)
                    binding.search.setSelection(it.url.length)
                }
            }
        }
        binding.search.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            presenter.onSearchSuggestionClicked(suggestionsAdapter.getItem(position) as WebPage)
        }
        binding.search.setAdapter(suggestionsAdapter)
        val searchListener = SearchListener(onConfirm = {
            presenter.onSearch(binding.search.text.toString())
        })
        binding.search.setOnEditorActionListener(searchListener)
        binding.search.setOnKeyListener(searchListener)

        binding.actionBack.setOnClickListener { presenter.onBackClick() }
        binding.actionForward.setOnClickListener { presenter.onForwardClick() }
        binding.actionHome.setOnClickListener { presenter.onHomeClick() }
        binding.newTabButton.setOnClickListener { presenter.onNewTabClick() }
        binding.searchRefresh.setOnClickListener { presenter.onRefreshOrStopClick() }
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onViewDetached()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun renderState(viewState: BrowserViewState) {
        binding.actionBack.isEnabled = viewState.isBackEnabled
        binding.actionForward.isEnabled = viewState.isForwardEnabled
        binding.search.setText(viewState.displayUrl)
        binding.searchSslStatus.setImageDrawable(createSslDrawableForState(viewState.sslState))
        binding.searchSslStatus.updateVisibilityForDrawable()
        binding.tabCountView.updateCount(viewState.tabs.size)
        binding.progressView.progress = viewState.progress
        binding.searchRefresh.setImageResource(if (viewState.isRefresh) {
            R.drawable.ic_action_refresh
        } else {
            R.drawable.ic_action_delete
        })
        tabsAdapter.submitList(viewState.tabs)
        bookmarksAdapter.submitList(viewState.bookmarks)
    }

    private fun ImageView.updateVisibilityForDrawable() {
        if (drawable == null) {
            visibility = View.GONE
        } else {
            visibility = View.VISIBLE
        }
    }
}
