package acr.browser.lightning.browser

import acr.browser.lightning.adblock.allowlist.AllowListModel
import acr.browser.lightning.browser.data.CookieAdministrator
import acr.browser.lightning.browser.di.Browser2Scope
import acr.browser.lightning.browser.di.IncognitoMode
import acr.browser.lightning.browser.download.PendingDownload
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
import acr.browser.lightning.browser.ui.UiConfiguration
import acr.browser.lightning.browser.view.targetUrl.LongPress
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
import acr.browser.lightning.html.bookmark.BookmarkPageFactory
import acr.browser.lightning.html.history.HistoryPageFactory
import acr.browser.lightning.search.SearchEngineProvider
import acr.browser.lightning.ssl.SslState
import acr.browser.lightning.utils.Option
import acr.browser.lightning.utils.QUERY_PLACE_HOLDER
import acr.browser.lightning.utils.isBookmarkUrl
import acr.browser.lightning.utils.isDownloadsUrl
import acr.browser.lightning.utils.isHistoryUrl
import acr.browser.lightning.utils.isSpecialUrl
import acr.browser.lightning.utils.smartUrlFilter
import androidx.activity.result.ActivityResult
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
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
@Browser2Scope
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
    private val uiConfiguration: UiConfiguration,
    private val historyPageFactory: HistoryPageFactory,
    private val allowListModel: AllowListModel,
    private val cookieAdministrator: CookieAdministrator,
    private val tabCountNotifier: TabCountNotifier,
    @IncognitoMode private val incognitoMode: Boolean,
    private val coroutineDispatchers: CoroutineDispatchers,
) {

    private val browserCoroutineScope = BrowserCoroutineScope(
        CoroutineScope(coroutineDispatchers.main + SupervisorJob())
    )

    private var view: BrowserContract.View? = null
    private var viewState: BrowserViewState = BrowserViewState(
        displayUrl = "",
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
        findInPage = null
    )
    private var tabListState: List<TabViewState> = emptyList()
    private var currentTab: TabModel? = null
    private var currentFolder: Bookmark.Folder = Bookmark.Folder.Root
    private var isTabDrawerOpen = false
    private var isBookmarkDrawerOpen = false
    private var isSearchViewFocused = false
    private var pendingAction: BrowserContract.Action.LoadUrl? = null
    private var isCustomViewShowing = false

    private val tabJobs: MutableList<Job> = mutableListOf()
    private val allTabsJobs: MutableList<Job> = mutableListOf()

    /**
     * Call when the view is attached to the presenter.
     */
    fun onViewAttached(view: BrowserContract.View) {
        this.view = view
        view.updateState(viewState)

        currentFolder = Bookmark.Folder.Root
        browserCoroutineScope.launch {
            cookieAdministrator.adjustCookieSettings()

            val bookmarks = bookmarkRepository.bookmarksAndFolders(folder = Bookmark.Folder.Root)
            view.updateState(viewState.copy(bookmarks = bookmarks, isRootFolder = true))

            val tabs = model.initializeTabs()
            val lastTab = if (tabs.isEmpty()) {
                model.createTab(homePageInitializer)
            } else {
                tabs.last()
            }
            selectTab(model.selectTab(lastTab.id))
        }

        browserCoroutineScope.launch {
            model.tabsListChanges().collectLatest { list ->
                this@BrowserPresenter.view?.updateTabs(list.map { it.asViewState() })

                allTabsJobs.forEach { it.cancel() }
                allTabsJobs.clear()
                list.subscribeToUpdates(allTabsJobs)

                tabCountNotifier.notifyTabCountChange(list.size)
            }
        }
    }

    /**
     * Call when the view is detached from the presenter.
     */
    fun onViewDetached() {
        view = null

        tabJobs.forEach { it.cancel() }
        allTabsJobs.forEach { it.cancel() }
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

    private fun TabModel.asViewState(): TabViewState = TabViewState(
        id = id,
        icon = favicon,
        title = title,
        isSelected = isForeground,
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

    private fun selectTab(tabModel: TabModel?, focusTab: Boolean = true) {
        if (currentTab == tabModel) {
            view?.closeTabDrawer()
            return
        }
        currentTab?.isForeground = false
        currentTab = tabModel
        currentTab?.isForeground = true

        view?.clearSearchFocus()

        val tab = tabModel ?: return run {
            view.updateState(
                viewState.copy(
                    displayUrl = searchBoxModel.getDisplayContent(
                        url = "",
                        title = null,
                        isLoading = false
                    ),
                    enableFullMenu = false,
                    isForwardEnabled = false,
                    isBackEnabled = false,
                    sslState = SslState.None,
                    progress = 100,
                    findInPage = null
                )
            )
            view.updateTabs(tabListState.map { it.copy(isSelected = false) })
        }

        view?.showToolbar()
        if (focusTab) {
            view?.closeTabDrawer()
        }

        view.updateTabs(tabListState.map { it.copy(isSelected = it.id == tab.id) })

        tabJobs.forEach { it.cancel() }
        tabJobs.clear()

        tabJobs += browserCoroutineScope.launch {
            combine(
                tab.sslChanges().onStart { emit(tab.sslState) },
                tab.titleChanges().onStart { emit(tab.title) },
                tab.urlChanges().onStart { emit(tab.url) },
                tab.loadingProgress().onStart { emit(tab.loadingProgress) },
                tab.canGoBackChanges().onStart { emit(tab.canGoBack()) },
                tab.canGoForwardChanges().onStart { emit(tab.canGoForward()) },
                tab.urlChanges().onStart { emit(tab.url) }
                    .map { bookmarkRepository.isBookmark(it) },
                tab.urlChanges().onStart { emit(tab.url) }.map(String::isSpecialUrl),
                tab.themeColorChanges().onStart { emit(tab.themeColor) }
            ) { sslState, title, url, progress, canGoBack, canGoForward, isBookmark, isSpecialUrl, themeColor ->
                viewState.copy(
                    displayUrl = searchBoxModel.getDisplayContent(
                        url = url,
                        title = title,
                        isLoading = progress < 100
                    ).takeIf { !isSearchViewFocused } ?: viewState.displayUrl,
                    enableFullMenu = !url.isSpecialUrl(),
                    themeColor = Option.Some(themeColor),
                    isRefresh = (progress == 100).takeIf { !isSearchViewFocused }
                        ?: viewState.isRefresh,
                    isForwardEnabled = canGoForward,
                    isBackEnabled = canGoBack,
                    sslState = sslState.takeIf { !isSearchViewFocused } ?: viewState.sslState,
                    progress = progress,
                    isBookmarked = isBookmark,
                    isBookmarkEnabled = !isSpecialUrl,
                    findInPage = tab.findQuery
                )
            }.flowOn(coroutineDispatchers.main).collectLatest {
                view.updateState(it)
            }
        }


        tabJobs += browserCoroutineScope.launch {
            tab.downloadRequests().collectLatest {
                navigator.download(it)
            }
        }

        tabJobs += browserCoroutineScope.launch {
            tab.urlChanges()
                .distinctUntilChanged()
                .collectLatest { url ->
                    url.takeIf { !it.isSpecialUrl() && it.isNotBlank() }?.let {
                        historyRecord.visit(tab.title, it)
                    }
                    view?.showToolbar()
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
            tab.closeWindowRequests().collectLatest {
                onTabClose(tabListState.indexOfCurrentTab())
            }
        }

        tabJobs += browserCoroutineScope.launch {
            tab.fileChooserRequests().collectLatest {
                view?.showFileChooser(it)
            }
        }

        tabJobs += browserCoroutineScope.launch {
            tab.showCustomViewRequests().collectLatest {
                view?.showCustomView(it)
                isCustomViewShowing = true
            }
        }

        tabJobs += browserCoroutineScope.launch {
            tab.hideCustomViewRequests().collectLatest {
                view?.hideCustomView()
                isCustomViewShowing = false
            }
        }

        tabJobs += browserCoroutineScope.launch {
            tab.focusRequests().collectLatest {
                view?.closeTabDrawer()
            }
        }
    }

    private fun List<TabModel>.subscribeToUpdates(allTabsJobs: MutableList<Job>) {
        forEach { tabModel ->
            allTabsJobs += browserCoroutineScope.launch {
                combine(
                    tabModel.titleChanges().onStart { emit(tabModel.title) },
                    tabModel.faviconChanges()
                        .onStart { emit(tabModel.favicon) },
                    tabModel.previewChanges()
                ) { title, bitmap, pair ->
                    Triple(title, bitmap, pair)
                }.distinctUntilChanged()
                    .flowOn(coroutineDispatchers.main)
                    .collectLatest { (title, bitmap, _) ->
                        view.updateTabs(tabListState.updateId(tabModel.id) {
                            it.copy(title = title, icon = bitmap, preview = tabModel.preview)
                        })
                    }
            }
        }
    }

    /**
     * Call when a new action is triggered, such as the user opening a new URL in the browser.
     */
    fun onNewAction(action: BrowserContract.Action) {
        when (action) {
            is BrowserContract.Action.LoadUrl -> if (action.url.isSpecialUrl()) {
                view?.showLocalFileBlockedDialog()
                pendingAction = action
            } else {
                createNewTabAndSelect(
                    tabInitializer = UrlInitializer(action.url),
                    shouldSelect = true,
                    tabType = TabModel.Type.EPHEMERAL
                )
            }

            BrowserContract.Action.Panic -> panicClean()
        }
    }

    /**
     * Call when the user confirms that they do or do not want to allow a local file to be opened
     * in the browser. This is a security gate to prevent malicious local files from being opened
     * in the browser without the user's knowledge.
     */
    fun onConfirmOpenLocalFile(allow: Boolean) {
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

    private fun panicClean() {
        createNewTabAndSelect(tabInitializer = NoOpInitializer(), shouldSelect = true)

        browserCoroutineScope.launch {
            model.clean()
            historyPageFactory.deleteHistoryPage()

            model.deleteAllTabs()
            navigator.closeBrowser()

            // System exit needed in the case of receiving
            // the panic intent since finish() isn't completely
            // closing the browser
            exitProcess(1)
        }
    }

    /**
     * Call when the user selects an option from the menu.
     */
    fun onMenuClick(menuSelection: MenuSelection) {
        when (menuSelection) {
            MenuSelection.NEW_TAB -> onNewTabClick()
            MenuSelection.NEW_INCOGNITO_TAB -> navigator.launchIncognito(url = null)
            MenuSelection.SHARE -> currentTab?.url?.takeIf { !it.isSpecialUrl() }?.let {
                navigator.sharePage(url = it, title = currentTab?.title)
            }

            MenuSelection.HISTORY -> createNewTabAndSelect(
                historyPageInitializer,
                shouldSelect = true
            )

            MenuSelection.DOWNLOADS -> createNewTabAndSelect(
                downloadPageInitializer,
                shouldSelect = true
            )

            MenuSelection.FIND -> view?.showFindInPageDialog()
            MenuSelection.COPY_LINK -> currentTab?.url?.takeIf { !it.isSpecialUrl() }
                ?.let(navigator::copyPageLink)

            MenuSelection.ADD_TO_HOME -> currentTab?.url?.takeIf { !it.isSpecialUrl() }
                ?.let { addToHomeScreen() }

            MenuSelection.BOOKMARKS -> view?.openBookmarkDrawer()
            MenuSelection.ADD_BOOKMARK -> currentTab?.url?.takeIf { !it.isSpecialUrl() }
                ?.let { showAddBookmarkDialog() }

            MenuSelection.SETTINGS -> navigator.openSettings()
            MenuSelection.BACK -> onBackClick()
            MenuSelection.FORWARD -> onForwardClick()
        }
    }

    private fun addToHomeScreen() {
        currentTab?.let {
            navigator.addToHomeScreen(it.url, it.title, it.favicon)
        }
    }

    private fun createNewTabAndSelect(
        tabInitializer: TabInitializer,
        shouldSelect: Boolean,
        tabType: TabModel.Type = TabModel.Type.NORMAL
    ) {
        browserCoroutineScope.launch {
            val tab = model.createTab(tabInitializer, tabType = tabType)
            if (shouldSelect) {
                selectTab(model.selectTab(tab.id))
            }
        }
    }

    private fun List<TabViewState>.tabIndexForId(id: Int?): Int =
        indexOfFirst { it.id == id }

    private fun List<TabViewState>.indexOfCurrentTab(): Int = tabIndexForId(currentTab?.id)

    /**
     * Call when the user selects a combination of keys to perform a shortcut.
     */
    fun onKeyComboClick(keyCombo: KeyCombo) {
        when (keyCombo) {
            KeyCombo.CTRL_F -> view?.showFindInPageDialog()
            KeyCombo.CTRL_T -> onNewTabClick()
            KeyCombo.CTRL_W -> onTabClose(tabListState.indexOfCurrentTab())
            KeyCombo.CTRL_Q -> view?.showCloseBrowserDialog(tabListState.indexOfCurrentTab())
            KeyCombo.CTRL_R -> onRefreshOrStopClick()
            KeyCombo.CTRL_TAB -> TODO()
            KeyCombo.CTRL_SHIFT_TAB -> TODO()
            KeyCombo.SEARCH -> TODO()
            KeyCombo.ALT_0 -> onTabClick(0.coerceAtMost(tabListState.size - 1))
            KeyCombo.ALT_1 -> onTabClick(1.coerceAtMost(tabListState.size - 1))
            KeyCombo.ALT_2 -> onTabClick(2.coerceAtMost(tabListState.size - 1))
            KeyCombo.ALT_3 -> onTabClick(3.coerceAtMost(tabListState.size - 1))
            KeyCombo.ALT_4 -> onTabClick(4.coerceAtMost(tabListState.size - 1))
            KeyCombo.ALT_5 -> onTabClick(5.coerceAtMost(tabListState.size - 1))
            KeyCombo.ALT_6 -> onTabClick(6.coerceAtMost(tabListState.size - 1))
            KeyCombo.ALT_7 -> onTabClick(7.coerceAtMost(tabListState.size - 1))
            KeyCombo.ALT_8 -> onTabClick(8.coerceAtMost(tabListState.size - 1))
            KeyCombo.ALT_9 -> onTabClick(9.coerceAtMost(tabListState.size - 1))
        }
    }

    /**
     * Call when the user selects a tab to switch to at the provided [index].
     */
    fun onTabClick(index: Int) {
        selectTab(model.selectTab(tabListState[index].id))
    }

    /**
     * Call when the user long presses on a tab at the provided [index].
     */
    fun onTabLongClick(index: Int) {
        view?.showCloseBrowserDialog(tabListState[index].id)
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

    /**
     * Call when the user clicks on the close button for the tab at the provided [index]
     */
    fun onTabClose(index: Int) {
        if (index == -1) {
            // If the user clicks on close multiple times, the index may be -1 if the view is in the
            // process of being removed.
            return
        }
        val nextTab = tabListState.nextSelected(index)

        val currentTabId = currentTab?.id
        val needToSelectNextTab = tabListState[index].id == currentTabId

        browserCoroutineScope.launch {
            model.deleteTab(tabListState[index].id)
            if (needToSelectNextTab) {
                nextTab?.id?.let {
                    val shouldClose = currentTab?.tabType == TabModel.Type.EPHEMERAL
                    selectTab(model.selectTab(it), focusTab = false)
                    if (shouldClose) {
                        navigator.backgroundBrowser()
                    }
                } ?: run {
                    selectTab(tabModel = null)
                    navigator.closeBrowser()
                }
            }
        }
    }

    /**
     * Call when the tab drawer is opened or closed.
     *
     * @param isOpen True if the drawer is now open, false if it is now closed.
     */
    fun onTabDrawerMoved(isOpen: Boolean) {
        isTabDrawerOpen = isOpen
        if (isOpen) {
            view?.openTabDrawer()
        } else {
            view?.closeTabDrawer()
        }
    }

    /**
     * Call when the bookmark drawer is opened or closed.
     *
     * @param isOpen True if the drawer is now open, false if it is now closed.
     */
    fun onBookmarkDrawerMoved(isOpen: Boolean) {
        isBookmarkDrawerOpen = isOpen
        if (isOpen) {
            view?.openBookmarkDrawer()
        } else {
            view?.closeBookmarkDrawer()
        }
    }

    /**
     * Called when the user clicks on the device back button or swipes to go back. Differentiated
     * from [onBackClick] which is called when the user presses the browser's back button.
     */
    fun onNavigateBack() {
        when {
            isCustomViewShowing -> {
                view?.hideCustomView()
                currentTab?.hideCustomView()
            }

            isTabDrawerOpen -> view?.closeTabDrawer()
            isBookmarkDrawerOpen -> if (currentFolder != Bookmark.Folder.Root) {
                onBookmarkMenuClick()
            } else {
                view?.closeBookmarkDrawer()
            }

            currentTab?.canGoBack() == true -> currentTab?.goBack()
            currentTab?.canGoBack() == false -> if (incognitoMode) {
                currentTab?.id?.let {
                    view?.showCloseBrowserDialog(it)
                }
            } else if (currentTab?.tabType in listOf(
                    TabModel.Type.EPHEMERAL,
                    TabModel.Type.POP_UP
                )
            ) {
                onTabClose(tabListState.indexOfCurrentTab())
            } else {
                navigator.backgroundBrowser()
            }
        }
    }

    /**
     * Called when the user presses the browser's back button.
     */
    fun onBackClick() {
        if (currentTab?.canGoBack() == true) {
            currentTab?.goBack()
        }
    }

    /**
     * Called when the user presses the browser's forward button.
     */
    fun onForwardClick() {
        if (currentTab?.canGoForward() == true) {
            currentTab?.goForward()
        }
    }

    /**
     * Call when the user clicks on the home button.
     */
    fun onHomeClick() {
        currentTab?.loadFromInitializer(homePageInitializer)
    }

    /**
     * Call when the user clicks on the open new tab button.
     */
    fun onNewTabClick() {
        createNewTabAndSelect(homePageInitializer, shouldSelect = true)
    }

    /**
     * Call when the user long clicks on the new tab button, indicating that they want to re-open
     * the last closed tab.
     */
    fun onNewTabLongClick() {
        browserCoroutineScope.launch {
            val tab = model.reopenTab()
            if (tab != null) {
                selectTab(model.selectTab(tab.id))
            }
        }
    }

    /**
     * Call when the user clicks on the refresh (or stop/delete) button that is located in the
     * search bar.
     */
    fun onRefreshOrStopClick() {
        if (isSearchViewFocused) {
            view?.renderState(viewState.copy(displayUrl = ""))
            return
        }
        if (currentTab?.loadingProgress != 100) {
            currentTab?.stopLoading()
        } else {
            reload()
        }
    }

    private fun reload() {
        val currentUrl = currentTab?.url
        if (currentUrl?.isSpecialUrl() == true) {
            when {
                currentUrl.isBookmarkUrl() ->
                    browserCoroutineScope.launch {
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

    /**
     * Call when the focus state changes for the search bar.
     *
     * @param isFocused True if the view is now focused, false otherwise.
     */
    fun onSearchFocusChanged(isFocused: Boolean) {
        isSearchViewFocused = isFocused
        if (isFocused) {
            view?.updateState(
                viewState.copy(
                    sslState = SslState.None,
                    isRefresh = false,
                    displayUrl = currentTab?.url?.takeIf { !it.isSpecialUrl() }.orEmpty()
                )
            )
        } else {
            view?.updateState(
                viewState.copy(
                    sslState = currentTab?.sslState ?: SslState.None,
                    isRefresh = (currentTab?.loadingProgress ?: 0) == 100,
                    displayUrl = searchBoxModel.getDisplayContent(
                        url = currentTab?.url.orEmpty(),
                        title = currentTab?.title.orEmpty(),
                        isLoading = (currentTab?.loadingProgress ?: 0) < 100
                    )
                )
            )
        }
    }

    /**
     * Call when the user submits a search [query] to the search bar. At this point the user has
     * provided intent to search and is no longer trying to manipulate the query.
     */
    fun onSearch(query: String) {
        if (query.isEmpty()) {
            return
        }
        currentTab?.stopLoading()
        val searchUrl = searchEngineProvider.provideSearchEngine().queryUrl + QUERY_PLACE_HOLDER
        val url = smartUrlFilter(query.trim(), true, searchUrl)
        view?.updateState(
            viewState.copy(
                displayUrl = searchBoxModel.getDisplayContent(
                    url = url,
                    title = currentTab?.title,
                    isLoading = (currentTab?.loadingProgress ?: 0) < 100
                )
            )
        )
        currentTab?.loadUrl(url)
    }

    /**
     * Call when the user enters a [query] to look for in the current web page.
     */
    fun onFindInPage(query: String) {
        currentTab?.find(query)
        view?.updateState(viewState.copy(findInPage = query))
    }

    /**
     * Call when the user selects to move to the next highlighted word in the web page.
     */
    fun onFindNext() {
        currentTab?.findNext()
    }

    /**
     * Call when the user selects to move to the previous highlighted word in the web page.
     */
    fun onFindPrevious() {
        currentTab?.findPrevious()
    }

    /**
     * Call when the user chooses to dismiss the find in page UI component.
     */
    fun onFindDismiss() {
        currentTab?.clearFindMatches()
        view?.updateState(viewState.copy(findInPage = null))
    }

    /**
     * Call when the user selects a search suggestion that was suggested by the search box.
     */
    fun onSearchSuggestionClicked(webPage: WebPage) {
        val url = when (webPage) {
            is HistoryEntry,
            is Bookmark.Entry -> webPage.url

            is SearchSuggestion -> webPage.title
            else -> null
        } ?: error("Other types cannot be search suggestions: $webPage")

        onSearch(url)
    }

    /**
     * Call when the user clicks on the SSL icon in the search box.
     */
    fun onSslIconClick() {
        currentTab?.sslCertificateInfo?.let {
            view?.showSslDialog(it)
        }
    }

    /**
     * Call when the user clicks on a bookmark from the bookmark list at the provided [index].
     */
    fun onBookmarkClick(index: Int) {
        when (val bookmark = viewState.bookmarks[index]) {
            is Bookmark.Entry -> {
                currentTab?.loadUrl(bookmark.url)
                view?.closeBookmarkDrawer()
            }

            Bookmark.Folder.Root -> error("Cannot click on root folder")
            is Bookmark.Folder.Entry -> {
                currentFolder = bookmark
                browserCoroutineScope.launch {
                    val bookmarks = bookmarkRepository.bookmarksAndFolders(folder = bookmark)
                    view?.updateState(viewState.copy(bookmarks = bookmarks, isRootFolder = false))
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

    /**
     * Call when the user long presses on a bookmark in the bookmark list at the provided [index].
     */
    fun onBookmarkLongClick(index: Int) {
        when (val item = viewState.bookmarks[index]) {
            is Bookmark.Entry -> view?.showBookmarkOptionsDialog(item)
            is Bookmark.Folder.Entry -> view?.showFolderOptionsDialog(item)
            Bookmark.Folder.Root -> Unit // Root is not clickable
        }
    }

    /**
     * Call when the user clicks on the page tools button.
     */
    fun onToolsClick() {
        val currentUrl = currentTab?.url ?: return
        view?.showToolsDialog(
            areAdsAllowed = allowListModel.isUrlAllowedAds(currentUrl),
            shouldShowAdBlockOption = !currentUrl.isSpecialUrl()
        )
    }

    /**
     * Call when the user chooses to toggle the desktop user agent on/off.
     */
    fun onToggleDesktopAgent() {
        browserCoroutineScope.launch {
            currentTab?.toggleDesktopAgent()
            currentTab?.reload()
        }
    }

    /**
     * Call when the user chooses to toggle ad blocking on/off for the current web page.
     */
    fun onToggleAdBlocking() {
        val currentUrl = currentTab?.url ?: return
        if (allowListModel.isUrlAllowedAds(currentUrl)) {
            allowListModel.removeUrlFromAllowList(currentUrl)
        } else {
            allowListModel.addUrlToAllowList(currentUrl)
        }
        currentTab?.reload()
    }

    /**
     * Call when the user clicks on the star icon to add a bookmark for the current page or remove
     * the existing one.
     */
    fun onStarClick() {
        val url = currentTab?.url ?: return
        val title = currentTab?.title.orEmpty()
        if (url.isSpecialUrl()) {
            return
        }
        browserCoroutineScope.launch {
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
                view?.updateState(
                    viewState.copy(
                        bookmarks = bookmarks,
                        isBookmarked = bookmarkRepository.isBookmark(url)
                    )
                )
            } else {
                showAddBookmarkDialog()
            }
        }
    }

    private fun showAddBookmarkDialog() {
        browserCoroutineScope.launch {
            val folders = bookmarkRepository.getFolderNames()
            view?.showAddBookmarkDialog(
                title = currentTab?.title.orEmpty(),
                url = currentTab?.url.orEmpty(),
                folders = folders
            )
        }
    }

    /**
     * Call when the user confirms the details for adding a bookmark.
     *
     * @param title The title of the bookmark.
     * @param url The URL of the bookmark.
     * @param folder The name of the folder the bookmark is in.
     */
    fun onBookmarkConfirmed(title: String, url: String, folder: String) {
        browserCoroutineScope.launch {
            bookmarkRepository.addBookmarkIfNotExists(
                Bookmark.Entry(
                    url = url,
                    title = title,
                    position = 0,
                    folder = folder.asFolder()
                )
            )
            val bookmarks = bookmarkRepository.bookmarksAndFolders(folder = currentFolder)
            view?.updateState(
                viewState.copy(
                    bookmarks = bookmarks,
                    isBookmarked = bookmarkRepository.isBookmark(url)
                )
            )
        }
    }

    /**
     * Call when the user confirms the details when editing a bookmark.
     *
     * @param title The title of the bookmark.
     * @param url The URL of the bookmark.
     * @param folder The name of the folder the bookmark is in.
     */
    fun onBookmarkEditConfirmed(title: String, url: String, folder: String) {
        browserCoroutineScope.launch {
            bookmarkRepository.editBookmark(
                oldBookmark = Bookmark.Entry(
                    url = url,
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
            view?.updateState(viewState.copy(bookmarks = bookmarks))
            if (currentTab?.url?.isBookmarkUrl() == true) {
                reload()
            }
        }
    }

    /**
     * Call when the user confirms a name change to an existing folder.
     *
     * @param oldTitle The previous title of the folder.
     * @param newTitle The new title of the folder.
     */
    fun onBookmarkFolderRenameConfirmed(oldTitle: String, newTitle: String) {
        browserCoroutineScope.launch {
            bookmarkRepository.renameFolder(oldTitle, newTitle)
            val bookmarks = bookmarkRepository.bookmarksAndFolders(folder = currentFolder)
            view?.updateState(viewState.copy(bookmarks = bookmarks))
            if (currentTab?.url?.isBookmarkUrl() == true) {
                reload()
            }
        }
    }

    /**
     * Call when the user clicks on a menu [option] for the provided [bookmark].
     */
    fun onBookmarkOptionClick(
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

            BrowserContract.BookmarkOptionEvent.COPY_LINK ->
                navigator.copyPageLink(bookmark.url)

            BrowserContract.BookmarkOptionEvent.REMOVE ->
                browserCoroutineScope.launch {
                    bookmarkRepository.deleteBookmark(bookmark)
                    val bookmarks = bookmarkRepository.bookmarksAndFolders(folder = currentFolder)
                    view?.updateState(viewState.copy(bookmarks = bookmarks))
                    if (currentTab?.url?.isBookmarkUrl() == true) {
                        reload()
                    }
                }

            BrowserContract.BookmarkOptionEvent.EDIT ->
                browserCoroutineScope.launch {
                    val folders = bookmarkRepository.getFolderNames()
                    view?.showEditBookmarkDialog(
                        bookmark.title,
                        bookmark.url,
                        bookmark.folder.title,
                        folders
                    )
                }
        }
    }

    /**
     * Call when the user clicks on a menu [option] for the provided [folder].
     */
    fun onFolderOptionClick(folder: Bookmark.Folder, option: BrowserContract.FolderOptionEvent) {
        when (option) {
            BrowserContract.FolderOptionEvent.RENAME -> view?.showEditFolderDialog(folder.title)
            BrowserContract.FolderOptionEvent.REMOVE ->
                browserCoroutineScope.launch {
                    bookmarkRepository.deleteFolder(folder.title)
                    val bookmarks = bookmarkRepository.bookmarksAndFolders(folder = currentFolder)
                    view?.updateState(viewState.copy(bookmarks = bookmarks))
                    if (currentTab?.url?.isBookmarkUrl() == true) {
                        reload()
                        currentTab?.goBack()
                    }
                }
        }
    }

    /**
     * Call when the user clicks on a menu [option] for the provided [download] entry.
     */
    fun onDownloadOptionClick(
        download: DownloadEntry,
        option: BrowserContract.DownloadOptionEvent
    ) {
        when (option) {
            BrowserContract.DownloadOptionEvent.DELETE ->
                browserCoroutineScope.launch {
                    downloadsRepository.deleteAllDownloads()
                    if (currentTab?.url?.isDownloadsUrl() == true) {
                        reload()
                    }
                }

            BrowserContract.DownloadOptionEvent.DELETE_ALL ->
                browserCoroutineScope.launch {
                    downloadsRepository.deleteDownload(download.url)
                    if (currentTab?.url?.isDownloadsUrl() == true) {
                        reload()
                    }
                }
        }
    }

    /**
     * Call when the user clicks on a menu [option] for the provided [historyEntry].
     */
    fun onHistoryOptionClick(
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

            BrowserContract.HistoryOptionEvent.COPY_LINK -> navigator.copyPageLink(historyEntry.url)
            BrowserContract.HistoryOptionEvent.REMOVE ->
                browserCoroutineScope.launch {
                    historyRepository.deleteHistoryEntry(historyEntry.url)
                    if (currentTab?.url?.isHistoryUrl() == true) {
                        reload()
                    }
                }
        }
    }

    /**
     * Call when the user clicks on the tab count button (or home button in desktop mode, or
     * incognito icon in incognito mode).
     */
    fun onTabCountViewClick() {
        if (uiConfiguration.tabConfiguration == TabConfiguration.DRAWER_SIDE) {
            view?.openTabDrawer()
        } else if (uiConfiguration.tabConfiguration == TabConfiguration.DRAWER_BOTTOM) {
            if (isTabDrawerOpen) {
                view?.closeTabDrawer()
            } else {
                view?.openTabDrawer()
            }
        } else {
            currentTab?.loadFromInitializer(homePageInitializer)
        }
    }

    /**
     * Call when the user clicks on the tab menu located in the tab drawer.
     */
    fun onTabMenuClick() {
        currentTab?.let {
            view?.showCloseBrowserDialog(it.id)
        }
    }

    /**
     * Call when the user clicks on the bookmark menu (star or back arrow) located in the bookmark
     * drawer.
     */
    fun onBookmarkMenuClick() {
        if (currentFolder != Bookmark.Folder.Root) {
            currentFolder = Bookmark.Folder.Root
            browserCoroutineScope.launch {
                val bookmarks =
                    bookmarkRepository.bookmarksAndFolders(folder = Bookmark.Folder.Root)
                view?.updateState(viewState.copy(bookmarks = bookmarks, isRootFolder = true))
            }
        }
    }

    /**
     * Call when the user long presses anywhere on the web page with the provided tab [id].
     */
    fun onPageLongPress(id: Int, longPress: LongPress) {
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
                    view?.showFolderOptionsDialog(folderTitle.asFolder())
                } else {
                    browserCoroutineScope.launch {
                        val bookmark = bookmarkRepository.findBookmarkForUrl(url)
                        if (bookmark != null) {
                            view?.showBookmarkOptionsDialog(bookmark)
                        }
                    }
                }
            } else if (pageUrl.isDownloadsUrl()) {
                browserCoroutineScope.launch {
                    val download = downloadsRepository.findDownloadForUrl(url)
                    if (download != null) {
                        view?.showDownloadOptionsDialog(download)
                    }
                }
            } else if (pageUrl.isHistoryUrl()) {
                browserCoroutineScope.launch {
                    val entries = historyRepository.findHistoryEntriesContaining(url)
                    entries.firstOrNull()?.let {
                        view?.showHistoryOptionsDialog(it)
                    } ?: view?.showHistoryOptionsDialog(HistoryEntry(url = url, title = ""))
                }
            }
        } else {
            when (longPress.hitCategory) {
                LongPress.Category.IMAGE -> view?.showImageLongPressDialog(longPress)
                LongPress.Category.LINK -> view?.showLinkLongPressDialog(longPress)
                LongPress.Category.UNKNOWN -> Unit // Do nothing
            }
        }
    }

    /**
     * Call when the user selects an option from the close browser menu that can be invoked by long
     * pressing on individual tabs.
     */
    fun onCloseBrowserEvent(id: Int, closeTabEvent: BrowserContract.CloseTabEvent) {
        when (closeTabEvent) {
            BrowserContract.CloseTabEvent.CLOSE_CURRENT ->
                onTabClose(tabListState.tabIndexForId(id))

            BrowserContract.CloseTabEvent.CLOSE_OTHERS -> browserCoroutineScope.launch {
                val currentTabId = currentTab?.id
                model.tabsList.filter { it.id != id }.forEach {
                    model.deleteTab(it.id)
                    if (currentTabId != id) {
                        selectTab(model.selectTab(id))
                    }
                }
            }

            BrowserContract.CloseTabEvent.CLOSE_ALL -> browserCoroutineScope.launch {
                model.deleteAllTabs()
                navigator.closeBrowser()
            }
        }
    }

    /**
     * Call when the user long presses on a link within the web page and selects what they want to
     * do with that link.
     */
    fun onLinkLongPressEvent(
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

            BrowserContract.LinkLongPressEvent.COPY_LINK ->
                longPress.targetUrl?.let(navigator::copyPageLink)
        }
    }

    /**
     * Call when the user long presses on an image within the web page and selects what they want to
     * do with that image.
     */
    fun onImageLongPressEvent(
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

            BrowserContract.ImageLongPressEvent.COPY_LINK ->
                longPress.targetUrl?.let(navigator::copyPageLink)

            BrowserContract.ImageLongPressEvent.DOWNLOAD -> navigator.download(
                PendingDownload(
                    url = longPress.targetUrl.orEmpty(),
                    userAgent = null,
                    contentDisposition = "attachment",
                    mimeType = null,
                    contentLength = 0
                )
            )
        }
    }

    /**
     * Call when the user has selected a file from the file chooser to upload.
     */
    fun onFileChooserResult(activityResult: ActivityResult) {
        currentTab?.handleFileChooserResult(activityResult)
    }

    private fun BrowserContract.View?.updateState(state: BrowserViewState) {
        viewState = state
        this?.renderState(viewState)
    }

    private fun BrowserContract.View?.updateTabs(tabs: List<TabViewState>) {
        tabListState = tabs
        this?.renderTabs(tabListState)
    }
}
