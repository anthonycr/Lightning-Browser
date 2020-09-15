package acr.browser.lightning._browser2

import acr.browser.lightning._browser2.history.HistoryRecord
import acr.browser.lightning._browser2.tab.Tab
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
import acr.browser.lightning.utils.QUERY_PLACE_HOLDER
import acr.browser.lightning.utils.isSpecialUrl
import acr.browser.lightning.utils.smartUrlFilter
import acr.browser.lightning.view.HomePageInitializer
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import javax.inject.Inject

/**
 * Created by anthonycr on 9/11/20.
 */
class BrowserPresenter @Inject constructor(
    private val model: BrowserContract.Model,
    private val bookmarkRepository: BookmarkRepository,
    @MainScheduler private val mainScheduler: Scheduler,
    @DatabaseScheduler private val databaseScheduler: Scheduler,
    private val historyRecord: HistoryRecord,
    private val homePageInitializer: HomePageInitializer,
    private val searchBoxModel: SearchBoxModel,
    private val searchEngineProvider: SearchEngineProvider
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
        isBookmarked = false
    )
    private var currentTab: TabModel? = null
    private var currentFolder: Bookmark.Folder = Bookmark.Folder.Root

    private val compositeDisposable = CompositeDisposable()
    private var sslDisposable: Disposable? = null
    private var titleDisposable: Disposable? = null
    private var urlDisposable: Disposable? = null
    private var loadingDisposable: Disposable? = null
    private var canGoBackDisposable: Disposable? = null
    private var canGoForwardDisposable: Disposable? = null

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

        compositeDisposable += model.initializeTabs()
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .switchIfEmpty(model.createTab(homePageInitializer).map(::listOf))
            .subscribe { list ->
                this.view.updateState(viewState.copy(tabs = list.map { it.asViewState() }))
                selectTab(model.selectTab(list.last().id))
            }

        compositeDisposable += model.tabsListChanges()
            .observeOn(mainScheduler)
            .subscribe { list ->
                this.view.updateState(viewState.copy(tabs = list.map { it.asViewState() }))
            }
    }

    private fun TabModel.asViewState(): Tab = Tab(
        id = id,
        icon = "",
        title = title,
        isSelected = isForeground
    )

    private fun List<Tab>.updateId(id: Int, map: (Tab) -> Tab): List<Tab> = map {
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

        view.updateState(viewState.copy(
            displayUrl = searchBoxModel.getDisplayContent(
                url = tabModel?.url.orEmpty(),
                title = tabModel?.title,
                isLoading = (tabModel?.loadingProgress ?: 0) < 100
            ),
            isForwardEnabled = tabModel?.canGoForward() ?: false,
            isBackEnabled = tabModel?.canGoBack() ?: false,
            sslState = tabModel?.sslState ?: SslState.None,
            progress = tabModel?.loadingProgress ?: 100,
            tabs = viewState.tabs.map {
                if (it.id == tabModel?.id) {
                    it.copy(isSelected = true)
                } else {
                    it.copy(isSelected = false)
                }
            }
        ))

        sslDisposable?.dispose()
        sslDisposable = tabModel?.sslChanges()
            ?.distinctUntilChanged()
            ?.observeOn(mainScheduler)
            ?.subscribe {
                view.updateState(viewState.copy(sslState = it))
            }

        titleDisposable?.dispose()
        titleDisposable = tabModel?.titleChanges()
            ?.distinctUntilChanged()
            ?.observeOn(mainScheduler)
            ?.subscribe { title ->
                view.updateState(viewState.copy(tabs = viewState.tabs.updateId(tabModel.id) {
                    it.copy(title = title)
                }))

                currentTab?.url?.takeIf { !it.isSpecialUrl() }?.let {
                    historyRecord.recordVisit(title, it)
                }
            }

        urlDisposable?.dispose()
        urlDisposable = tabModel?.urlChanges()
            ?.distinctUntilChanged()
            ?.observeOn(mainScheduler)
            ?.subscribe {
                view.updateState(viewState.copy(
                    displayUrl = searchBoxModel.getDisplayContent(
                        url = tabModel.url,
                        title = tabModel.title,
                        isLoading = (tabModel.loadingProgress ?: 0) < 100
                    )
                ))
            }

        loadingDisposable?.dispose()
        loadingDisposable = tabModel?.loadingProgress()
            ?.distinctUntilChanged()
            ?.observeOn(mainScheduler)
            ?.subscribe {
                view.updateState(viewState.copy(progress = it, isRefresh = it == 100))
            }

        canGoBackDisposable?.dispose()
        canGoBackDisposable = tabModel?.canGoBackChanges()
            ?.distinctUntilChanged()
            ?.observeOn(mainScheduler)
            ?.subscribe {
                view.updateState(viewState.copy(isBackEnabled = it))
            }

        canGoForwardDisposable?.dispose()
        canGoForwardDisposable = tabModel?.canGoForwardChanges()
            ?.distinctUntilChanged()
            ?.observeOn(mainScheduler)
            ?.subscribe {
                view.updateState(viewState.copy(isForwardEnabled = it))
            }
    }

    /**
     * TODO
     */
    fun onViewDetached() {
        view = null
        compositeDisposable.dispose()

        sslDisposable?.dispose()
        titleDisposable?.dispose()
        urlDisposable?.dispose()
        loadingDisposable?.dispose()
        canGoBackDisposable?.dispose()
        canGoForwardDisposable?.dispose()
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
        currentTab?.loadUrl("https://google.com")
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
        if (currentTab?.loadingProgress != 100) {
            currentTab?.stopLoading()
        } else {
            currentTab?.reload()
        }
    }

    /**
     * TODO
     */
    fun onSearch(query: String) {
        currentTab?.stopLoading()
        val searchUrl = searchEngineProvider.provideSearchEngine().queryUrl + QUERY_PLACE_HOLDER
        val url = smartUrlFilter(query, true, searchUrl)
        view?.updateState(viewState.copy(
            displayUrl = searchBoxModel.getDisplayContent(
                url = currentTab?.url.orEmpty(),
                title = currentTab?.url,
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
