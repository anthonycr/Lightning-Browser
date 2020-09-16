package acr.browser.lightning._browser2

import acr.browser.lightning.R
import acr.browser.lightning._browser2.bookmark.BookmarkRecyclerViewAdapter
import acr.browser.lightning._browser2.image.ImageLoader
import acr.browser.lightning._browser2.search.SearchListener
import acr.browser.lightning._browser2.tab.TabRecyclerViewAdapter
import acr.browser.lightning.browser.activity.StyleRemovingTextWatcher
import acr.browser.lightning.browser.activity.ThemableBrowserActivity
import acr.browser.lightning.database.SearchSuggestion
import acr.browser.lightning.database.WebPage
import acr.browser.lightning.databinding.BrowserActivityBinding
import acr.browser.lightning.di.injector
import acr.browser.lightning.search.SuggestionsAdapter
import acr.browser.lightning.ssl.createSslDrawableForState
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import javax.inject.Inject

/**
 * Created by anthonycr on 9/11/20.
 */
class BrowserActivity : ThemableBrowserActivity(), BrowserContract.View {

    private lateinit var binding: BrowserActivityBinding
    private lateinit var tabsAdapter: TabRecyclerViewAdapter
    private lateinit var bookmarksAdapter: BookmarkRecyclerViewAdapter

    @Inject
    internal lateinit var imageLoader: ImageLoader

    @Inject
    internal lateinit var presenter: BrowserPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BrowserActivityBinding.inflate(LayoutInflater.from(this))

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        injector.browser2ComponentBuilder()
            .activity(this)
            .browserFrame(binding.contentFrame)
            .build()
            .inject(this)

        tabsAdapter = TabRecyclerViewAdapter(
            onClick = presenter::onTabClick,
            onCloseClick = presenter::onTabClose,
            onLongClick = {}
        )
        binding.tabsList.adapter = tabsAdapter
        binding.tabsList.layoutManager = LinearLayoutManager(this)

        bookmarksAdapter = BookmarkRecyclerViewAdapter(
            onClick = presenter::onBookmarkClick,
            onLongClick = presenter::onBookmarkLongClick,
            imageLoader = imageLoader
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
        binding.search.addTextChangedListener(StyleRemovingTextWatcher())

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val menu = when (item.itemId) {
            android.R.id.home -> TODO()
            R.id.action_back -> TODO()
            R.id.action_forward -> TODO()
            R.id.action_add_to_homescreen -> BrowserContract.Menu.ADD_TO_HOME
            R.id.action_new_tab -> BrowserContract.Menu.NEW_TAB
            R.id.action_incognito -> BrowserContract.Menu.NEW_INCOGNITO_TAB
            R.id.action_share -> BrowserContract.Menu.SHARE
            R.id.action_bookmarks -> BrowserContract.Menu.BOOKMARKS
            R.id.action_copy -> BrowserContract.Menu.COPY_LINK
            R.id.action_settings -> BrowserContract.Menu.SETTINGS
            R.id.action_history -> BrowserContract.Menu.HISTORY
            R.id.action_downloads -> BrowserContract.Menu.DOWNLOADS
            R.id.action_add_bookmark -> BrowserContract.Menu.ADD_BOOKMARK
            R.id.action_find -> BrowserContract.Menu.FIND
            R.id.action_reading_mode -> BrowserContract.Menu.READER
            else -> null
        }

        return menu?.let(presenter::onMenuClick)?.let { true }
            ?: super.onOptionsItemSelected(item)
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
        visibility = if (drawable == null) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }
}
