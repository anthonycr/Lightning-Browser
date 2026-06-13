package acr.browser.lightning.browser

import acr.browser.lightning.R
import acr.browser.lightning.ThemableBrowserActivity
import acr.browser.lightning.browser.di.injector
import acr.browser.lightning.browser.keys.KeyEventAdapter
import acr.browser.lightning.browser.search.IntentExtractor
import acr.browser.lightning.browser.tab.TabPager
import acr.browser.lightning.browser.ui.TabConfiguration
import acr.browser.lightning.browser.view.targetUrl.LongPress
import acr.browser.lightning.compose.StateProvider
import acr.browser.lightning.constant.HTTP
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.HistoryEntry
import acr.browser.lightning.database.downloads.DownloadEntry
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.dialog.DialogItem
import acr.browser.lightning.dialog.LightningDialogBuilder
import acr.browser.lightning.extensions.color
import acr.browser.lightning.extensions.drawable
import acr.browser.lightning.extensions.resizeAndShow
import acr.browser.lightning.search.SuggestionsModel
import acr.browser.lightning.ssl.SslState
import acr.browser.lightning.utils.Option
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.widget.FrameLayout
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.collectAsState
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

/**
 * The base browser activity that governs the browsing experience for both default and incognito
 * browsers.
 */
abstract class BrowserActivity : ThemableBrowserActivity() {

    @Suppress("ConvertLambdaToReference")
    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { presenter.onFileChooserResult(it) }

    @Inject
    internal lateinit var keyEventAdapter: KeyEventAdapter

    @Inject
    internal lateinit var presenter: BrowserPresenter

    @Inject
    internal lateinit var tabPager: TabPager

    @Inject
    internal lateinit var intentExtractor: IntentExtractor

    @Inject
    internal lateinit var lightningDialogBuilder: LightningDialogBuilder

    @Named("tab")
    @Inject
    internal lateinit var tabConfigurationProvider: StateProvider<TabConfiguration>

    @Inject
    internal lateinit var suggestionsModel: SuggestionsModel

    /**
     * True if the activity is operating in incognito mode, false otherwise.
     */
    abstract fun isIncognito(): Boolean

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val browserFrame = FrameLayout(this)
        val customFrame = FrameLayout(this)
        injector.browser2ComponentBuilder()
            .activity(this)
            .browserFrame(browserFrame)
            .customFrame(customFrame)
            .initialIntent(intent.takeIf { savedInstanceState == null })
            .build()
            .inject(this)

        setContent {
            BrowserScreen(
                tabConfigurationProvider,
                state.collectAsState().value,
                presenter,
                browserFrame,
                customFrame,
                suggestionsModel
            )
        }

        presenter.onViewAttached(BrowserStateAdapter(this))

        tabPager.longPressListener = presenter::onPageLongPress

        onBackPressedDispatcher.addCallback {
            presenter.onNavigateBack()
        }
    }

    override fun onNewIntent(intent: Intent) {
        intentExtractor.extractUrlFromIntent(intent)?.let(presenter::onNewAction)
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

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return keyEventAdapter.adaptKeyEvent(event)?.let(presenter::onKeyComboClick)?.let { true }
            ?: super.onKeyUp(keyCode, event)
    }

    val state = MutableStateFlow(
        BrowserViewState(
            displayUrl = "",
            searchQuery = "",
            searchQuerySelection = Pair(0, 0),
            isRefresh = true,
            sslState = SslState.None,
            progress = 0,
            enableFullMenu = true,
            themeColor = Option.None,
            isForwardEnabled = false,
            isBackEnabled = false,
            bookmarks = emptyList(),
            isBookmarked = false,
            isBookmarkEnabled = true,
            isRootFolder = true,
            findInPage = null,
            tabs = emptyList(),
            tabCountText = "",
            isIncognito = isIncognito()
        )
    )

    /**
     * @see BrowserContract.View.renderState
     */
    fun renderState(viewState: BrowserViewState) {
        // TODO: Move into presenter
        lifecycleScope.launch {
            state.emit(viewState)
        }
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
        BrowserDialog.show(
            this, longPress.targetUrl?.replace(HTTP, ""),
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
        BrowserDialog.show(
            this, longPress.targetUrl?.replace(HTTP, ""),
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
        lifecycleScope.launch {
            state.emit(state.value.copy(openBookmarks = true))
        }
    }

    /**
     * @see BrowserContract.View.closeBookmarkDrawer
     */
    fun closeBookmarkDrawer() {
        lifecycleScope.launch {
            state.emit(state.value.copy(openBookmarks = false))
        }
    }

    /**
     * @see BrowserContract.View.openTabDrawer
     */
    fun openTabDrawer() {
        lifecycleScope.launch {
            state.emit(state.value.copy(openTabs = true))
        }
    }

    /**
     * @see BrowserContract.View.closeTabDrawer
     */
    fun closeTabDrawer() {
        lifecycleScope.launch {
            state.emit(state.value.copy(openTabs = false))
        }
    }

    /**
     * @see BrowserContract.View.showToolbar
     */
    fun showToolbar() {
        // TODO: Show toolbar
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
    fun showCustomView() {
        // TODO: Internalize state in presenter
        lifecycleScope.launch {
            state.emit(state.value.copy(showCustomView = true))
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        setFullscreen(enabled = true, immersive = true)
    }

    /**
     * @see BrowserContract.View.hideCustomView
     */
    fun hideCustomView() {
        // TODO: Internalize state in presenter
        lifecycleScope.launch {
            state.emit(state.value.copy(showCustomView = false))
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        setFullscreen(enabled = false, immersive = false)
    }

    private fun setFullscreen(enabled: Boolean, immersive: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            if (enabled) {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (immersive) {
                    hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    hide(WindowInsetsCompat.Type.statusBars())
                }
            } else {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // TODO: Animate color change
//    private fun animateColorChange(color: Int) {
//        if (!userPreferencesDataStore.colorModeEnabled.getUnsafe() || userPreferencesDataStore.useTheme.getUnsafe() != AppTheme.LIGHT || isIncognito()) {
//            return
//        }
//        val adapter = tabsAdapter as? DesktopTabRecyclerViewAdapter
//        val colorAnimator = ColorAnimator(defaultColor)
//        binding.toolbar.startAnimation(
//            colorAnimator.animateTo(
//                color
//            ) { mainColor, secondaryColor ->
//                if (userPreferencesDataStore.tabConfiguration.getUnsafe() != TabConfiguration.DESKTOP) {
//                    backgroundDrawable.color = mainColor
//                    window.setBackgroundDrawable(backgroundDrawable)
//                } else {
//                    adapter?.updateForegroundTabColor(mainColor)
//                }
//                binding.toolbar.setBackgroundColor(mainColor)
//                binding.searchContainer.background?.tint(secondaryColor)
//            })
//    }
}
