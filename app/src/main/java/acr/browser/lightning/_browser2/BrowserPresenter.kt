package acr.browser.lightning._browser2

import acr.browser.lightning._browser2.di.InitialUrl
import acr.browser.lightning._browser2.history.HistoryRecord
import acr.browser.lightning._browser2.keys.KeyCombo
import acr.browser.lightning._browser2.menu.MenuSelection
import acr.browser.lightning._browser2.tab.TabViewState
import acr.browser.lightning._browser2.tab.TabModel
import acr.browser.lightning.browser.SearchBoxModel
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.HistoryEntry
import acr.browser.lightning.database.SearchSuggestion
import acr.browser.lightning.database.WebPage
import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.di.DatabaseScheduler
import acr.browser.lightning.di.MainScheduler
import acr.browser.lightning.search.SearchEngineProvider
import acr.browser.lightning.ssl.SslState
import acr.browser.lightning.utils.*
import acr.browser.lightning.view.DownloadPageInitializer
import acr.browser.lightning.view.HistoryPageInitializer
import acr.browser.lightning.view.HomePageInitializer
import acr.browser.lightning.view.UrlInitializer
import android.graphics.Bitmap
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import java.util.*
import javax.inject.Inject

/**
 * Created by anthonycr on 9/11/20.
 */
