package acr.browser.lightning._browser2

import acr.browser.lightning._browser2.data.CookieAdministrator
import acr.browser.lightning._browser2.di.Browser2Scope
import acr.browser.lightning._browser2.di.IncognitoMode
import acr.browser.lightning._browser2.di.InitialUrl
import acr.browser.lightning._browser2.download.PendingDownload
import acr.browser.lightning._browser2.history.HistoryRecord
import acr.browser.lightning._browser2.keys.KeyCombo
import acr.browser.lightning._browser2.menu.MenuSelection
import acr.browser.lightning._browser2.notification.TabCountNotifier
import acr.browser.lightning._browser2.tab.TabModel
import acr.browser.lightning._browser2.tab.TabViewState
import acr.browser.lightning._browser2.ui.TabConfiguration
import acr.browser.lightning._browser2.ui.UiConfiguration
import acr.browser.lightning.adblock.allowlist.AllowListModel
import acr.browser.lightning.browser.SearchBoxModel
import acr.browser.lightning.database.*
import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.database.downloads.DownloadEntry
import acr.browser.lightning.database.downloads.DownloadsRepository
import acr.browser.lightning.database.history.HistoryRepository
import acr.browser.lightning.di.DatabaseScheduler
import acr.browser.lightning.di.DiskScheduler
import acr.browser.lightning.di.MainScheduler
import acr.browser.lightning.html.bookmark.BookmarkPageFactory
import acr.browser.lightning.html.history.HistoryPageFactory
import acr.browser.lightning.search.SearchEngineProvider
import acr.browser.lightning.ssl.SslState
import acr.browser.lightning.utils.*
import acr.browser.lightning.view.*
import android.graphics.Color
import androidx.core.net.toUri
import io.reactivex.Maybe
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.toObservable
import targetUrl.LongPress
import javax.inject.Inject
import kotlin.system.exitProcess

/**
 * Created by anthonycr on 9/11/20.
 */
