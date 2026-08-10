package acr.browser.lightning.browser

import acr.browser.lightning.BrowserUiEvent
import acr.browser.lightning.R
import acr.browser.lightning.adblock.allowlist.AllowListModel
import acr.browser.lightning.browser.history.HistoryRecord
import acr.browser.lightning.browser.keys.KeyCombo
import acr.browser.lightning.browser.menu.MenuSelection
import acr.browser.lightning.browser.notification.TabCountNotifier
import acr.browser.lightning.browser.search.SearchBoxModel
import acr.browser.lightning.browser.tab.DownloadPageInitializer
import acr.browser.lightning.browser.tab.HistoryPageInitializer
import acr.browser.lightning.browser.tab.HomePageInitializer
import acr.browser.lightning.browser.tab.NoOpInitializer
import acr.browser.lightning.browser.tab.TabInitializer
import acr.browser.lightning.browser.tab.TabModel
import acr.browser.lightning.browser.tab.TabViewState
import acr.browser.lightning.browser.tab.UrlInitializer
import acr.browser.lightning.browser.ui.TabConfiguration
import acr.browser.lightning.browser.view.LongPress
import acr.browser.lightning.concurrency.BrowserCoroutineScope
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.concurrency.combine
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.HistoryEntry
import acr.browser.lightning.database.SearchSuggestion
import acr.browser.lightning.database.WebPage
import acr.browser.lightning.database.asFolder
import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.database.downloads.DownloadEntry
import acr.browser.lightning.database.downloads.DownloadsRepository
import acr.browser.lightning.database.history.HistoryRepository
import acr.browser.lightning.di.BrowserScope
import acr.browser.lightning.di.IncognitoMode
import acr.browser.lightning.download.PendingDownload
import acr.browser.lightning.favicon.FaviconModel
import acr.browser.lightning.html.bookmark.BookmarkPageFactory
import acr.browser.lightning.html.history.HistoryPageFactory
import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.resources.NumberFormatter
import acr.browser.lightning.resources.ResourceProvider
import acr.browser.lightning.search.SearchEngineProvider
import acr.browser.lightning.search.engine.search
import acr.browser.lightning.ssl.SslState
import acr.browser.lightning.theme.ThemeProvider
import acr.browser.lightning.utils.isBookmarkUrl
import acr.browser.lightning.utils.isDownloadsUrl
import acr.browser.lightning.utils.isHistoryUrl
import acr.browser.lightning.utils.isSpecialUrl
import androidx.activity.result.ActivityResult
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.system.exitProcess

/**
 * The monolithic (oops) presenter that governs the behavior of the browser UI and interactions by
 * the user for both default and incognito browsers. This presenter should live for the entire
 * duration of the browser activity, which itself should not be recreated during configuration
 * changes.
 */
