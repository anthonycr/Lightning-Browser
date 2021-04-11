package acr.browser.lightning._browser2

import acr.browser.lightning.R
import acr.browser.lightning._browser2.bookmark.BookmarkRecyclerViewAdapter
import acr.browser.lightning._browser2.image.ImageLoader
import acr.browser.lightning._browser2.keys.KeyEventAdapter
import acr.browser.lightning._browser2.menu.MenuItemAdapter
import acr.browser.lightning._browser2.search.IntentExtractor
import acr.browser.lightning._browser2.search.SearchListener
import acr.browser.lightning._browser2.tab.*
import acr.browser.lightning._browser2.ui.BookmarkConfiguration
import acr.browser.lightning._browser2.ui.TabConfiguration
import acr.browser.lightning._browser2.ui.UiConfiguration
import acr.browser.lightning.browser.BrowserView
import acr.browser.lightning.browser.activity.StyleRemovingTextWatcher
import acr.browser.lightning.browser.activity.ThemableBrowserActivity
import acr.browser.lightning.constant.HTTP
import acr.browser.lightning.database.SearchSuggestion
import acr.browser.lightning.database.WebPage
import acr.browser.lightning.databinding.BrowserActivityBinding
import acr.browser.lightning.di.injector
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.dialog.DialogItem
import acr.browser.lightning.dialog.LightningDialogBuilder
import acr.browser.lightning.search.SuggestionsAdapter
import acr.browser.lightning.ssl.createSslDrawableForState
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import targetUrl.LongPress
import javax.inject.Inject

/**
 * Created by anthonycr on 9/11/20.
 */
class BrowserActivity : ThemableBrowserActivity() {

    private lateinit var binding: BrowserActivityBinding
    private lateinit var tabsAdapter: ListAdapter<TabViewState, TabViewHolder>
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

    @Inject
    internal lateinit var tabPager: TabPager

    @Inject
    internal lateinit var intentExtractor: IntentExtractor

    @Inject
    internal lateinit var lightningDialogBuilder: LightningDialogBuilder

    @Inject
    internal lateinit var uiConfiguration: UiConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BrowserActivityBinding.inflate(LayoutInflater.from(this))

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        injector.browser2ComponentBuilder()
            .activity(this)
            .browserFrame(binding.contentFrame)
            .toolbarRoot(binding.uiLayout)
            .toolbar(binding.toolbarLayout)
            .initialIntent(intent)
            .build()
            .inject(this)

        binding.bookmarkDrawer.layoutParams = (binding.bookmarkDrawer.layoutParams as DrawerLayout.LayoutParams).apply {
            gravity = when (uiConfiguration.bookmarkConfiguration) {
                BookmarkConfiguration.LEFT -> Gravity.START
                BookmarkConfiguration.RIGHT -> Gravity.END
            }
        }

        binding.tabDrawer.layoutParams = (binding.tabDrawer.layoutParams as DrawerLayout.LayoutParams).apply {
            gravity = when (uiConfiguration.bookmarkConfiguration) {
                BookmarkConfiguration.LEFT -> Gravity.END
                BookmarkConfiguration.RIGHT -> Gravity.START
            }
        }

        binding.homeImageView.isVisible = uiConfiguration.tabConfiguration == TabConfiguration.DESKTOP
        binding.tabCountView.isVisible = uiConfiguration.tabConfiguration == TabConfiguration.DRAWER

