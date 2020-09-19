package acr.browser.lightning._browser2

import acr.browser.lightning.R
import acr.browser.lightning._browser2.bookmark.BookmarkRecyclerViewAdapter
import acr.browser.lightning._browser2.image.ImageLoader
import acr.browser.lightning._browser2.keys.KeyEventAdapter
import acr.browser.lightning._browser2.menu.MenuItemAdapter
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
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import javax.inject.Inject

/**
 * Created by anthonycr on 9/11/20.
 */
class BrowserActivity : ThemableBrowserActivity() {

    private lateinit var binding: BrowserActivityBinding
    private lateinit var tabsAdapter: TabRecyclerViewAdapter
    private lateinit var bookmarksAdapter: BookmarkRecyclerViewAdapter

    @Inject
    internal lateinit var imageLoader: ImageLoader

    @Inject
    internal lateinit var keyEventAdapter: KeyEventAdapter

    @Inject
    internal lateinit var menuItemAdapter: MenuItemAdapter

    @Inject
    internal lateinit var inputMethodManager: InputMethodManager

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

        presenter.onViewAttached(BrowserStateAdapter(this))

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
        }, inputMethodManager)
        binding.search.setOnEditorActionListener(searchListener)
        binding.search.setOnKeyListener(searchListener)
        binding.search.addTextChangedListener(StyleRemovingTextWatcher())
        binding.search.setOnFocusChangeListener { _, hasFocus -> presenter.onSearchFocusChanged(hasFocus) }

        binding.actionBack.setOnClickListener { presenter.onBackClick() }
        binding.actionForward.setOnClickListener { presenter.onForwardClick() }
        binding.actionHome.setOnClickListener { presenter.onHomeClick() }
        binding.newTabButton.setOnClickListener { presenter.onNewTabClick() }
        binding.searchRefresh.setOnClickListener { presenter.onRefreshOrStopClick() }
        binding.actionAddBookmark.setOnClickListener { presenter.onStarClick() }
        binding.actionPageTools.setOnClickListener { presenter.onToolsClick() }
        binding.tabHeaderButton.setOnClickListener { presenter.onTabMenuClick() }
        binding.actionReading.setOnClickListener { presenter.onReadingModeClick() }
        binding.bookmarkBackButton.setOnClickListener { presenter.onBookmarkMenuClick() }
        binding.searchSslStatus.setOnClickListener { presenter.onSslIconClick() }
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
        return menuItemAdapter.adaptMenuItem(item)?.let(presenter::onMenuClick)?.let { true }
            ?: super.onOptionsItemSelected(item)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return keyEventAdapter.adaptKeyEvent(event)?.let(presenter::onKeyComboClick)?.let { true }
            ?: super.onKeyUp(keyCode, event)
    }

    /**
     * TODO
     */
    fun renderState(viewState: PartialBrowserViewState) {
        viewState.isBackEnabled?.let { binding.actionBack.isEnabled = it }
        viewState.isForwardEnabled?.let { binding.actionForward.isEnabled = it }
        viewState.displayUrl?.let(binding.search::setText)
        viewState.sslState?.let {
            binding.searchSslStatus.setImageDrawable(createSslDrawableForState(it))
            binding.searchSslStatus.updateVisibilityForDrawable()
        }
        viewState.tabs?.let { binding.tabCountView.updateCount(viewState.tabs.size) }
        viewState.progress?.let { binding.progressView.progress = it }
        viewState.isRefresh?.let {
            binding.searchRefresh.setImageResource(if (viewState.isRefresh) {
                R.drawable.ic_action_refresh
            } else {
                R.drawable.ic_action_delete
            })
        }
        viewState.tabs?.let { tabsAdapter.submitList(viewState.tabs) }
        viewState.bookmarks?.let { bookmarksAdapter.submitList(viewState.bookmarks) }
        viewState.isBookmarked?.let { binding.actionAddBookmark.isSelected = it }
        viewState.isBookmarkEnabled?.let { binding.actionAddBookmark.isEnabled = it }
    }

    private fun ImageView.updateVisibilityForDrawable() {
        visibility = if (drawable == null) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }
}