@BrowserScope
class BrowserPresenter @Inject constructor(
    private val model: BrowserContract.Model,
    private val navigator: BrowserContract.Navigator,
    private val bookmarkRepository: BookmarkRepository,
    private val downloadsRepository: DownloadsRepository,
    private val historyRepository: HistoryRepository,
    private val historyRecord: HistoryRecord,
    private val bookmarkPageFactory: BookmarkPageFactory,
    private val homePageInitializer: HomePageInitializer,
    private val historyPageInitializer: HistoryPageInitializer,
    private val downloadPageInitializer: DownloadPageInitializer,
    private val searchBoxModel: SearchBoxModel,
    private val searchEngineProvider: SearchEngineProvider,
    private val historyPageFactory: HistoryPageFactory,
    private val allowListModel: AllowListModel,
    private val tabCountNotifier: TabCountNotifier,
    @IncognitoMode private val incognitoMode: Boolean,
    coroutineDispatchers: CoroutineDispatchers,
    private val faviconModel: FaviconModel,
    private val resourceProvider: ResourceProvider,
    private val numberFormatter: NumberFormatter,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val themeProvider: ThemeProvider,
) {

    private val browserCoroutineScope = BrowserCoroutineScope(
        CoroutineScope(coroutineDispatchers.main + SupervisorJob())
    )

    private var view: BrowserContract.View? = null
    private var currentTab: TabModel? = null
    private var currentFolder: Bookmark.Folder = Bookmark.Folder.Root
    private var currentBookmarks: List<Bookmark> = emptyList()
    private var pendingAction: BrowserContract.Action.LoadUrl? = null
    private var pendingSnackbarAction: EphemeralAction? = null
    private var isCustomViewShowing = false

    private val tabJobs: MutableList<Job> = mutableListOf()
    private val allTabsJobMap: MutableMap<Int, Job> = mutableMapOf()

    /**
     * The current state of the browser UI.
     */
    val state: MutableStateFlow<BrowserViewState> = MutableStateFlow(
        BrowserViewState(
            displayUrl = "",
            searchQuery = "",
            searchQuerySelection = Pair(0, 0),
            isRefresh = true,
            sslState = SslState.None,
            progress = 0,
            enableFullMenu = true,
            themeColor = null,
            isSearchBarExpanded = false,
            isForwardEnabled = false,
            isBackEnabled = false,
            bookmarks = emptyList(),
            isBookmarked = false,
            isBookmarkEnabled = true,
            isRootFolder = true,
            findInPage = null,
            tabs = emptyList(),
            tabCountText = "",
            isIncognito = incognitoMode,
        )
    )

    init {
        browserCoroutineScope.launch {
            val bookmarks = async {
                bookmarkRepository.bookmarksAndFolders(folder = Bookmark.Folder.Root)
            }

            val tabs = model.initializeTabs()
            val lastTab = if (tabs.isEmpty()) {
                model.createTab(homePageInitializer)
            } else {
                tabs.last()
            }
            currentBookmarks = bookmarks.await()
            val bookmarkListItems = currentBookmarks.asListItems()
            state.updateSelf {
                updateTabViewState().copy(
                    bookmarks = bookmarkListItems,
                    isRootFolder = true
                )
            }
            selectTab(model.selectTab(lastTab.id))
        }

        browserCoroutineScope.launch {
            // Only react to changes, pages are initially loaded with the current theme.
            themeProvider.appThemeValues().drop(1).collectLatest {
                if (currentTab?.url?.isSpecialUrl() == true) {
                    reload()
                }
            }
        }

        browserCoroutineScope.launch {
            model.tabsListChanges().collectLatest { list ->
                list.subscribeToUpdates()

                tabCountNotifier.notifyTabCountChange(list.size)
            }
        }
    }

    /**
     * Call when the view is attached to the presenter.
     */
    fun onViewAttached(view: BrowserContract.View) {
        this.view = view
    }

    /**
     * Call when the view is detached from the presenter.
     */
    fun onViewDetached() {
        view = null

        tabJobs.forEach { it.cancel() }
        allTabsJobMap.values.forEach { it.cancel() }
        browserCoroutineScope.cancel()
    }

    /**
     * Call when the view is hidden (i.e. the browser is sent to the background).
     */
    fun onViewHidden() {
        model.markAllNonEphemeral()
        browserCoroutineScope.launch {
            model.freeze()
        }
    }

    /**
     * Call when a [browserUiEvent] is triggered by the user in the UI.
     */
    fun onEvent(browserUiEvent: BrowserUiEvent) {
        browserCoroutineScope.launch {
            when (browserUiEvent) {
                BrowserUiEvent.SnackbarActionPerformed -> onSnackbarActionPerformed()
                BrowserUiEvent.SnackbarDismissed -> onSnackbarDismissed()
                is BrowserUiEvent.FileChooserResult -> onFileChooserResult(browserUiEvent.activityResult)
                is BrowserUiEvent.ImageLongPress -> onImageLongPressEvent(
                    browserUiEvent.longPress,
                    browserUiEvent.imageLongPressEvent
                )

                is BrowserUiEvent.LinkLongPress -> onLinkLongPressEvent(
                    browserUiEvent.longPress,
                    browserUiEvent.linkLongPressEvent
                )

                is BrowserUiEvent.CloseBrowser -> onCloseBrowserEvent(
                    browserUiEvent.id,
                    browserUiEvent.closeTabEvent
                )

                is BrowserUiEvent.PageLongPress -> onPageLongPress(
                    browserUiEvent.id,
                    browserUiEvent.longPress
                )

                BrowserUiEvent.BookmarkMenuClick -> onBookmarkMenuClick()
                BrowserUiEvent.TabCountClick -> onTabCountViewClick()
                is BrowserUiEvent.HistoryOptionClick -> onHistoryOptionClick(
                    browserUiEvent.historyEntry,
                    browserUiEvent.option
                )

                is BrowserUiEvent.DownloadOptionClick -> onDownloadOptionClick(
                    browserUiEvent.downloadEntry,
                    browserUiEvent.optionClick
                )

                is BrowserUiEvent.FolderOptionClick -> onFolderOptionClick(
                    browserUiEvent.folder,
                    browserUiEvent.optionClick
                )

                is BrowserUiEvent.BookmarkOptionClick -> onBookmarkOptionClick(
                    browserUiEvent.bookmark,
                    browserUiEvent.optionClick
                )

                is BrowserUiEvent.BookmarkFolderRenameConfirmed -> onBookmarkFolderRenameConfirmed(
                    browserUiEvent.oldTitle,
                    browserUiEvent.newTitle
                )

                is BrowserUiEvent.BookmarkEditConfirmed -> onBookmarkEditConfirmed(
                    browserUiEvent.title,
                    browserUiEvent.url,
                    browserUiEvent.folder
                )

                is BrowserUiEvent.BookmarkConfirmed -> onBookmarkConfirmed(
                    browserUiEvent.title,
                    browserUiEvent.url,
                    browserUiEvent.folder
                )

                BrowserUiEvent.StarClick -> onStarClick()
                BrowserUiEvent.ToggleAdBlockingClick -> onToggleAdBlocking()
                BrowserUiEvent.ToggleDesktopAgentClick -> onToggleDesktopAgent()
                BrowserUiEvent.ToolsClick -> onToolsClick()
                is BrowserUiEvent.BookmarkLongClick -> onBookmarkLongClick(browserUiEvent.index)
                is BrowserUiEvent.BookmarkClick -> onBookmarkClick(browserUiEvent.index)
                BrowserUiEvent.SslIconClick -> onSslIconClick()
                BrowserUiEvent.DialogDismissed -> onDialogDismissed()
                is BrowserUiEvent.SearchSuggestionInsertClick -> onSearchSuggestionInsertClicked(
                    browserUiEvent.webPage
                )

                is BrowserUiEvent.SearchSuggestionClick -> onSearchSuggestionClicked(browserUiEvent.webPage)
                BrowserUiEvent.FindInPageDismissed -> onFindDismiss()
                BrowserUiEvent.FindInPagePrevious -> onFindPrevious()
                BrowserUiEvent.FindInPageNext -> onFindNext()
                is BrowserUiEvent.FindInPage -> onFindInPage(browserUiEvent.query)
                is BrowserUiEvent.SearchBarExpandedOrCollapsed -> onSearchBarExpandedOrCollapsed(
                    browserUiEvent.expanded
                )

                is BrowserUiEvent.SearchConfirmed -> onSearch(browserUiEvent.query)
                is BrowserUiEvent.SearchQueryChanged -> onSearchQueryChanged(
                    browserUiEvent.query,
                    browserUiEvent.selectionStart,
                    browserUiEvent.selectionEnd
                )

                BrowserUiEvent.RefreshOrStopClick -> onRefreshOrStopClick()
                BrowserUiEvent.NewTabClick -> onNewTabClick()
                BrowserUiEvent.HomeClick -> onHomeClick()
                BrowserUiEvent.ForwardClick -> onForwardClick()
                BrowserUiEvent.BackClick -> onBackClick()
                BrowserUiEvent.NavigateBack -> onNavigateBack()
                is BrowserUiEvent.BookmarkDrawerMoved -> onBookmarkDrawerMoved(browserUiEvent.isOpen)
                is BrowserUiEvent.TabDrawerMoved -> onTabDrawerMoved(browserUiEvent.isOpen)
                BrowserUiEvent.TabScroll -> onTabScroll()
                is BrowserUiEvent.TabClose -> onTabClose(browserUiEvent.index)
                is BrowserUiEvent.TabLongClick -> onTabLongClick(browserUiEvent.index)
                is BrowserUiEvent.TabClick -> onTabClick(browserUiEvent.index)
                is BrowserUiEvent.KeyComboClick -> onKeyComboClick(browserUiEvent.keyCombo)
                is BrowserUiEvent.MenuClick -> onMenuClick(browserUiEvent.menuSelection)
                is BrowserUiEvent.ConfirmOpenLocalFile -> onConfirmOpenLocalFile(browserUiEvent.allow)
                is BrowserUiEvent.NewAction -> onNewAction(browserUiEvent.action)
                BrowserUiEvent.TabMenuClick -> onTabMenuClick()
            }
        }
    }

    private fun BrowserViewState.updateTabViewState(): BrowserViewState {
        val selectedId = model.selectedTab?.id
        return copy(
            tabs = model.tabsList.map { it.asViewState(it.id == selectedId) },
            tabCountText = model.tabsList.size.asTabCountText(),
            isSearchBarExpanded = false,
        )
    }

    private fun TabModel.asViewState(selected: Boolean): TabViewState = TabViewState(
        id = id,
        icon = favicon,
        title = title,
        isSelected = selected,
        preview = preview
    )

    private fun List<TabViewState>.updateId(
        id: Int,
        map: (TabViewState) -> TabViewState
    ): List<TabViewState> = map {
        if (it.id == id) {
            map(it)
        } else {
            it
        }
    }

    private suspend fun selectTab(tabModel: TabModel?, focusTab: Boolean = true) {
        if (currentTab == tabModel) {
            state.updateSelf { copy(openTabs = false) }
            return
        }
        currentTab?.isForeground = false
        currentTab = tabModel
        currentTab?.isForeground = true

        val tab = tabModel ?: return run {
            val displayContent = searchBoxModel.getDisplayContent(
                url = "",
                title = null,
                isLoading = false
            )
            state.updateSelf {
                copy(
                    displayUrl = displayContent,
                    enableFullMenu = false,
                    isForwardEnabled = false,
                    isBackEnabled = false,
                    sslState = SslState.None,
                    progress = 100,
                    findInPage = null,
                    tabs = tabs.map { it.copy(isSelected = false) }
                )
            }
        }

        if (focusTab) {
            state.updateSelf { copy(openTabs = false) }
        }

        val updatedToolbarVisibility = toolbarVisibility(true)
        state.updateSelf {
            val updatedState = updateTabViewState()
            updatedState.copy(
                scrollToTab = updatedState.tabs.indexOfFirst { it.isSelected },
                toolbarVisibility = updatedToolbarVisibility
            )
        }

        tabJobs.forEach { it.cancel() }
        tabJobs.clear()

        tabJobs += combine(
            tab.sslChanges(),
            tab.titleChanges(),
            tab.urlChanges().onStart { emit(tab.url) },
            tab.loadingProgress().onStart { emit(tab.loadingProgress) },
            tab.canGoBackChanges().onStart { emit(tab.canGoBack()) },
            tab.canGoForwardChanges().onStart { emit(tab.canGoForward()) },
            tab.urlChanges().onStart { emit(tab.url) }.map { bookmarkRepository.isBookmark(it) },
            tab.urlChanges().onStart { emit(tab.url) }.map(String::isSpecialUrl),
            tab.themeColorChanges().onStart { emit(tab.themeColor) }
        ) { sslState, title, url, progress, canGoBack, canGoForward, isBookmark, isSpecialUrl, themeColor ->
            val displayContent = searchBoxModel.getDisplayContent(
                url = url,
                title = title,
                isLoading = progress < 100
            )
            state.updateSelf {
                copy(
                    displayUrl = displayContent,
                    searchQuery = tab.searchQuery,
                    searchQuerySelection = tab.searchQuerySelection,
                    enableFullMenu = !isSpecialUrl,
                    themeColor = themeColor,
                    isRefresh = progress == 100,
                    isForwardEnabled = canGoForward,
                    isBackEnabled = canGoBack,
                    sslState = sslState,
                    progress = progress,
                    isBookmarked = isBookmark,
                    isBookmarkEnabled = !isSpecialUrl,
                    findInPage = tab.findQuery
                )
            }
        }.launchIn(browserCoroutineScope)

        tabJobs += browserCoroutineScope.launch {
            tab.downloadRequests().collectLatest {
                navigator.download(it)
                showSnackbar(resourceProvider.stringResource(R.string.download_pending))
            }
        }

        tabJobs += browserCoroutineScope.launch {
            tab.urlChanges()
                .distinctUntilChanged()
                .collectLatest { url ->
                    url.takeIf { !it.isSpecialUrl() && it.isNotBlank() }?.let {
                        historyRecord.visit(tab.title, it)
                    }
                    val updatedToolbarVisibility = toolbarVisibility(true)
                    state.updateSelf { copy(toolbarVisibility = updatedToolbarVisibility) }
                }
        }

        tabJobs += browserCoroutineScope.launch {
            tab.createWindowRequests().collectLatest {
                createNewTabAndSelect(
                    tabInitializer = it,
                    shouldSelect = true,
                    tabType = TabModel.Type.POP_UP
                )
            }
        }

        tabJobs += browserCoroutineScope.launch {
            tab.showHideToolbar().collectLatest {
                val updatedToolbarVisibility = toolbarVisibility(it)
                state.updateSelf { copy(toolbarVisibility = updatedToolbarVisibility) }
            }
        }

        tabJobs += browserCoroutineScope.launch {
            tab.closeWindowRequests().collectLatest {
                onTabClose(state.value.tabs.indexOfCurrentTab())
            }
        }

        tabJobs += browserCoroutineScope.launch {
            tab.fileChooserRequests().collectLatest {
                view?.showFileChooser(it)
            }
        }

        tabJobs += browserCoroutineScope.launch {
            tab.showCustomViewRequests().collectLatest {
                state.updateSelf { copy(showCustomView = true) }
                isCustomViewShowing = true
            }
        }

        tabJobs += browserCoroutineScope.launch {
            tab.hideCustomViewRequests().collectLatest {
                state.updateSelf { copy(showCustomView = false) }
                isCustomViewShowing = false
            }
        }

        tabJobs += browserCoroutineScope.launch {
            tab.focusRequests().collectLatest {
                state.updateSelf { copy(openTabs = false) }
            }
        }
    }

    private fun List<TabModel>.subscribeToUpdates() {
        allTabsJobMap.keys
            .filter { id -> none { tabModel -> tabModel.id == id } }
            .forEach { id -> allTabsJobMap.remove(id)?.cancel() }
        forEach { tabModel ->
            if (allTabsJobMap[tabModel.id] == null) {
                allTabsJobMap[tabModel.id] = browserCoroutineScope.launch {
                    combine(
                        tabModel.titleChanges(),
                        tabModel.faviconChanges(),
                        tabModel.previewChanges()
                    ) { title, favicon, preview -> Triple(title, favicon, preview) }
                        .distinctUntilChanged()
                        .collectLatest { (title, favicon, preview) ->
                            state.updateSelf {
                                copy(
                                    tabs = tabs.updateId(tabModel.id) {
                                        it.copy(
                                            title = title,
                                            icon = favicon,
                                            preview = preview
                                        )
                                    }
                                )
                            }
                        }
                }
            }
        }
    }

    private suspend fun onNewAction(action: BrowserContract.Action) {
        when (action) {
            is BrowserContract.Action.LoadUrl -> if (action.url.isSpecialUrl()) {
                state.updateSelf {
                    copy(
                        dialog = BrowserViewState.Dialogs.LocalFileBlocked,
                        isSearchBarExpanded = false,
                    )
                }
                pendingAction = action
            } else {
                createNewTabAndSelect(
                    tabInitializer = UrlInitializer(action.url),
                    shouldSelect = true,
                    tabType = TabModel.Type.EPHEMERAL
                )
            }

            BrowserContract.Action.Panic -> panicClean()
            is BrowserContract.Action.Search -> onSearch(action.query)
        }
    }

    private suspend fun onConfirmOpenLocalFile(allow: Boolean) {
        onDialogDismissed()
        if (allow) {
            pendingAction?.let {
                createNewTabAndSelect(
                    tabInitializer = UrlInitializer(it.url),
                    shouldSelect = true,
                    tabType = TabModel.Type.EPHEMERAL
                )
            }
        }
        pendingAction = null
    }

    private suspend fun panicClean() {
        createNewTabAndSelect(tabInitializer = NoOpInitializer(), shouldSelect = true)

        model.clean()
        historyPageFactory.deleteHistoryPage()

        model.deleteAllTabs()
        state.updateSelf { updateTabViewState() }
        navigator.closeBrowser()

        // System exit needed in the case of receiving
        // the panic intent since finish() isn't completely
        // closing the browser
        exitProcess(1)
    }

    private suspend fun onMenuClick(menuSelection: MenuSelection) {
        when (menuSelection) {
            MenuSelection.NEW_TAB -> onNewTabClick()
            MenuSelection.NEW_INCOGNITO_TAB -> navigator.launchIncognito(url = null)
            MenuSelection.SHARE -> currentTab?.url?.takeIf { !it.isSpecialUrl() }?.let {
                navigator.sharePage(url = it, title = currentTab?.title)
            }

            MenuSelection.HISTORY -> createNewTabAndSelect(
                tabInitializer = historyPageInitializer,
                shouldSelect = true,
                tabType = TabModel.Type.POP_UP
            )

            MenuSelection.DOWNLOADS -> createNewTabAndSelect(
                tabInitializer = downloadPageInitializer,
                shouldSelect = true,
                tabType = TabModel.Type.POP_UP
            )

            MenuSelection.FIND -> {
                currentTab?.find("")
                state.updateSelf { copy(findInPage = "") }
            }

            MenuSelection.COPY_LINK -> {
                currentTab?.url?.takeIf { !it.isSpecialUrl() }
                    ?.let(navigator::copyPageLink)
                showSnackbar(resourceProvider.stringResource(R.string.message_link_copied))
            }

            MenuSelection.ADD_TO_HOME -> currentTab?.url?.takeIf { !it.isSpecialUrl() }
                ?.let { addToHomeScreen() }

            MenuSelection.BOOKMARKS -> state.updateSelf { copy(openBookmarks = true) }
            MenuSelection.ADD_BOOKMARK -> currentTab?.url?.takeIf { !it.isSpecialUrl() }
                ?.let { showAddBookmarkDialog() }

            MenuSelection.SETTINGS -> navigator.openSettings()
            MenuSelection.BACK -> onBackClick()
            MenuSelection.FORWARD -> onForwardClick()
        }
    }

    private suspend fun addToHomeScreen() {
        currentTab?.let {
            val result = navigator.addToHomeScreen(
                url = it.url,
                title = it.title,
                favicon = (it.favicon as? TabModel.Favicon.Icon)?.bitmap?.asAndroidBitmap()
            )
            if (result) {
                showSnackbar(resourceProvider.stringResource(R.string.message_added_to_homescreen))
            } else {
                showSnackbar(resourceProvider.stringResource(R.string.shortcut_message_failed_to_add))
            }
        }
    }

    private suspend fun createNewTabAndSelect(
        tabInitializer: TabInitializer,
        shouldSelect: Boolean,
        tabType: TabModel.Type = TabModel.Type.NORMAL
    ) {
        val tab = model.createTab(tabInitializer, tabType = tabType)
        state.updateSelf { updateTabViewState() }
        if (shouldSelect) {
            selectTab(model.selectTab(tab.id))
        } else {
            showSnackbar(
                message = resourceProvider.stringResource(R.string.result_open_background_tab),
                action = EphemeralAction(resourceProvider.stringResource(R.string.action_open)) {
                    selectTab(model.selectTab(tab.id))
                }
            )
        }
    }

    private fun List<TabViewState>.tabIndexForId(id: Int?): Int =
        indexOfFirst { it.id == id }

    private fun List<TabViewState>.indexOfCurrentTab(): Int = tabIndexForId(currentTab?.id)

    private suspend fun onKeyComboClick(keyCombo: KeyCombo) {
        when (keyCombo) {
            KeyCombo.CTRL_F -> {
                currentTab?.find("")
                state.updateSelf { copy(findInPage = "") }
            }

            KeyCombo.CTRL_T -> onNewTabClick()
            KeyCombo.CTRL_W -> onTabClose(state.value.tabs.indexOfCurrentTab())
            KeyCombo.CTRL_Q -> state.updateSelf {
                copy(dialog = BrowserViewState.Dialogs.CloseBrowser(tabs.indexOfCurrentTab()))
            }

            KeyCombo.CTRL_R -> onRefreshOrStopClick()

            KeyCombo.CTRL_TAB -> {
                val currentIndex = state.value.tabs.indexOfCurrentTab()
                val nextIndex =
                    if (currentIndex + 1 < state.value.tabs.size) currentIndex + 1 else 0
                onTabClick(nextIndex)
            }

            KeyCombo.CTRL_SHIFT_TAB -> {
                val currentIndex = state.value.tabs.indexOfCurrentTab()
                val previousIndex = if (currentIndex - 1 >= 0) {
                    currentIndex - 1
                } else {
                    state.value.tabs.lastIndex
                }
                onTabClick(previousIndex)
            }

            KeyCombo.SEARCH -> currentTab?.searchQuery?.let { onSearch(it) }
            KeyCombo.ALT_0 -> onTabClick(0.coerceAtMost(state.value.tabs.lastIndex))
            KeyCombo.ALT_1 -> onTabClick(1.coerceAtMost(state.value.tabs.lastIndex))
            KeyCombo.ALT_2 -> onTabClick(2.coerceAtMost(state.value.tabs.lastIndex))
            KeyCombo.ALT_3 -> onTabClick(3.coerceAtMost(state.value.tabs.lastIndex))
            KeyCombo.ALT_4 -> onTabClick(4.coerceAtMost(state.value.tabs.lastIndex))
            KeyCombo.ALT_5 -> onTabClick(5.coerceAtMost(state.value.tabs.lastIndex))
            KeyCombo.ALT_6 -> onTabClick(6.coerceAtMost(state.value.tabs.lastIndex))
            KeyCombo.ALT_7 -> onTabClick(7.coerceAtMost(state.value.tabs.lastIndex))
            KeyCombo.ALT_8 -> onTabClick(8.coerceAtMost(state.value.tabs.lastIndex))
            KeyCombo.ALT_9 -> onTabClick(9.coerceAtMost(state.value.tabs.lastIndex))
        }
    }

    private suspend fun onTabClick(index: Int) {
        selectTab(model.selectTab(state.value.tabs[index].id))
    }

    private suspend fun onTabLongClick(index: Int) {
        state.updateSelf { copy(dialog = BrowserViewState.Dialogs.CloseBrowser(tabs[index].id)) }
    }

    private fun <T> List<T>.nextSelected(removedIndex: Int): T? {
        val nextIndex = when {
            size > removedIndex + 1 -> removedIndex + 1
            removedIndex > 0 -> removedIndex - 1
            else -> -1
        }
        return if (nextIndex >= 0) {
            this[nextIndex]
        } else {
            null
        }
    }

    private suspend fun onTabClose(index: Int) {
        if (index == -1) {
            // If the user clicks on close multiple times, the index may be -1 if the view is in the
            // process of being removed.
            return
        }
        val nextTab = state.value.tabs.nextSelected(index)

        val currentTabId = currentTab?.id
        val needToSelectNextTab = state.value.tabs[index].id == currentTabId

        model.deleteTab(state.value.tabs[index].id)
        state.updateSelf { updateTabViewState() }
        if (needToSelectNextTab) {
            nextTab?.id?.let {
                val shouldClose = currentTab?.tabType == TabModel.Type.EPHEMERAL
                selectTab(model.selectTab(it), focusTab = false)
                if (shouldClose) {
                    navigator.backgroundBrowser()
                } else {
                    showSnackbar(
                        message = resourceProvider.stringResource(R.string.message_reopen),
                        action = EphemeralAction(resourceProvider.stringResource(R.string.action_reopen)) {
                            reopenTab()
                        }
                    )
                }
            } ?: run {
                selectTab(tabModel = null)
                navigator.closeBrowser()
            }
        }
    }

    private suspend fun onTabScroll() {
        state.updateSelf { copy(scrollToTab = -1) }
    }

    private suspend fun onTabDrawerMoved(isOpen: Boolean) {
        state.updateSelf { copy(openTabs = isOpen) }
    }

    private suspend fun onBookmarkDrawerMoved(isOpen: Boolean) {
        state.updateSelf { copy(openBookmarks = isOpen) }
    }

    private suspend fun onNavigateBack() {
        when {
            isCustomViewShowing -> {
                state.updateSelf { copy(showCustomView = false) }
                currentTab?.hideCustomView()
            }

            state.value.openTabs -> state.updateSelf { copy(openTabs = false) }
            state.value.openBookmarks -> if (currentFolder != Bookmark.Folder.Root) {
                onBookmarkMenuClick()
            } else {
                state.updateSelf { copy(openBookmarks = false) }
            }

            currentTab?.canGoBack() == true -> currentTab?.goBack()
            currentTab?.canGoBack() == false -> if (incognitoMode) {
                currentTab?.id?.let {
                    state.updateSelf { copy(dialog = BrowserViewState.Dialogs.CloseBrowser(it)) }
                }
            } else if (currentTab?.tabType in listOf(
                    TabModel.Type.EPHEMERAL,
                    TabModel.Type.POP_UP
                )
            ) {
                onTabClose(state.value.tabs.indexOfCurrentTab())
            } else {
                navigator.backgroundBrowser()
            }
        }
    }

    private fun onBackClick() {
        if (currentTab?.canGoBack() == true) {
            currentTab?.goBack()
        }
    }

    private fun onForwardClick() {
        if (currentTab?.canGoForward() == true) {
            currentTab?.goForward()
        }
    }

    private fun onHomeClick() {
        currentTab?.loadFromInitializer(homePageInitializer)
    }

    private suspend fun onNewTabClick() {
        createNewTabAndSelect(homePageInitializer, shouldSelect = true)
    }

    private suspend fun onRefreshOrStopClick() {
        if (currentTab?.loadingProgress != 100) {
            currentTab?.stopLoading()
        } else {
            reload()
        }
    }

    private suspend fun reload() {
        val currentUrl = currentTab?.url
        if (currentUrl?.isSpecialUrl() == true) {
            when {
                currentUrl.isBookmarkUrl() -> {
                    bookmarkPageFactory.buildPage()
                    currentTab?.reload()
                }

                currentUrl.isDownloadsUrl() ->
                    currentTab?.loadFromInitializer(downloadPageInitializer)

                currentUrl.isHistoryUrl() ->
                    currentTab?.loadFromInitializer(historyPageInitializer)

                else -> currentTab?.reload()
            }
        } else {
            currentTab?.reload()
        }
    }

    private suspend fun onSearchQueryChanged(
        query: String,
        selectionStart: Int,
        selectionEnd: Int
    ) {
        currentTab?.searchQuery = query
        currentTab?.searchQuerySelection = Pair(selectionStart, selectionEnd)
        state.updateSelf {
            copy(
                searchQuery = query,
                searchQuerySelection = Pair(selectionStart, selectionEnd)
            )
        }
    }

    private suspend fun onSearch(query: String) {
        if (query.isEmpty()) {
            return
        }
        currentTab?.stopLoading()
        val url = searchEngineProvider.provideSearchEngine().search(query)
        val displayContent = searchBoxModel.getDisplayContent(
            url = url,
            title = currentTab?.title,
            isLoading = (currentTab?.loadingProgress ?: 0) < 100
        )
        state.updateSelf {
            copy(displayUrl = displayContent)
        }
        currentTab?.loadUrl(url)
    }

    private suspend fun onSearchBarExpandedOrCollapsed(expanded: Boolean) {
        state.updateSelf { copy(isSearchBarExpanded = expanded) }
    }

    private suspend fun onFindInPage(query: String) {
        currentTab?.find(query)
        state.updateSelf { copy(findInPage = query) }
    }

    private fun onFindNext() {
        currentTab?.findNext()
    }

    private fun onFindPrevious() {
        currentTab?.findPrevious()
    }

    private suspend fun onFindDismiss() {
        currentTab?.clearFindMatches()
        state.updateSelf { copy(findInPage = null) }
    }

    private suspend fun onSearchSuggestionClicked(webPage: WebPage) {
        val url = when (webPage) {
            is HistoryEntry,
            is Bookmark.Entry -> webPage.url

            is SearchSuggestion -> webPage.title
            else -> null
        } ?: error("Other types cannot be search suggestions: $webPage")

        onSearch(url)
    }

    private suspend fun onSearchSuggestionInsertClicked(webPage: WebPage) {
        val url = when (webPage) {
            is HistoryEntry,
            is Bookmark.Entry -> webPage.url

            is SearchSuggestion -> webPage.title
            else -> null
        } ?: error("Other types cannot be search suggestions: $webPage")

        onSearchQueryChanged(url, url.length, url.length)
    }

    private suspend fun onDialogDismissed() {
        state.updateSelf { copy(dialog = null) }
    }

    private suspend fun onSslIconClick() {
        currentTab?.sslCertificateInfo?.let {
            state.updateSelf { copy(dialog = BrowserViewState.Dialogs.SslInfo(it)) }
        }
    }

    private suspend fun onBookmarkClick(index: Int) {
        when (val bookmark = currentBookmarks[index]) {
            is Bookmark.Entry -> {
                currentTab?.loadUrl(bookmark.url)
                state.updateSelf { copy(openBookmarks = false) }
            }

            Bookmark.Folder.Root -> error("Cannot click on root folder")
            is Bookmark.Folder.Entry -> {
                currentFolder = bookmark
                val bookmarks = bookmarkRepository.bookmarksAndFolders(folder = bookmark)
                currentBookmarks = bookmarks
                val bookmarkListItems = bookmarks.asListItems()
                state.updateSelf {
                    copy(bookmarks = bookmarkListItems, isRootFolder = false)
                }
            }
        }
    }

    private suspend fun BookmarkRepository.bookmarksAndFolders(folder: Bookmark.Folder): List<Bookmark> {
        val bookmarks = getBookmarksFromFolderSorted(folder = folder.title)
        return if (folder == Bookmark.Folder.Root) {
            bookmarks + getFoldersSorted()
        } else {
            bookmarks
        }
    }

    private suspend fun List<Bookmark>.asListItems(): List<BrowserViewState.BookmarkListItem> {
        return map {
            when (it) {
                is Bookmark.Entry -> BrowserViewState.BookmarkListItem(
                    title = it.title,
                    icon = BrowserViewState.BookmarkListItem.Icon.Image(
                        faviconModel.getFaviconPathForUrl(it.url)
                    )
                )

                is Bookmark.Folder -> BrowserViewState.BookmarkListItem(
                    title = it.title,
                    icon = BrowserViewState.BookmarkListItem.Icon.Folder
                )
            }
        }
    }

    private suspend fun onBookmarkLongClick(index: Int) {
        when (val item = currentBookmarks[index]) {
            is Bookmark.Entry -> state.updateSelf {
                copy(dialog = BrowserViewState.Dialogs.BookmarkOptions(item))
            }

            is Bookmark.Folder.Entry -> state.updateSelf {
                copy(dialog = BrowserViewState.Dialogs.FolderOptions(item))
            }

            Bookmark.Folder.Root -> Unit // Root is not clickable
        }
    }

    private suspend fun onToolsClick() {
        val currentUrl = currentTab?.url ?: return
        state.updateSelf {
            copy(
                dialog = BrowserViewState.Dialogs.PageTools(
                    areAdsAllowed = allowListModel.isUrlAllowedAds(currentUrl),
                    shouldShowAdBlockOption = !currentUrl.isSpecialUrl()
                )
            )
        }
    }

    private suspend fun onToggleDesktopAgent() {
        onDialogDismissed()
        currentTab?.toggleDesktopAgent()
        currentTab?.reload()
    }

    private suspend fun onToggleAdBlocking() {
        onDialogDismissed()
        val currentUrl = currentTab?.url ?: return
        if (allowListModel.isUrlAllowedAds(currentUrl)) {
            allowListModel.removeUrlFromAllowList(currentUrl)
        } else {
            allowListModel.addUrlToAllowList(currentUrl)
        }
        currentTab?.reload()
    }

    private suspend fun onStarClick() {
        val url = currentTab?.url ?: return
        val title = currentTab?.title.orEmpty()
        if (url.isSpecialUrl()) {
            return
        }
        val isBookmark = bookmarkRepository.isBookmark(url)
        if (isBookmark) {
            bookmarkRepository.deleteBookmark(
                Bookmark.Entry(
                    url = url,
                    title = title,
                    position = 0,
                    folder = Bookmark.Folder.Root
                )
            )
            val bookmarks = bookmarkRepository.bookmarksAndFolders(folder = currentFolder)
            currentBookmarks = bookmarks
            val bookmarkListItems = bookmarks.asListItems()
            val isBookmark = bookmarkRepository.isBookmark(url)
            state.updateSelf {
                copy(
                    bookmarks = bookmarkListItems,
                    isBookmarked = isBookmark
                )
            }
        } else {
            showAddBookmarkDialog()
        }
    }

    private suspend fun showAddBookmarkDialog() {
        val folders = bookmarkRepository.getFolderNames()
        val existing = bookmarkRepository.findBookmarkForUrl(currentTab?.url.orEmpty())
        state.updateSelf {
            if (existing != null) {
                copy(
                    dialog = BrowserViewState.Dialogs.EditBookmark(
                        title = existing.title,
                        url = existing.url,
                        folder = existing.folder.title,
                        folders = folders
                    )
                )
            } else {
                copy(
                    dialog = BrowserViewState.Dialogs.AddBookmark(
                        title = currentTab?.title.orEmpty(),
                        url = currentTab?.url.orEmpty(),
                        folders = folders
                    )
                )
            }
        }
    }

    private suspend fun onBookmarkConfirmed(title: String, url: String, folder: String) {
        onDialogDismissed()
        bookmarkRepository.addBookmarkIfNotExists(
            Bookmark.Entry(
                url = url,
                title = title,
                position = 0,
                folder = folder.asFolder()
            )
        )
        val bookmarks = bookmarkRepository.bookmarksAndFolders(folder = currentFolder)
        currentBookmarks = bookmarks
        val bookmarkListItems = bookmarks.asListItems()
        val isBookmark = bookmarkRepository.isBookmark(url)
        state.updateSelf {
            copy(bookmarks = bookmarkListItems, isBookmarked = isBookmark)
        }
    }

    private suspend fun onBookmarkEditConfirmed(title: String, url: String, folder: String) {
        val oldUrl = (state.value.dialog as? BrowserViewState.Dialogs.EditBookmark)?.url ?: return
        onDialogDismissed()
        bookmarkRepository.editBookmark(
            oldBookmark = Bookmark.Entry(
                url = oldUrl,
                title = "",
                position = 0,
                folder = Bookmark.Folder.Root
            ),
            newBookmark = Bookmark.Entry(
                url = url,
                title = title,
                position = 0,
                folder = folder.asFolder()
            )
        )
        val bookmarks = bookmarkRepository.bookmarksAndFolders(folder = currentFolder)
        currentBookmarks = bookmarks
        val isBookmarked = currentTab?.url?.let { bookmarkRepository.isBookmark(it) } ?: false
        val bookmarkListItems = bookmarks.asListItems()
        state.updateSelf {
            copy(bookmarks = bookmarkListItems, isBookmarked = isBookmarked)
        }
        if (currentTab?.url?.isBookmarkUrl() == true) {
            reload()
        }
    }

    private suspend fun onBookmarkFolderRenameConfirmed(oldTitle: String, newTitle: String) {
        onDialogDismissed()
        bookmarkRepository.renameFolder(oldTitle, newTitle)
        val bookmarks = bookmarkRepository.bookmarksAndFolders(folder = currentFolder)
        currentBookmarks = bookmarks
        val bookmarkListItems = bookmarks.asListItems()
        state.updateSelf { copy(bookmarks = bookmarkListItems) }
        if (currentTab?.url?.isBookmarkUrl() == true) {
            reload()
        }
    }

    private suspend fun onBookmarkOptionClick(
        bookmark: Bookmark.Entry,
        option: BrowserContract.BookmarkOptionEvent
    ) {
        when (option) {
            BrowserContract.BookmarkOptionEvent.NEW_TAB ->
                createNewTabAndSelect(UrlInitializer(bookmark.url), shouldSelect = true)

            BrowserContract.BookmarkOptionEvent.BACKGROUND_TAB ->
                createNewTabAndSelect(UrlInitializer(bookmark.url), shouldSelect = false)

            BrowserContract.BookmarkOptionEvent.INCOGNITO_TAB -> navigator.launchIncognito(bookmark.url)
            BrowserContract.BookmarkOptionEvent.SHARE ->
                navigator.sharePage(url = bookmark.url, title = bookmark.title)

            BrowserContract.BookmarkOptionEvent.COPY_LINK -> {
                navigator.copyPageLink(bookmark.url)
                showSnackbar(resourceProvider.stringResource(R.string.message_link_copied))
            }

            BrowserContract.BookmarkOptionEvent.REMOVE -> {
                bookmarkRepository.deleteBookmark(bookmark)
                val bookmarks = bookmarkRepository.bookmarksAndFolders(folder = currentFolder)
                currentBookmarks = bookmarks
                val bookmarkListItems = bookmarks.asListItems()
                state.updateSelf { copy(bookmarks = bookmarkListItems) }
                if (currentTab?.url?.isBookmarkUrl() == true) {
                    reload()
                }
            }

            BrowserContract.BookmarkOptionEvent.EDIT -> {
                val folders = bookmarkRepository.getFolderNames()
                state.updateSelf {
                    copy(
                        dialog = BrowserViewState.Dialogs.EditBookmark(
                            title = bookmark.title,
                            url = bookmark.url,
                            folder = bookmark.folder.title,
                            folders = folders
                        )
                    )
                }
            }
        }
        onDialogDismissed()
    }

    private suspend fun onFolderOptionClick(
        folder: Bookmark.Folder,
        option: BrowserContract.FolderOptionEvent
    ) {
        onDialogDismissed()
        when (option) {
            BrowserContract.FolderOptionEvent.RENAME -> state.updateSelf {
                copy(dialog = BrowserViewState.Dialogs.EditFolder(title = folder.title))
            }

            BrowserContract.FolderOptionEvent.REMOVE -> {
                bookmarkRepository.deleteFolder(folder.title)
                val bookmarks = bookmarkRepository.bookmarksAndFolders(folder = currentFolder)
                currentBookmarks = bookmarks
                val bookmarkListItems = bookmarks.asListItems()
                state.updateSelf { copy(bookmarks = bookmarkListItems) }
                if (currentTab?.url?.isBookmarkUrl() == true) {
                    reload()
                    currentTab?.goBack()
                }
            }
        }
    }

    private suspend fun onDownloadOptionClick(
        download: DownloadEntry,
        option: BrowserContract.DownloadOptionEvent
    ) {
        when (option) {
            BrowserContract.DownloadOptionEvent.DELETE -> {
                downloadsRepository.deleteDownload(download.location)
                if (currentTab?.url?.isDownloadsUrl() == true) {
                    reload()
                }
            }

            BrowserContract.DownloadOptionEvent.DELETE_ALL -> {
                downloadsRepository.deleteAllDownloads()
                if (currentTab?.url?.isDownloadsUrl() == true) {
                    reload()
                }
            }
        }
        onDialogDismissed()
    }

    private suspend fun onHistoryOptionClick(
        historyEntry: HistoryEntry,
        option: BrowserContract.HistoryOptionEvent
    ) {
        when (option) {
            BrowserContract.HistoryOptionEvent.NEW_TAB ->
                createNewTabAndSelect(UrlInitializer(historyEntry.url), shouldSelect = true)

            BrowserContract.HistoryOptionEvent.BACKGROUND_TAB ->
                createNewTabAndSelect(UrlInitializer(historyEntry.url), shouldSelect = false)

            BrowserContract.HistoryOptionEvent.INCOGNITO_TAB ->
                navigator.launchIncognito(historyEntry.url)

            BrowserContract.HistoryOptionEvent.SHARE ->
                navigator.sharePage(url = historyEntry.url, title = historyEntry.title)

            BrowserContract.HistoryOptionEvent.COPY_LINK -> {
                navigator.copyPageLink(historyEntry.url)
                showSnackbar(resourceProvider.stringResource(R.string.message_link_copied))
            }

            BrowserContract.HistoryOptionEvent.REMOVE -> {
                historyRepository.deleteHistoryEntry(historyEntry.url)
                if (currentTab?.url?.isHistoryUrl() == true) {
                    reload()
                }
            }
        }
        onDialogDismissed()
    }

    private suspend fun onTabCountViewClick() {
        when (userPreferencesDataStore.tabConfiguration.get()) {
            TabConfiguration.DRAWER_SIDE -> state.updateSelf { copy(openTabs = true) }

            TabConfiguration.DRAWER_BOTTOM -> state.updateSelf { copy(openTabs = !openTabs) }

            else -> currentTab?.loadFromInitializer(homePageInitializer)
        }
    }

    private suspend fun onTabMenuClick() {
        currentTab?.let {
            state.updateSelf { copy(dialog = BrowserViewState.Dialogs.CloseBrowser(it.id)) }
        }
    }

    private suspend fun onBookmarkMenuClick() {
        if (currentFolder != Bookmark.Folder.Root) {
            currentFolder = Bookmark.Folder.Root
            val bookmarks = bookmarkRepository.bookmarksAndFolders(folder = Bookmark.Folder.Root)
            currentBookmarks = bookmarks
            val bookmarkListItems = bookmarks.asListItems()
            state.updateSelf { copy(bookmarks = bookmarkListItems, isRootFolder = true) }
        }
    }

    private suspend fun onPageLongPress(id: Int, longPress: LongPress) {
        val pageUrl = model.tabsList.find { it.id == id }?.url
        if (pageUrl?.isSpecialUrl() == true) {
            val url = longPress.targetUrl ?: return
            if (pageUrl.isBookmarkUrl()) {
                if (url.isBookmarkUrl()) {
                    val filename = requireNotNull(longPress.targetUrl.toUri().lastPathSegment) {
                        "Last segment should always exist for bookmark file"
                    }
                    val folderTitle = filename.substring(
                        0,
                        filename.length - BookmarkPageFactory.FILENAME.length - 1
                    )
                    state.updateSelf {
                        copy(dialog = BrowserViewState.Dialogs.FolderOptions(folderTitle.asFolder()))
                    }
                } else {
                    val bookmark = bookmarkRepository.findBookmarkForUrl(url)
                    if (bookmark != null) {
                        state.updateSelf {
                            copy(dialog = BrowserViewState.Dialogs.BookmarkOptions(bookmark))
                        }
                    }
                }
            } else if (pageUrl.isDownloadsUrl()) {
                val download = downloadsRepository.findDownloadForUrl(url)
                if (download != null) {
                    state.updateSelf {
                        copy(dialog = BrowserViewState.Dialogs.DownloadOptions(download))
                    }
                }
            } else if (pageUrl.isHistoryUrl()) {
                val entries = historyRepository.findHistoryEntriesContaining(url)
                state.updateSelf {
                    copy(
                        dialog = BrowserViewState.Dialogs.HistoryOptions(
                            entries.firstOrNull()
                                ?: HistoryEntry(url = url, title = "")
                        )
                    )
                }
            }
        } else {
            when (longPress.hitCategory) {
                LongPress.Category.IMAGE -> state.updateSelf {
                    copy(dialog = BrowserViewState.Dialogs.ImageLongPress(longPress))
                }

                LongPress.Category.LINK -> state.updateSelf {
                    copy(dialog = BrowserViewState.Dialogs.LinkLongPress(longPress))
                }

                LongPress.Category.UNKNOWN -> Unit // Do nothing
            }
        }
    }

    private suspend fun onCloseBrowserEvent(id: Int, closeTabEvent: BrowserContract.CloseTabEvent) {
        when (closeTabEvent) {
            BrowserContract.CloseTabEvent.CLOSE_CURRENT ->
                onTabClose(state.value.tabs.tabIndexForId(id))

            BrowserContract.CloseTabEvent.CLOSE_OTHERS -> {
                val currentTabId = currentTab?.id
                model.tabsList.filter { it.id != id }.forEach {
                    model.deleteTab(it.id)
                    state.updateSelf { updateTabViewState() }
                    if (currentTabId != id) {
                        selectTab(model.selectTab(id))
                    }
                }
            }

            BrowserContract.CloseTabEvent.CLOSE_ALL -> {
                model.deleteAllTabs()
                state.updateSelf { updateTabViewState() }
                navigator.closeBrowser()
            }
        }
        onDialogDismissed()
    }

    private suspend fun onLinkLongPressEvent(
        longPress: LongPress,
        linkLongPressEvent: BrowserContract.LinkLongPressEvent
    ) {
        when (linkLongPressEvent) {
            BrowserContract.LinkLongPressEvent.NEW_TAB ->
                longPress.targetUrl?.let {
                    createNewTabAndSelect(
                        UrlInitializer(it),
                        shouldSelect = true
                    )
                }

            BrowserContract.LinkLongPressEvent.BACKGROUND_TAB ->
                longPress.targetUrl?.let {
                    createNewTabAndSelect(
                        UrlInitializer(it),
                        shouldSelect = false
                    )
                }

            BrowserContract.LinkLongPressEvent.INCOGNITO_TAB -> longPress.targetUrl?.let(navigator::launchIncognito)
            BrowserContract.LinkLongPressEvent.SHARE ->
                longPress.targetUrl?.let { navigator.sharePage(url = it, title = null) }

            BrowserContract.LinkLongPressEvent.COPY_LINK -> {
                longPress.targetUrl?.let(navigator::copyPageLink)
                showSnackbar(resourceProvider.stringResource(R.string.message_link_copied))
            }
        }
        onDialogDismissed()
    }

    private suspend fun onImageLongPressEvent(
        longPress: LongPress,
        imageLongPressEvent: BrowserContract.ImageLongPressEvent
    ) {
        when (imageLongPressEvent) {
            BrowserContract.ImageLongPressEvent.NEW_TAB ->
                longPress.targetUrl?.let {
                    createNewTabAndSelect(
                        UrlInitializer(it),
                        shouldSelect = true
                    )
                }

            BrowserContract.ImageLongPressEvent.BACKGROUND_TAB ->
                longPress.targetUrl?.let {
                    createNewTabAndSelect(
                        UrlInitializer(it),
                        shouldSelect = false
                    )
                }

            BrowserContract.ImageLongPressEvent.INCOGNITO_TAB -> longPress.targetUrl?.let(navigator::launchIncognito)
            BrowserContract.ImageLongPressEvent.SHARE ->
                longPress.targetUrl?.let { navigator.sharePage(url = it, title = null) }

            BrowserContract.ImageLongPressEvent.COPY_LINK -> {
                longPress.targetUrl?.let(navigator::copyPageLink)
                showSnackbar(resourceProvider.stringResource(R.string.message_link_copied))
            }

            BrowserContract.ImageLongPressEvent.DOWNLOAD -> {
                navigator.download(
                    PendingDownload(
                        url = longPress.hitUrl.orEmpty(),
                        userAgent = null,
                        contentDisposition = "attachment",
                        mimeType = null,
                        contentLength = 0
                    )
                )
                showSnackbar(resourceProvider.stringResource(R.string.download_pending))
            }
        }
        onDialogDismissed()
    }

    private fun onFileChooserResult(activityResult: ActivityResult) {
        currentTab?.handleFileChooserResult(activityResult)
    }

    private suspend fun onSnackbarDismissed() {
        hideSnackbar()
        pendingSnackbarAction = null
    }

    private suspend fun onSnackbarActionPerformed() {
        hideSnackbar()
        pendingSnackbarAction?.action()
        pendingSnackbarAction = null
    }

    private fun Int.asTabCountText(): String = if (this > 99) {
        resourceProvider.stringResource(R.string.infinity)
    } else {
        numberFormatter.formatNumber(this)
    }

    private suspend fun reopenTab() {
        val tab = model.reopenTab()
        state.updateSelf { updateTabViewState() }
        if (tab != null) {
            selectTab(model.selectTab(tab.id))
        }
    }

    private suspend fun toolbarVisibility(show: Boolean): BrowserViewState.ToolbarVisibility =
        if (!userPreferencesDataStore.fullScreenEnabled.get()) {
            BrowserViewState.ToolbarVisibility.FIXED
        } else if (show) {
            BrowserViewState.ToolbarVisibility.SHOW
        } else {
            BrowserViewState.ToolbarVisibility.HIDE
        }

    private suspend fun hideSnackbar() {
        state.updateSelf { copy(ephemeral = null) }
    }

    private suspend fun showSnackbar(message: String, action: EphemeralAction? = null) {
        pendingSnackbarAction = action
        state.updateSelf {
            copy(
                ephemeral = BrowserViewState.Ephemeral(
                    message = message,
                    actionLabel = action?.label
                )
            )
        }
    }

    private suspend fun <T> MutableStateFlow<T>.updateSelf(function: T.() -> T) {
        emit(value.function())
    }

    private class EphemeralAction(
        val label: String,
        val action: suspend () -> Unit,
    )
}