class BrowserPresenter @Inject constructor(
    private val model: BrowserContract.Model,
    private val navigator: BrowserContract.Navigator,
    private val bookmarkRepository: BookmarkRepository,
    @MainScheduler private val mainScheduler: Scheduler,
    @DatabaseScheduler private val databaseScheduler: Scheduler,
    private val historyRecord: HistoryRecord,
    private val homePageInitializer: HomePageInitializer,
    private val historyPageInitializer: HistoryPageInitializer,
    private val downloadPageInitializer: DownloadPageInitializer,
    private val searchBoxModel: SearchBoxModel,
    private val searchEngineProvider: SearchEngineProvider,
    @InitialUrl private val initialUrl: String?
) {

    private var view: BrowserContract.View? = null
    private var viewState: BrowserViewState = BrowserViewState(
        displayUrl = "",
        isRefresh = true,
        sslState = SslState.None,
        progress = 0,
        tabs = emptyList(),
        isForwardEnabled = false,
        isBackEnabled = false,
        bookmarks = emptyList(),
        isBookmarked = false,
        isBookmarkEnabled = true
    )
    private var currentTab: TabModel? = null
    private var currentFolder: Bookmark.Folder = Bookmark.Folder.Root
    private var isSearchViewFocused = false

    private val compositeDisposable = CompositeDisposable()
    private val allTabsDisposable = CompositeDisposable()
    private var tabDisposable: Disposable? = null

    /**
     * TODO
     */
    fun onViewAttached(view: BrowserContract.View) {
        this.view = view
        view.updateState(viewState)

        compositeDisposable += bookmarkRepository.getBookmarksFromFolderSorted(folder = null)
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribe { list ->
                this.view?.updateState(viewState.copy(bookmarks = list))
            }

        compositeDisposable += model.tabsListChanges()
            .observeOn(mainScheduler)
            .subscribe { list ->
                this.view.updateState(viewState.copy(tabs = list.map { it.asViewState() }))

                allTabsDisposable.clear()
                list.subscribeToUpdates(allTabsDisposable)
            }

        compositeDisposable += model.initializeTabs()
            .observeOn(mainScheduler)
            .mergeWith(
                Maybe.fromCallable { initialUrl }
                    .flatMapSingleElement { model.createTab(UrlInitializer(it)) }
                    .map { listOf(it) }
            )
            .toList()
            .map { it.flatten() }
            .filter { it.isNotEmpty() }
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

        model.freeze()

        compositeDisposable.dispose()
        tabDisposable?.dispose()
    }

    private fun TabModel.asViewState(): TabViewState = TabViewState(
        id = id,
        icon = favicon,
        title = title,
        isSelected = isForeground
    )

    private fun List<TabViewState>.updateId(id: Int, map: (TabViewState) -> TabViewState): List<TabViewState> = map {
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

        val tab = tabModel ?: return view.updateState(viewState.copy(
            displayUrl = searchBoxModel.getDisplayContent(
                url = "",
                title = null,
                isLoading = false
            ),
            isForwardEnabled = false,
            isBackEnabled = false,
            sslState = SslState.None,
            progress = 100,
            tabs = viewState.tabs.map {
                it.copy(isSelected = false)
            }
        ))

        tabDisposable?.dispose()
        tabDisposable = Observables.combineLatest(
            tab.sslChanges().startWith(tab.sslState),
            tab.titleChanges().startWith(tab.title),
            tab.urlChanges().startWith(tab.url),
            tab.loadingProgress().startWith(tab.loadingProgress),
            tab.canGoBackChanges().startWith(tab.canGoBack()),
            tab.canGoForwardChanges().startWith(tab.canGoForward())
        ) { sslState, title, url, progress, canGoBack, canGoForward ->
            viewState.copy(
                displayUrl = searchBoxModel.getDisplayContent(
                    url = url,
                    title = title,
                    isLoading = progress < 100
                ),
                isRefresh = progress == 100,
                isForwardEnabled = canGoForward,
                isBackEnabled = canGoBack,
                sslState = sslState,
                progress = progress,
                tabs = viewState.tabs.map { it.copy(isSelected = it.id == tabModel.id) }
            )
        }.subscribeOn(mainScheduler)
            .subscribe { view.updateState(it) }
    }

    private fun List<TabModel>.subscribeToUpdates(compositeDisposable: CompositeDisposable) {
        forEach { tabModel ->
            compositeDisposable += Observables.combineLatest(
                tabModel.titleChanges().startWith(tabModel.title),
                tabModel.faviconChanges().optional().startWith(Option.fromNullable(tabModel.favicon))
            ).distinctUntilChanged()
                .subscribeOn(mainScheduler)
                .subscribeBy { (title, bitmap) ->
                    view.updateState(viewState.copy(tabs = viewState.tabs.updateId(tabModel.id) {
                        it.copy(title = title, icon = bitmap.value())
                    }))

                    tabModel.url.takeIf { !it.isSpecialUrl() }?.let {
                        historyRecord.recordVisit(title, it)
                    }
                }
        }
    }

    private fun <T> Observable<T>.optional(): Observable<Option<T>> = map { Option.Some(it) }

    /**
     * TODO
     */
    fun onNewDeepLink(url: String) {
        compositeDisposable += model.createTab(UrlInitializer(url))
            .observeOn(mainScheduler)
            .subscribe { tab ->
                selectTab(model.selectTab(tab.id))
            }
    }

    /**
     * TODO
     */
    fun onMenuClick(menuSelection: MenuSelection) {
        when (menuSelection) {
            MenuSelection.NEW_TAB -> onNewTabClick()
            MenuSelection.NEW_INCOGNITO_TAB -> TODO()
            MenuSelection.SHARE -> currentTab?.url?.takeIf { !it.isSpecialUrl() }?.let {
                navigator.sharePage(url = it, title = currentTab?.title)
            }
            MenuSelection.HISTORY ->
                compositeDisposable += model.createTab(historyPageInitializer)
                    .observeOn(mainScheduler)
                    .subscribe { tab ->
                        selectTab(model.selectTab(tab.id))
                    }
            MenuSelection.DOWNLOADS ->
                compositeDisposable += model.createTab(downloadPageInitializer)
                    .observeOn(mainScheduler)
                    .subscribe { tab ->
                        selectTab(model.selectTab(tab.id))
                    }
            MenuSelection.FIND -> TODO()
            MenuSelection.COPY_LINK -> currentTab?.url?.takeIf { !it.isSpecialUrl() }
                ?.let(navigator::copyPageLink)
            MenuSelection.ADD_TO_HOME -> TODO()
            MenuSelection.BOOKMARKS -> TODO()
            MenuSelection.ADD_BOOKMARK -> TODO()
            MenuSelection.READER -> currentTab?.url?.takeIf { !it.isSpecialUrl() }
                ?.let(navigator::openReaderMode)
            MenuSelection.SETTINGS -> navigator.openSettings()
        }
    }

    /**
     * TODO
     */
    fun onKeyComboClick(keyCombo: KeyCombo) {
        when (keyCombo) {
            KeyCombo.CTRL_F -> TODO()
            KeyCombo.CTRL_T -> onNewTabClick()
            KeyCombo.CTRL_W -> TODO()
            KeyCombo.CTRL_Q -> TODO()
            KeyCombo.CTRL_R -> onRefreshOrStopClick()
            KeyCombo.CTRL_TAB -> TODO()
            KeyCombo.CTRL_SHIFT_TAB -> TODO()
            KeyCombo.SEARCH -> TODO()
            KeyCombo.ALT_0 -> TODO()
            KeyCombo.ALT_1 -> TODO()
            KeyCombo.ALT_2 -> TODO()
            KeyCombo.ALT_3 -> TODO()
            KeyCombo.ALT_4 -> TODO()
            KeyCombo.ALT_5 -> TODO()
            KeyCombo.ALT_6 -> TODO()
            KeyCombo.ALT_7 -> TODO()
            KeyCombo.ALT_8 -> TODO()
            KeyCombo.ALT_9 -> TODO()
        }
    }

    /**
     * TODO
     */
    fun onTabClick(index: Int) {
        selectTab(model.selectTab(viewState.tabs[index].id))
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
        val nextTab = viewState.tabs.nextSelected(index)

        val needToSelectNextTab = viewState.tabs[index].id == currentTab?.id

        compositeDisposable += model.deleteTab(viewState.tabs[index].id)
            .observeOn(mainScheduler)
            .subscribe {
                if (needToSelectNextTab) {
                    nextTab?.id?.let {
                        selectTab(model.selectTab(it))
                    } ?: selectTab(tabModel = null)
                }
            }
    }

    /**
     * TODO
     */
    fun onBackClick() {
        if (currentTab?.canGoBack() == true) {
            currentTab?.goBack()
        }
    }

    /**
     * TODO
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
        compositeDisposable += model.createTab(homePageInitializer)
            .observeOn(mainScheduler)
            .subscribe { tab ->
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
            currentTab?.reload()
        }
    }

    /**
     * TODO
     */
    fun onSearchFocusChanged(isFocused: Boolean) {
        isSearchViewFocused = isFocused
        if (isFocused) {
            view?.updateState(viewState.copy(sslState = SslState.None, isRefresh = false))
        } else {
            view?.updateState(viewState.copy(
                sslState = currentTab?.sslState ?: SslState.None,
                isRefresh = (currentTab?.loadingProgress ?: 0) == 100,
                displayUrl = searchBoxModel.getDisplayContent(
                    url = currentTab?.url.orEmpty(),
                    title = currentTab?.title.orEmpty(),
                    isLoading = (currentTab?.loadingProgress ?: 0) < 100
                )
            ))
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
        view?.updateState(viewState.copy(
            displayUrl = searchBoxModel.getDisplayContent(
                url = url,
                title = currentTab?.title,
                isLoading = (currentTab?.loadingProgress ?: 0) < 100
            )
        ))
        currentTab?.loadUrl(url)
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
        // TODO
    }

    /**
     * TODO
     */
    fun onBookmarkClick(index: Int) {
        when (val bookmark = viewState.bookmarks[index]) {
            is Bookmark.Entry -> currentTab?.loadUrl(bookmark.url)
            Bookmark.Folder.Root -> TODO()
            is Bookmark.Folder.Entry -> {
                currentFolder = bookmark
                compositeDisposable += bookmarkRepository
                    .getBookmarksFromFolderSorted(folder = bookmark.title)
                    .subscribeOn(databaseScheduler)
                    .observeOn(mainScheduler)
                    .subscribe { list ->
                        view?.updateState(viewState.copy(bookmarks = list))
                    }
            }
        }
    }

    /**
     * TODO
     */
    fun onBookmarkLongClick(index: Int) {

    }

    /**
     * TODO
     */
    fun onToolsClick() {

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
            .flatMap {
                if (it) {
                    bookmarkRepository.deleteBookmark(Bookmark.Entry(
                        url = url,
                        title = title,
                        position = 0,
                        folder = Bookmark.Folder.Root
                    ))
                } else {
                    bookmarkRepository.addBookmarkIfNotExists(Bookmark.Entry(
                        url = url,
                        title = title,
                        position = 0,
                        folder = Bookmark.Folder.Root
                    ))
                }
            }
            .flatMap { bookmarkRepository.getBookmarksFromFolderSorted(folder = currentFolder.title) }
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribe { list ->
                this.view?.updateState(viewState.copy(bookmarks = list))
            }
    }

    /**
     * TODO
     */
    fun onReadingModeClick() {

    }

    /**
     * TODO
     */
    fun onTabMenuClick() {

    }

    /**
     * TODO
     */
    fun onBookmarkMenuClick() {
        if (currentFolder != Bookmark.Folder.Root) {
            compositeDisposable += bookmarkRepository
                .getBookmarksFromFolderSorted(folder = null)
                .subscribeOn(databaseScheduler)
                .observeOn(mainScheduler)
                .subscribe { list ->
                    view?.updateState(viewState.copy(bookmarks = list))
                }
        }
    }

    private fun BrowserContract.View?.updateState(state: BrowserViewState) {
        viewState = state
        this?.renderState(viewState)
    }
}
