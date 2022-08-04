package acr.browser.lightning.browser

import acr.browser.lightning.AppTheme
import acr.browser.lightning.R
import acr.browser.lightning.ThemableBrowserActivity
import acr.browser.lightning.browser.bookmark.BookmarkRecyclerViewAdapter
import acr.browser.lightning.browser.color.ColorAnimator
import acr.browser.lightning.browser.image.ImageLoader
import acr.browser.lightning.browser.keys.KeyEventAdapter
import acr.browser.lightning.browser.menu.MenuItemAdapter
import acr.browser.lightning.browser.search.IntentExtractor
import acr.browser.lightning.browser.search.SearchListener
import acr.browser.lightning.browser.tab.*
import acr.browser.lightning.browser.ui.BookmarkConfiguration
import acr.browser.lightning.browser.ui.TabConfiguration
import acr.browser.lightning.browser.ui.UiConfiguration
import acr.browser.lightning.browser.search.StyleRemovingTextWatcher
import acr.browser.lightning.constant.HTTP
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.HistoryEntry
import acr.browser.lightning.database.SearchSuggestion
import acr.browser.lightning.database.WebPage
import acr.browser.lightning.database.downloads.DownloadEntry
import acr.browser.lightning.databinding.BrowserActivityBinding
import acr.browser.lightning.browser.di.injector
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.dialog.DialogItem
import acr.browser.lightning.dialog.LightningDialogBuilder
import acr.browser.lightning.extensions.*
import acr.browser.lightning.search.SuggestionsAdapter
import acr.browser.lightning.ssl.createSslDrawableForState
import acr.browser.lightning.utils.ProxyUtils
import acr.browser.lightning.utils.value
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.MenuRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import acr.browser.lightning.browser.view.targetUrl.LongPress
import javax.inject.Inject

/**
 * The base browser activity that governs the browsing experience for both default and incognito
 * browsers.
 */
abstract class BrowserActivity : ThemableBrowserActivity() {

    private lateinit var binding: BrowserActivityBinding
    private lateinit var tabsAdapter: ListAdapter<TabViewState, TabViewHolder>
    private lateinit var bookmarksAdapter: BookmarkRecyclerViewAdapter

    private var menuItemShare: MenuItem? = null
    private var menuItemCopyLink: MenuItem? = null
    private var menuItemAddToHome: MenuItem? = null
    private var menuItemAddBookmark: MenuItem? = null
    private var menuItemReaderMode: MenuItem? = null

    private val defaultColor by lazy { color(R.color.primary_color) }
    private val backgroundDrawable by lazy { ColorDrawable(defaultColor) }

    private var customView: View? = null

    @Suppress("ConvertLambdaToReference")
    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { presenter.onFileChooserResult(it) }

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

    @Inject
    internal lateinit var proxyUtils: ProxyUtils

    /**
     * True if the activity is operating in incognito mode, false otherwise.
     */
    abstract fun isIncognito(): Boolean

    /**
     * Provide the menu used by the browser instance.
     */
    @MenuRes
    abstract fun menu(): Int

    /**
     * Provide the home icon used by the browser instance.
     */
    @DrawableRes
    abstract fun homeIcon(): Int

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
            .incognitoMode(isIncognito())
            .build()
            .inject(this)

        binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {

            override fun onDrawerOpened(drawerView: View) {
                if (drawerView == binding.tabDrawer) {
                    presenter.onTabDrawerMoved(isOpen = true)
                } else if (drawerView == binding.bookmarkDrawer) {
                    presenter.onBookmarkDrawerMoved(isOpen = true)
                }
            }

            override fun onDrawerClosed(drawerView: View) {
                if (drawerView == binding.tabDrawer) {
                    presenter.onTabDrawerMoved(isOpen = false)
                } else if (drawerView == binding.bookmarkDrawer) {
                    presenter.onBookmarkDrawerMoved(isOpen = false)
                }
            }
        })

        binding.bookmarkDrawer.layoutParams =
            (binding.bookmarkDrawer.layoutParams as DrawerLayout.LayoutParams).apply {
                gravity = when (uiConfiguration.bookmarkConfiguration) {
                    BookmarkConfiguration.LEFT -> Gravity.START
                    BookmarkConfiguration.RIGHT -> Gravity.END
                }
            }

        binding.tabDrawer.layoutParams =
            (binding.tabDrawer.layoutParams as DrawerLayout.LayoutParams).apply {
                gravity = when (uiConfiguration.bookmarkConfiguration) {
                    BookmarkConfiguration.LEFT -> Gravity.END
                    BookmarkConfiguration.RIGHT -> Gravity.START
                }
            }

        binding.homeImageView.isVisible =
            uiConfiguration.tabConfiguration == TabConfiguration.DESKTOP || isIncognito()
        binding.homeImageView.setImageResource(homeIcon())
        binding.tabCountView.isVisible =
            uiConfiguration.tabConfiguration == TabConfiguration.DRAWER && !isIncognito()

        if (uiConfiguration.tabConfiguration == TabConfiguration.DESKTOP) {
            binding.drawerLayout.setDrawerLockMode(
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                binding.tabDrawer
            )
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
            binding.desktopTabsList.layoutManager =
                LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
            binding.desktopTabsList.itemAnimator?.takeIfInstance<SimpleItemAnimator>()
                ?.supportsChangeAnimations = false
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

        val suggestionsAdapter = SuggestionsAdapter(this, isIncognito = isIncognito()).apply {
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
            inputMethodManager.hideSoftInputFromWindow(binding.root.windowToken, 0)
        }
        binding.search.setAdapter(suggestionsAdapter)
        val searchListener = SearchListener(
            onConfirm = { presenter.onSearch(binding.search.text.toString()) },
            inputMethodManager = inputMethodManager
        )
        binding.search.setOnEditorActionListener(searchListener)
        binding.search.setOnKeyListener(searchListener)
        binding.search.addTextChangedListener(StyleRemovingTextWatcher())
        binding.search.setOnFocusChangeListener { _, hasFocus ->
            binding.search.selectAll()
            presenter.onSearchFocusChanged(hasFocus)
        }

        binding.findPrevious.setOnClickListener { presenter.onFindPrevious() }
        binding.findNext.setOnClickListener { presenter.onFindNext() }
        binding.findQuit.setOnClickListener { presenter.onFindDismiss() }

        binding.homeButton.setOnClickListener { presenter.onTabCountViewClick() }
        binding.actionBack.setOnClickListener { presenter.onBackClick() }
        binding.actionForward.setOnClickListener { presenter.onForwardClick() }
        binding.actionHome.setOnClickListener { presenter.onHomeClick() }
        binding.newTabButton.setOnClickListener { presenter.onNewTabClick() }
        binding.newTabButton.setOnLongClickListener {
            presenter.onNewTabLongClick()
            true
        }
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
        presenter.onNavigateBack()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(menu(), menu)
        menuItemShare = menu.findItem(R.id.action_share)
        menuItemCopyLink = menu.findItem(R.id.action_copy)
        menuItemAddToHome = menu.findItem(R.id.action_add_to_homescreen)
        menuItemAddBookmark = menu.findItem(R.id.action_add_bookmark)
        menuItemReaderMode = menu.findItem(R.id.action_reading_mode)
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
     * @see BrowserContract.View.renderState
     */
    fun renderState(viewState: PartialBrowserViewState) {
        viewState.isBackEnabled?.let { binding.actionBack.isEnabled = it }
        viewState.isForwardEnabled?.let { binding.actionForward.isEnabled = it }
        viewState.displayUrl?.let(binding.search::setText)
        viewState.sslState?.let {
            binding.searchSslStatus.setImageDrawable(createSslDrawableForState(it))
            binding.searchSslStatus.updateVisibilityForDrawable()
        }
        viewState.enableFullMenu?.let {
            menuItemShare?.isVisible = it
            menuItemCopyLink?.isVisible = it
            menuItemAddToHome?.isVisible = it
            menuItemAddBookmark?.isVisible = it
            menuItemReaderMode?.isVisible = it
        }
        viewState.themeColor?.value()?.let(::animateColorChange)
        viewState.progress?.let { binding.progressView.progress = it }
        viewState.isRefresh?.let {
            binding.searchRefresh.setImageResource(
                if (it) {
                    R.drawable.ic_action_refresh
                } else {
                    R.drawable.ic_action_delete
                }
            )
        }
        viewState.bookmarks?.let(bookmarksAdapter::submitList)
        viewState.isBookmarked?.let { binding.actionAddBookmark.isSelected = it }
        viewState.isBookmarkEnabled?.let { binding.actionAddBookmark.isEnabled = it }
        viewState.isRootFolder?.let {
            binding.bookmarkBackButton.setImageResource(
                if (it) {
                    R.drawable.ic_action_star
                } else {
                    R.drawable.ic_action_back
                }
            )
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

    /**
     * @see BrowserContract.View.renderTabs
     */
    fun renderTabs(tabListState: List<TabViewState>) {
        binding.tabCountView.updateCount(tabListState.size)
        tabsAdapter.submitList(tabListState)
    }

    /**
     * @see BrowserContract.View.showAddBookmarkDialog
     */
    fun showAddBookmarkDialog(title: String, url: String, folders: List<String>) {
        lightningDialogBuilder.showAddBookmarkDialog(
            activity = this,
            currentTitle = title,
            currentUrl = url,
            folders = folders,
            onSave = presenter::onBookmarkConfirmed
        )
    }

    /**
     * @see BrowserContract.View.showBookmarkOptionsDialog
     */
    fun showBookmarkOptionsDialog(bookmark: Bookmark.Entry) {
        lightningDialogBuilder.showLongPressedDialogForBookmarkUrl(
            activity = this,
            onClick = {
                presenter.onBookmarkOptionClick(bookmark, it)
            }
        )
    }

    /**
     * @see BrowserContract.View.showEditBookmarkDialog
     */
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

    /**
     * @see BrowserContract.View.showFolderOptionsDialog
     */
    fun showFolderOptionsDialog(folder: Bookmark.Folder) {
        lightningDialogBuilder.showBookmarkFolderLongPressedDialog(
            activity = this,
            onClick = {
                presenter.onFolderOptionClick(folder, it)
            }
        )
    }

    /**
     * @see BrowserContract.View.showEditFolderDialog
     */
    fun showEditFolderDialog(oldTitle: String) {
        lightningDialogBuilder.showRenameFolderDialog(
            activity = this,
            oldTitle = oldTitle,
            onSave = presenter::onBookmarkFolderRenameConfirmed
        )
    }

    /**
     * @see BrowserContract.View.showDownloadOptionsDialog
     */
    fun showDownloadOptionsDialog(download: DownloadEntry) {
        lightningDialogBuilder.showLongPressedDialogForDownloadUrl(
            activity = this,
            onClick = {
                presenter.onDownloadOptionClick(download, it)
            }
        )
    }

    /**
     * @see BrowserContract.View.showHistoryOptionsDialog
     */
    fun showHistoryOptionsDialog(historyEntry: HistoryEntry) {
        lightningDialogBuilder.showLongPressedHistoryLinkDialog(
            activity = this,
            onClick = {
                presenter.onHistoryOptionClick(historyEntry, it)
            }
        )
    }

    /**
     * @see BrowserContract.View.showFindInPageDialog
     */
    fun showFindInPageDialog() {
        BrowserDialog.showEditText(
            this,
            R.string.action_find,
            R.string.search_hint,
            R.string.search_hint,
            presenter::onFindInPage
        )
    }

    /**
     * @see BrowserContract.View.showLinkLongPressDialog
     */
    fun showLinkLongPressDialog(longPress: LongPress) {
        BrowserDialog.show(this, longPress.targetUrl?.replace(HTTP, ""),
            DialogItem(title = R.string.dialog_open_new_tab) {
                presenter.onLinkLongPressEvent(
                    longPress,
                    BrowserContract.LinkLongPressEvent.NEW_TAB
                )
            },
            DialogItem(title = R.string.dialog_open_background_tab) {
                presenter.onLinkLongPressEvent(
                    longPress,
                    BrowserContract.LinkLongPressEvent.BACKGROUND_TAB
                )
            },
            DialogItem(
                title = R.string.dialog_open_incognito_tab,
                isConditionMet = !isIncognito()
            ) {
                presenter.onLinkLongPressEvent(
                    longPress,
                    BrowserContract.LinkLongPressEvent.INCOGNITO_TAB
                )
            },
            DialogItem(title = R.string.action_share) {
                presenter.onLinkLongPressEvent(longPress, BrowserContract.LinkLongPressEvent.SHARE)
            },
            DialogItem(title = R.string.dialog_copy_link) {
                presenter.onLinkLongPressEvent(
                    longPress,
                    BrowserContract.LinkLongPressEvent.COPY_LINK
                )
            })
    }

    /**
     * @see BrowserContract.View.showImageLongPressDialog
     */
    fun showImageLongPressDialog(longPress: LongPress) {
        BrowserDialog.show(this, longPress.targetUrl?.replace(HTTP, ""),
            DialogItem(title = R.string.dialog_open_new_tab) {
                presenter.onImageLongPressEvent(
                    longPress,
                    BrowserContract.ImageLongPressEvent.NEW_TAB
                )
            },
            DialogItem(title = R.string.dialog_open_background_tab) {
                presenter.onImageLongPressEvent(
                    longPress,
                    BrowserContract.ImageLongPressEvent.BACKGROUND_TAB
                )
            },
            DialogItem(
                title = R.string.dialog_open_incognito_tab,
                isConditionMet = !isIncognito()
            ) {
                presenter.onImageLongPressEvent(
                    longPress,
                    BrowserContract.ImageLongPressEvent.INCOGNITO_TAB
                )
            },
            DialogItem(title = R.string.action_share) {
                presenter.onImageLongPressEvent(
                    longPress,
                    BrowserContract.ImageLongPressEvent.SHARE
                )
            },
            DialogItem(title = R.string.dialog_copy_link) {
                presenter.onImageLongPressEvent(
                    longPress,
                    BrowserContract.ImageLongPressEvent.COPY_LINK
                )
            },
            DialogItem(title = R.string.dialog_download_image) {
                presenter.onImageLongPressEvent(
                    longPress,
                    BrowserContract.ImageLongPressEvent.DOWNLOAD
                )
            })
    }

    /**
     * @see BrowserContract.View.showCloseBrowserDialog
     */
    fun showCloseBrowserDialog(id: Int) {
        BrowserDialog.show(
            this, R.string.dialog_title_close_browser,
            DialogItem(title = R.string.close_tab) {
                presenter.onCloseBrowserEvent(id, BrowserContract.CloseTabEvent.CLOSE_CURRENT)
            },
            DialogItem(title = R.string.close_other_tabs) {
                presenter.onCloseBrowserEvent(id, BrowserContract.CloseTabEvent.CLOSE_OTHERS)
            },
            DialogItem(title = R.string.close_all_tabs, onClick = {
                presenter.onCloseBrowserEvent(id, BrowserContract.CloseTabEvent.CLOSE_ALL)
            })
        )
    }

    /**
     * @see BrowserContract.View.openBookmarkDrawer
     */
    fun openBookmarkDrawer() {
        binding.drawerLayout.closeDrawer(binding.tabDrawer)
        binding.drawerLayout.openDrawer(binding.bookmarkDrawer)
    }

    /**
     * @see BrowserContract.View.closeBookmarkDrawer
     */
    fun closeBookmarkDrawer() {
        binding.drawerLayout.closeDrawer(binding.bookmarkDrawer)
    }

    /**
     * @see BrowserContract.View.openTabDrawer
     */
    fun openTabDrawer() {
        binding.drawerLayout.closeDrawer(binding.bookmarkDrawer)
        binding.drawerLayout.openDrawer(binding.tabDrawer)
    }

    /**
     * @see BrowserContract.View.closeTabDrawer
     */
    fun closeTabDrawer() {
        binding.drawerLayout.closeDrawer(binding.tabDrawer)
    }

    /**
     * @see BrowserContract.View.showToolbar
     */
    fun showToolbar() {
        tabPager.showToolbar()
    }

    /**
     * @see BrowserContract.View.showToolsDialog
     */
    fun showToolsDialog(areAdsAllowed: Boolean, shouldShowAdBlockOption: Boolean) {
        val whitelistString = if (areAdsAllowed) {
            R.string.dialog_adblock_enable_for_site
        } else {
            R.string.dialog_adblock_disable_for_site
        }

        BrowserDialog.showWithIcons(
            this, getString(R.string.dialog_tools_title),
            DialogItem(
                icon = drawable(R.drawable.ic_action_desktop),
                title = R.string.dialog_toggle_desktop,
                onClick = presenter::onToggleDesktopAgent
            ),
            DialogItem(
                icon = drawable(R.drawable.ic_block),
                colorTint = color(R.color.error_red).takeIf { areAdsAllowed },
                title = whitelistString,
                isConditionMet = shouldShowAdBlockOption,
                onClick = presenter::onToggleAdBlocking
            )
        )
    }

    /**
     * @see BrowserContract.View.showLocalFileBlockedDialog
     */
    fun showLocalFileBlockedDialog() {
        AlertDialog.Builder(this)
            .setCancelable(true)
            .setTitle(R.string.title_warning)
            .setMessage(R.string.message_blocked_local)
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                presenter.onConfirmOpenLocalFile(allow = false)
            }
            .setPositiveButton(R.string.action_open) { _, _ ->
                presenter.onConfirmOpenLocalFile(allow = true)
            }
            .setOnCancelListener { presenter.onConfirmOpenLocalFile(allow = false) }
            .resizeAndShow()
    }

    /**
     * @see BrowserContract.View.showFileChooser
     */
    fun showFileChooser(intent: Intent) {
        launcher.launch(intent)
    }

    /**
     * @see BrowserContract.View.showCustomView
     */
    fun showCustomView(view: View) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        binding.root.addView(view)
        customView = view
        setFullscreen(enabled = true, immersive = true)
    }

    /**
     * @see BrowserContract.View.hideCustomView
     */
    fun hideCustomView() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        customView?.let(binding.root::removeView)
        customView = null
        setFullscreen(enabled = false, immersive = false)
    }

    // TODO: update to use non deprecated flags
    private fun setFullscreen(enabled: Boolean, immersive: Boolean) {
        val window = window
        val decor = window.decorView
        if (enabled) {
            if (immersive) {
                decor.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            } else {
                decor.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            decor.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    private fun animateColorChange(color: Int) {
        if (!userPreferences.colorModeEnabled || userPreferences.useTheme != AppTheme.LIGHT || isIncognito()) {
            return
        }
        val shouldShowTabsInDrawer = userPreferences.showTabsInDrawer
        val adapter = tabsAdapter as? DesktopTabRecyclerViewAdapter
        val colorAnimator = ColorAnimator(defaultColor)
        binding.toolbar.startAnimation(colorAnimator.animateTo(
            color
        ) { mainColor, secondaryColor ->
            if (shouldShowTabsInDrawer) {
                backgroundDrawable.color = mainColor
                window.setBackgroundDrawable(backgroundDrawable)
            } else {
                adapter?.updateForegroundTabColor(mainColor)
            }
            binding.toolbar.setBackgroundColor(mainColor)
            binding.searchContainer.background?.tint(secondaryColor)
        })
    }

    private fun ImageView.updateVisibilityForDrawable() {
        visibility = if (drawable == null) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }
}