        if (uiConfiguration.tabConfiguration == TabConfiguration.DESKTOP) {
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, binding.tabDrawer)
        }

        if (uiConfiguration.tabConfiguration == TabConfiguration.DRAWER) {
            tabsAdapter = DrawerTabRecyclerViewAdapter(
                onClick = presenter::onTabClick,
                onCloseClick = presenter::onTabClose,
                onLongClick = presenter::onTabLongClick
            )
            binding.drawerTabsList.isVisible = true
            binding.drawerTabsList.adapter = tabsAdapter
            binding.drawerTabsList.layoutManager = LinearLayoutManager(this)
            binding.desktopTabsList.isVisible = false
        } else {
            tabsAdapter = DesktopTabRecyclerViewAdapter(
                context = this,
                onClick = presenter::onTabClick,
                onCloseClick = presenter::onTabClose,
                onLongClick = presenter::onTabLongClick
            )
            binding.desktopTabsList.isVisible = true
            binding.desktopTabsList.adapter = tabsAdapter
            binding.desktopTabsList.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
            binding.drawerTabsList.isVisible = false
        }

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
            binding.search.clearFocus()
            presenter.onSearchSuggestionClicked(suggestionsAdapter.getItem(position) as WebPage)
        }
        binding.search.setAdapter(suggestionsAdapter)
        val searchListener = SearchListener(
            onConfirm = {
                presenter.onSearch(binding.search.text.toString())
            },
            inputMethodManager = inputMethodManager
        )
        binding.search.setOnEditorActionListener(searchListener)
        binding.search.setOnKeyListener(searchListener)
        binding.search.addTextChangedListener(StyleRemovingTextWatcher())
        binding.search.setOnFocusChangeListener { _, hasFocus -> presenter.onSearchFocusChanged(hasFocus) }

        binding.findPrevious.setOnClickListener { presenter.onFindPrevious() }
        binding.findNext.setOnClickListener { presenter.onFindNext() }
        binding.findQuit.setOnClickListener { presenter.onFindDismiss() }

        binding.homeButton.setOnClickListener { presenter.onTabCountViewClick() }
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

        tabPager.longPressListener = presenter::onPageLongPress
    }

    override fun onNewIntent(intent: Intent?) {
        intent?.let(intentExtractor::extractUrlFromIntent)?.let(presenter::onNewAction)
        super.onNewIntent(intent)

    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onViewDetached()
    }

    override fun onPause() {
        super.onPause()
        presenter.onViewHidden()
    }

    override fun onBackPressed() {
        presenter.onBackClick()
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
        viewState.tabs?.let { binding.tabCountView.updateCount(it.size) }
        viewState.progress?.let { binding.progressView.progress = it }
        viewState.isRefresh?.let {
            binding.searchRefresh.setImageResource(if (it) {
                R.drawable.ic_action_refresh
            } else {
                R.drawable.ic_action_delete
            })
        }
        viewState.tabs?.let(tabsAdapter::submitList)
        viewState.bookmarks?.let(bookmarksAdapter::submitList)
        viewState.isBookmarked?.let { binding.actionAddBookmark.isSelected = it }
        viewState.isBookmarkEnabled?.let { binding.actionAddBookmark.isEnabled = it }
        viewState.isRootFolder?.let {
            binding.bookmarkBackButton.setImageResource(if (it) {
                R.drawable.ic_action_star
            } else {
                R.drawable.ic_action_back
            })
        }
        viewState.findInPage?.let {
            if (it.isEmpty()) {
                binding.findBar.isVisible = false
            } else {
                binding.findBar.isVisible = true
                binding.findQuery.text = it
            }
        }
    }

    fun showAddBookmarkDialog(title: String, url: String, folders: List<String>) {
        lightningDialogBuilder.showAddBookmarkDialog(
            activity = this,
            currentTitle = title,
            currentUrl = url,
            folders = folders,
            onSave = presenter::onBookmarkConfirmed
        )
    }

    fun showEditBookmarkDialog(title: String, url: String, folder: String, folders: List<String>) {
        lightningDialogBuilder.showEditBookmarkDialog(
            activity = this,
            currentTitle = title,
            currentUrl = url,
            currentFolder = folder,
            folders = folders,
            onSave = presenter::onBookmarkEditConfirmed
        )
    }

    fun showEditFolderDialog(oldTitle: String) {
        lightningDialogBuilder.showRenameFolderDialog(
            activity = this,
            oldTitle = oldTitle,
            onSave = presenter::onBookmarkFolderRenameConfirmed
        )
    }

    fun showFindInPageDialog() {
        BrowserDialog.showEditText(
            this,
            R.string.action_find,
            R.string.search_hint,
            R.string.search_hint,
            presenter::onFindInPage
        )
    }

    fun showLinkLongPressDialog(longPress: LongPress) {
        BrowserDialog.show(this, longPress.targetUrl?.replace(HTTP, ""),
            DialogItem(title = R.string.dialog_open_new_tab) {
                presenter.onLinkLongPressEvent(longPress, BrowserView.LinkLongPressEvent.NEW_TAB)
            },
            DialogItem(title = R.string.dialog_open_background_tab) {
                presenter.onLinkLongPressEvent(longPress, BrowserView.LinkLongPressEvent.BACKGROUND_TAB)
            },
            DialogItem(
                title = R.string.dialog_open_incognito_tab,
                isConditionMet = this is BrowserActivity // TODO: Change for incognito
            ) {
                presenter.onLinkLongPressEvent(longPress, BrowserView.LinkLongPressEvent.INCOGNITO_TAB)
            },
            DialogItem(title = R.string.action_share) {
                presenter.onLinkLongPressEvent(longPress, BrowserView.LinkLongPressEvent.SHARE)
            },
            DialogItem(title = R.string.dialog_copy_link) {
                presenter.onLinkLongPressEvent(longPress, BrowserView.LinkLongPressEvent.COPY_LINK)
            })
    }

    fun showImageLongPressDialog(longPress: LongPress) {
        BrowserDialog.show(this, longPress.targetUrl?.replace(HTTP, ""),
            DialogItem(title = R.string.dialog_open_new_tab) {
                presenter.onImageLongPressEvent(longPress, BrowserView.ImageLongPressEvent.NEW_TAB)
            },
            DialogItem(title = R.string.dialog_open_background_tab) {
                presenter.onImageLongPressEvent(longPress, BrowserView.ImageLongPressEvent.BACKGROUND_TAB)
            },
            DialogItem(
                title = R.string.dialog_open_incognito_tab,
                isConditionMet = this is BrowserActivity // TODO: Change for incognito
            ) {
                presenter.onImageLongPressEvent(longPress, BrowserView.ImageLongPressEvent.INCOGNITO_TAB)
            },
            DialogItem(title = R.string.action_share) {
                presenter.onImageLongPressEvent(longPress, BrowserView.ImageLongPressEvent.SHARE)
            },
            DialogItem(title = R.string.dialog_copy_link) {
                presenter.onImageLongPressEvent(longPress, BrowserView.ImageLongPressEvent.COPY_LINK)
            },
            DialogItem(title = R.string.dialog_download_image) {
                presenter.onImageLongPressEvent(longPress, BrowserView.ImageLongPressEvent.DOWNLOAD)
            })
    }

    fun openBookmarkDrawer() {
        binding.drawerLayout.closeDrawer(binding.tabDrawer)
        binding.drawerLayout.openDrawer(binding.bookmarkDrawer)
    }

    fun openTabDrawer() {
        binding.drawerLayout.closeDrawer(binding.bookmarkDrawer)
        binding.drawerLayout.openDrawer(binding.tabDrawer)
    }

    private fun ImageView.updateVisibilityForDrawable() {
        visibility = if (drawable == null) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }
}