@Browser2Scope
class BrowserPresenter @Inject constructor(
    private val model: BrowserContract.Model,
    private val navigator: BrowserContract.Navigator,
    private val bookmarkRepository: BookmarkRepository,
    private val downloadsRepository: DownloadsRepository,
    private val historyRepository: HistoryRepository,
    @DiskScheduler private val diskScheduler: Scheduler,
    @MainScheduler private val mainScheduler: Scheduler,
    @DatabaseScheduler private val databaseScheduler: Scheduler,
    private val historyRecord: HistoryRecord,
    private val bookmarkPageFactory: BookmarkPageFactory,
    private val homePageInitializer: HomePageInitializer,
    private val historyPageInitializer: HistoryPageInitializer,
    private val downloadPageInitializer: DownloadPageInitializer,
    private val searchBoxModel: SearchBoxModel,
    private val searchEngineProvider: SearchEngineProvider,
    @InitialUrl private val initialUrl: String?,
    private val uiConfiguration: UiConfiguration,
    private val historyPageFactory: HistoryPageFactory,
    private val allowListModel: AllowListModel,
    private val cookieAdministrator: CookieAdministrator,
    private val tabCountNotifier: TabCountNotifier,
    @IncognitoMode private val incognitoMode: Boolean
) {

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
        findInPage = ""
    )
    private var tabListState: List<TabViewState> = emptyList()
    private var currentTab: TabModel? = null
    private var currentFolder: Bookmark.Folder = Bookmark.Folder.Root
    private var isTabDrawerOpen = false
    private var isBookmarkDrawerOpen = false
    private var isSearchViewFocused = false
    private var tabIdOpenedFromAction = -1
    private var pendingAction: BrowserContract.Action.LoadUrl? = null

    private val compositeDisposable = CompositeDisposable()
    private val allTabsDisposable = CompositeDisposable()
    private var tabDisposable: CompositeDisposable = CompositeDisposable()

    /**
     * TODO
     */
    fun onViewAttached(view: BrowserContract.View) {
        this.view = view
        view.updateState(viewState)

        cookieAdministrator.adjustCookieSettings()

        currentFolder = Bookmark.Folder.Root
        compositeDisposable += bookmarkRepository.bookmarksAndFolders(folder = Bookmark.Folder.Root)
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribe { list ->
                this.view?.updateState(viewState.copy(bookmarks = list, isRootFolder = true))
            }

        compositeDisposable += model.tabsListChanges()
            .observeOn(mainScheduler)
            .subscribe { list ->
                this.view?.updateTabs(list.map { it.asViewState() })

                allTabsDisposable.clear()
                list.subscribeToUpdates(allTabsDisposable)

                tabCountNotifier.notifyTabCountChange(list.size)
            }

        compositeDisposable += model.initializeTabs()
            .observeOn(mainScheduler)
            .concatWith(
                Maybe.fromCallable { initialUrl }
                    .flatMapSingleElement { model.createTab(UrlInitializer(it)) }
                    .map(::listOf)
            )
            .toList()
            .map(MutableList<List<TabModel>>::flatten)
            .filter(List<TabModel>::isNotEmpty)
            .switchIfEmpty(model.createTab(homePageInitializer).map(::listOf))
            .subscribe { list ->
                selectTab(model.selectTab(list.last().id))
            }
    }

    /**
     * TODO
     */
    fun onViewDetached() {
        view = null

        compositeDisposable.dispose()
        tabDisposable.dispose()
    }

    /**
     * TODO
     */
    fun onViewHidden() {
        model.freeze()
        tabIdOpenedFromAction = -1
    }

    private fun TabModel.asViewState(): TabViewState = TabViewState(
        id = id,
        icon = favicon,
        title = title,
        isSelected = isForeground
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

    private fun selectTab(tabModel: TabModel?) {
        if (currentTab == tabModel) {
            return
        }
        currentTab?.isForeground = false
        currentTab = tabModel
        currentTab?.isForeground = true

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
                    findInPage = ""
                )
            )
            view.updateTabs(tabListState.map { it.copy(isSelected = false) })
        }

        view?.showToolbar()
        view?.closeTabDrawer()

        view.updateTabs(tabListState.map { it.copy(isSelected = it.id == tab.id) })

        tabDisposable.dispose()
        tabDisposable = CompositeDisposable()
        tabDisposable += Observables.combineLatest(
            tab.sslChanges().startWith(tab.sslState),
            tab.titleChanges().startWith(tab.title),
            tab.urlChanges().startWith(tab.url),
            tab.loadingProgress().startWith(tab.loadingProgress),
            tab.canGoBackChanges().startWith(tab.canGoBack()),
            tab.canGoForwardChanges().startWith(tab.canGoForward()),
            tab.urlChanges().startWith(tab.url).observeOn(diskScheduler)
                .flatMapSingle(bookmarkRepository::isBookmark).observeOn(mainScheduler),
            tab.urlChanges().startWith(tab.url).map(String::isSpecialUrl),
            tab.themeColorChanges().startWith(tab.themeColor)
        ) { sslState, title, url, progress, canGoBack, canGoForward, isBookmark, isSpecialUrl, themeColor ->

            viewState.copy(
                displayUrl = searchBoxModel.getDisplayContent(
                    url = url,
                    title = title,
                    isLoading = progress < 100
                ),
                enableFullMenu = !url.isSpecialUrl(),
                themeColor = Option.Some(themeColor),
                isRefresh = progress == 100,
                isForwardEnabled = canGoForward,
                isBackEnabled = canGoBack,
                sslState = sslState,
                progress = progress,
                isBookmarked = isBookmark,
                isBookmarkEnabled = !isSpecialUrl,
                findInPage = tab.findQuery.orEmpty()
            )
        }.observeOn(mainScheduler)
            .subscribe { view.updateState(it) }

        tabDisposable += tab.downloadRequests()
            .subscribeOn(mainScheduler)
            .subscribeBy(onNext = navigator::download)

        tabDisposable += tab.urlChanges()
            .distinctUntilChanged()
            .subscribeOn(mainScheduler)
            .subscribeBy { view?.showToolbar() }

        tabDisposable += tab.createWindowRequests()
            .subscribeOn(mainScheduler)
            .subscribeBy { createNewTabAndSelect(it, shouldSelect = true) }

        tabDisposable += tab.closeWindowRequests()
            .subscribeOn(mainScheduler)
            .subscribeBy { onTabClose(tabListState.indexOfCurrentTab()) }
    }

    private fun List<TabModel>.subscribeToUpdates(compositeDisposable: CompositeDisposable) {
        forEach { tabModel ->
            compositeDisposable += Observables.combineLatest(
                tabModel.titleChanges().startWith(tabModel.title),
                tabModel.faviconChanges()
                    .startWith(Option.fromNullable(tabModel.favicon))
            ).distinctUntilChanged()
                .subscribeOn(mainScheduler)
                .subscribeBy { (title, bitmap) ->
                    view.updateTabs(tabListState.updateId(tabModel.id) {
                        it.copy(title = title, icon = bitmap.value())
                    })

                    tabModel.url.takeIf { !it.isSpecialUrl() && it.isNotBlank() }?.let {
                        historyRecord.recordVisit(title, it)
                    }
                }
        }
    }

    /**
     * TODO
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
                    markAsOpenedFromAction = true
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
                    markAsOpenedFromAction = true
                )
            }
        }
        pendingAction = null
    }

    private fun panicClean() {
        createNewTabAndSelect(tabInitializer = NoOpInitializer(), shouldSelect = true)
        model.clean()

        historyPageFactory.deleteHistoryPage().subscribe()
        model.deleteAllTabs().subscribe()
        navigator.closeBrowser()

        // System exit needed in the case of receiving
        // the panic intent since finish() isn't completely
        // closing the browser
        exitProcess(1)
    }

    /**
     * TODO
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
            MenuSelection.READER -> currentTab?.url?.takeIf { !it.isSpecialUrl() }
                ?.let(navigator::openReaderMode)
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
        markAsOpenedFromAction: Boolean = false
    ) {
        compositeDisposable += model.createTab(tabInitializer)
            .observeOn(mainScheduler)
            .subscribe { tab ->
                if (shouldSelect) {
                    selectTab(model.selectTab(tab.id))
                    if (markAsOpenedFromAction) {
                        tabIdOpenedFromAction = tab.id
                    }
                }
            }
    }

    private fun List<TabViewState>.tabIndexForId(id: Int?): Int =
        indexOfFirst { it.id == id }

    private fun List<TabViewState>.indexOfCurrentTab(): Int = tabIndexForId(currentTab?.id)

    /**
     * TODO
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
     * TODO
     */
    fun onTabClick(index: Int) {
        selectTab(model.selectTab(tabListState[index].id))
    }

    /**
     * TODO
     */
    fun onTabLongClick(index: Int) {
        view?.showCloseBrowserDialog(tabListState[index].id)
    }

    private fun <T> List<T>.nextSelected(removedIndex: Int): T? {
        val nextIndex = when {
            removedIndex > 0 -> removedIndex - 1
            size > removedIndex + 1 -> removedIndex + 1
            else -> -1
        }
        return if (nextIndex >= 0) {
            this[nextIndex]
        } else {
            null
        }
    }

    /**
     * TODO
     */
    fun onTabClose(index: Int) {
        if (index == -1) {
            return
        }
        val nextTab = tabListState.nextSelected(index)

        val currentTabId = currentTab?.id
        val needToSelectNextTab = tabListState[index].id == currentTabId

        compositeDisposable += model.deleteTab(tabListState[index].id)
            .observeOn(mainScheduler)
            .subscribe {
                if (needToSelectNextTab) {
                    nextTab?.id?.let {
                        selectTab(model.selectTab(it))
                        if (tabIdOpenedFromAction == currentTabId) {
                            tabIdOpenedFromAction = -1
                            navigator.backgroundBrowser()
                        }
                    } ?: run {
                        selectTab(tabModel = null)
                        navigator.closeBrowser()
                    }

                }
            }
    }

    fun onTabDrawerMoved(isOpen: Boolean) {
        isTabDrawerOpen = isOpen
    }

    fun onBookmarkDrawerMoved(isOpen: Boolean) {
        isBookmarkDrawerOpen = isOpen
    }

    /**
     * Called when the user clicks on the device back button or swipes to go back. Differentiated
     * from [onBackClick] which is called when the user presses the browser's back button.
     */
    fun onNavigateBack() {
        when {
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
            } else if (tabIdOpenedFromAction == currentTab?.id) {
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
     * TODO
     */
    fun onHomeClick() {
        currentTab?.loadFromInitializer(homePageInitializer)
    }

    /**
     * TODO
     */
    fun onNewTabClick() {
        createNewTabAndSelect(homePageInitializer, shouldSelect = true)
    }

    /**
     * Call when the user long clicks on the new tab button, indicating that they want to re-open
     * the last closed tab.
     */
    fun onNewTabLongClick() {
        compositeDisposable += model.reopenTab()
            .observeOn(mainScheduler)
            .subscribeBy { tab ->
                selectTab(model.selectTab(tab.id))
            }
    }

    /**
     * TODO
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
                    compositeDisposable += bookmarkPageFactory.buildPage()
                        .subscribeOn(diskScheduler)
                        .observeOn(mainScheduler)
                        .subscribeBy {
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
     * TODO
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
     * TODO
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
     * TODO
     */
    fun onFindInPage(query: String) {
        currentTab?.find(query)
        view?.updateState(viewState.copy(findInPage = query))
    }

    /**
     * TODO
     */
    fun onFindNext() {
        currentTab?.findNext()
    }

    /**
     * TODO
     */
    fun onFindPrevious() {
        currentTab?.findPrevious()
    }

    /**
     * TODO
     */
    fun onFindDismiss() {
        currentTab?.clearFindMatches()
        view?.updateState(viewState.copy(findInPage = ""))
    }

    /**
     * TODO
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
     * TODO
     */
    fun onSslIconClick() {
        currentTab?.sslCertificateInfo?.let {
            view?.showSslDialog(it)
        }
    }

    /**
     * TODO
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
                compositeDisposable += bookmarkRepository
                    .bookmarksAndFolders(folder = bookmark)
                    .subscribeOn(databaseScheduler)
                    .observeOn(mainScheduler)
                    .subscribe { list ->
                        view?.updateState(viewState.copy(bookmarks = list, isRootFolder = false))
                    }
            }
        }
    }

    private fun BookmarkRepository.bookmarksAndFolders(folder: Bookmark.Folder): Single<List<Bookmark>> =
        getBookmarksFromFolderSorted(folder = folder.title)
            .concatWith(Single.defer {
                if (folder == Bookmark.Folder.Root) {
                    getFoldersSorted()
                } else {
                    Single.just(emptyList())
                }
            })
            .toList()
            .map(MutableList<List<Bookmark>>::flatten)

    /**
     * TODO
     */
    fun onBookmarkLongClick(index: Int) {
        when (val item = viewState.bookmarks[index]) {
            is Bookmark.Entry -> view?.showBookmarkOptionsDialog(item)
            is Bookmark.Folder.Entry -> view?.showFolderOptionsDialog(item)
            Bookmark.Folder.Root -> Unit // Root is not clickable
        }
    }

    /**
     * TODO
     */
    fun onToolsClick() {
        val currentUrl = currentTab?.url ?: return
        view?.showToolsDialog(
            areAdsAllowed = allowListModel.isUrlAllowedAds(currentUrl),
            shouldShowAdBlockOption = !currentUrl.isSpecialUrl()
        )
    }

    fun onToggleDesktopAgent() {
        currentTab?.toggleDesktopAgent()
        currentTab?.reload()
    }

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
     * TODO
     */
    fun onStarClick() {
        val url = currentTab?.url ?: return
        val title = currentTab?.title.orEmpty()
        if (url.isSpecialUrl()) {
            return
        }
        compositeDisposable += bookmarkRepository.isBookmark(url)
            .flatMapMaybe {
                if (it) {
                    bookmarkRepository.deleteBookmark(
                        Bookmark.Entry(
                            url = url,
                            title = title,
                            position = 0,
                            folder = Bookmark.Folder.Root
                        )
                    ).toMaybe()
                } else {
                    Maybe.empty()
                }
            }
            .doOnComplete(::showAddBookmarkDialog)
            .flatMapSingleElement { bookmarkRepository.bookmarksAndFolders(folder = currentFolder) }
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribeBy { list ->
                this.view?.updateState(viewState.copy(bookmarks = list))
            }
    }

    private fun showAddBookmarkDialog() {
        compositeDisposable += bookmarkRepository.getFolderNames()
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribeBy {
                view?.showAddBookmarkDialog(
                    title = currentTab?.title.orEmpty(),
                    url = currentTab?.url.orEmpty(),
                    folders = it
                )
            }
    }

    fun onBookmarkConfirmed(title: String, url: String, folder: String) {
        compositeDisposable += bookmarkRepository.addBookmarkIfNotExists(
            Bookmark.Entry(
                url = url,
                title = title,
                position = 0,
                folder = folder.asFolder()
            )
        ).flatMap { bookmarkRepository.bookmarksAndFolders(folder = currentFolder) }
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribe { list ->
                this.view?.updateState(viewState.copy(bookmarks = list))
            }
    }

    fun onBookmarkEditConfirmed(title: String, url: String, folder: String) {
        compositeDisposable += bookmarkRepository.editBookmark(
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
        ).andThen(bookmarkRepository.bookmarksAndFolders(folder = currentFolder))
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribe { list ->
                this.view?.updateState(viewState.copy(bookmarks = list))
                if (currentTab?.url?.isBookmarkUrl() == true) {
                    reload()
                }
            }
    }

    fun onBookmarkFolderRenameConfirmed(oldTitle: String, newTitle: String) {
        compositeDisposable += bookmarkRepository.renameFolder(oldTitle, newTitle)
            .andThen(bookmarkRepository.bookmarksAndFolders(folder = currentFolder))
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribe { list ->
                this.view?.updateState(viewState.copy(bookmarks = list))
                if (currentTab?.url?.isBookmarkUrl() == true) {
                    reload()
                }
            }
    }

    /**
     * TODO
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
                compositeDisposable += bookmarkRepository.deleteBookmark(bookmark)
                    .flatMap { bookmarkRepository.bookmarksAndFolders(folder = currentFolder) }
                    .subscribeOn(databaseScheduler)
                    .observeOn(mainScheduler)
                    .subscribe { list ->
                        view?.updateState(viewState.copy(bookmarks = list))
                        if (currentTab?.url?.isBookmarkUrl() == true) {
                            reload()
                        }
                    }
            BrowserContract.BookmarkOptionEvent.EDIT ->
                compositeDisposable += bookmarkRepository.getFolderNames()
                    .subscribeOn(databaseScheduler)
                    .observeOn(mainScheduler)
                    .subscribeBy { folders ->
                        view?.showEditBookmarkDialog(
                            bookmark.title,
                            bookmark.url,
                            bookmark.folder.title,
                            folders
                        )
                    }
        }
    }

    fun onFolderOptionClick(folder: Bookmark.Folder, option: BrowserContract.FolderOptionEvent) {
        when (option) {
            BrowserContract.FolderOptionEvent.RENAME -> view?.showEditFolderDialog(folder.title)
            BrowserContract.FolderOptionEvent.REMOVE ->
                compositeDisposable += bookmarkRepository.deleteFolder(folder.title)
                    .andThen(bookmarkRepository.bookmarksAndFolders(folder = currentFolder))
                    .subscribeOn(databaseScheduler)
                    .observeOn(mainScheduler)
                    .subscribe { list ->
                        view?.updateState(viewState.copy(bookmarks = list))
                        if (currentTab?.url?.isBookmarkUrl() == true) {
                            reload()
                            currentTab?.goBack()
                        }
                    }
        }
    }

    fun onDownloadOptionClick(
        download: DownloadEntry,
        option: BrowserContract.DownloadOptionEvent
    ) {
        when (option) {
            BrowserContract.DownloadOptionEvent.DELETE ->
                compositeDisposable += downloadsRepository.deleteAllDownloads()
                    .subscribeOn(databaseScheduler)
                    .observeOn(mainScheduler)
                    .subscribeBy {
                        if (currentTab?.url?.isDownloadsUrl() == true) {
                            reload()
                        }
                    }
            BrowserContract.DownloadOptionEvent.DELETE_ALL ->
                compositeDisposable += downloadsRepository.deleteDownload(download.url)
                    .subscribeOn(databaseScheduler)
                    .observeOn(mainScheduler)
                    .subscribeBy {
                        if (currentTab?.url?.isDownloadsUrl() == true) {
                            reload()
                        }
                    }
        }
    }

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
                compositeDisposable += historyRepository.deleteHistoryEntry(historyEntry.url)
                    .subscribeOn(databaseScheduler)
                    .observeOn(mainScheduler)
                    .subscribeBy {
                        if (currentTab?.url?.isHistoryUrl() == true) {
                            reload()
                        }
                    }
        }
    }

    /**
     * TODO
     */
    fun onReadingModeClick() {
        currentTab?.url?.takeIf { !it.isSpecialUrl() }
            ?.let(navigator::openReaderMode)
    }

    /**
     * TODO
     */
    fun onTabCountViewClick() {
        if (uiConfiguration.tabConfiguration == TabConfiguration.DRAWER) {
            view?.openTabDrawer()
        } else {
            currentTab?.loadFromInitializer(homePageInitializer)
        }
    }

    /**
     * TODO
     */
    fun onTabMenuClick() {
        currentTab?.let {
            view?.showCloseBrowserDialog(it.id)
        }
    }

    /**
     * TODO
     */
    fun onBookmarkMenuClick() {
        if (currentFolder != Bookmark.Folder.Root) {
            currentFolder = Bookmark.Folder.Root
            compositeDisposable += bookmarkRepository
                .bookmarksAndFolders(folder = Bookmark.Folder.Root)
                .subscribeOn(databaseScheduler)
                .observeOn(mainScheduler)
                .subscribe { list ->
                    view?.updateState(viewState.copy(bookmarks = list, isRootFolder = true))
                }
        }
    }

    /**
     * TODO
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
                    compositeDisposable += bookmarkRepository.findBookmarkForUrl(url)
                        .subscribeOn(databaseScheduler)
                        .observeOn(mainScheduler)
                        .subscribeBy {
                            view?.showBookmarkOptionsDialog(it)
                        }
                }
            } else if (pageUrl.isDownloadsUrl()) {
                compositeDisposable += downloadsRepository.findDownloadForUrl(url)
                    .subscribeOn(databaseScheduler)
                    .observeOn(mainScheduler)
                    .subscribeBy {
                        view?.showDownloadOptionsDialog(it)
                    }
            } else if (pageUrl.isHistoryUrl()) {
                compositeDisposable += historyRepository.findHistoryEntriesContaining(url)
                    .subscribeOn(databaseScheduler)
                    .observeOn(mainScheduler)
                    .subscribeBy { entries ->
                        entries.firstOrNull()?.let {
                            view?.showHistoryOptionsDialog(it)
                        } ?: view?.showHistoryOptionsDialog(HistoryEntry(url = url, title = ""))
                    }

            }
            return
        }
        when (longPress.hitCategory) {
            LongPress.Category.IMAGE -> view?.showImageLongPressDialog(longPress)
            LongPress.Category.LINK -> view?.showLinkLongPressDialog(longPress)
            LongPress.Category.UNKNOWN -> Unit // Do nothing
        }
    }

    fun onCloseBrowserEvent(id: Int, closeTabEvent: BrowserContract.CloseTabEvent) {
        when (closeTabEvent) {
            BrowserContract.CloseTabEvent.CLOSE_CURRENT ->
                onTabClose(tabListState.tabIndexForId(id))
            BrowserContract.CloseTabEvent.CLOSE_OTHERS -> model.tabsList
                .filter { it.id != id }
                .toObservable()
                .flatMapCompletable { model.deleteTab(it.id) }
                .subscribeOn(mainScheduler)
                .subscribe()
            BrowserContract.CloseTabEvent.CLOSE_ALL ->
                compositeDisposable += model.deleteAllTabs().subscribeOn(mainScheduler)
                    .subscribeBy(onComplete = navigator::closeBrowser)
        }
    }

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

    private fun BrowserContract.View?.updateState(state: BrowserViewState) {
        viewState = state
        this?.renderState(viewState)
    }

    private fun BrowserContract.View?.updateTabs(tabs: List<TabViewState>) {
        tabListState = tabs
        this?.renderTabs(tabListState)
    }
}
