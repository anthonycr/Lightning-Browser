package acr.browser.lightning._browser2.tab

import acr.browser.lightning._browser2.BrowserContract
import acr.browser.lightning._browser2.tab.bundle.BundleStore
import acr.browser.lightning.adblock.AdBlocker
import acr.browser.lightning.adblock.allowlist.AllowListModel
import acr.browser.lightning.di.DiskScheduler
import acr.browser.lightning.di.MainScheduler
import acr.browser.lightning.view.*
import io.reactivex.*
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

/**
 * Created by anthonycr on 9/12/20.
 */
class TabsRepository @Inject constructor(
    private val webViewFactory: WebViewFactory,
    private val tabPager: TabPager,
    private val adBlocker: AdBlocker,
    private val allowListModel: AllowListModel,
    @DiskScheduler private val diskScheduler: Scheduler,
    @MainScheduler private val mainScheduler: Scheduler,
    private val bundleStore: BundleStore
) : BrowserContract.Model {

    private var selectedTab: TabModel? = null
    private val tabsListObservable = PublishSubject.create<List<TabModel>>()

    override fun deleteTab(id: Int): Completable = Completable.fromAction {
        if (selectedTab?.id == id) {
            tabPager.clearTab()
        }
        val tab = tabsList.forId(id)
        tab.destroy()
        tabsList -= tab
    }.doOnComplete {
        tabsListObservable.onNext(tabsList)
    }

    override fun createTab(tabInitializer: TabInitializer): Single<TabModel> = Single.fromCallable<TabModel> {
        val webView = webViewFactory.createWebView(isIncognito = false)
        tabPager.addTab(webView)
        val tabAdapter = TabAdapter(
            tabInitializer,
            webView,
            webViewFactory.createRequestHeaders(),
            TabWebViewClient(adBlocker, allowListModel),
            TabWebChromeClient()
        )

        tabsList += tabAdapter

        return@fromCallable tabAdapter
    }.doOnSuccess {
        tabsListObservable.onNext(tabsList)
    }

    override fun selectTab(id: Int): TabModel {
        val selected = tabsList.forId(id)
        selectedTab = selected
        tabPager.selectTab(id)

        return selected
    }

    override var tabsList = emptyList<TabModel>()
        private set

    override fun tabsListChanges(): Observable<List<TabModel>> = tabsListObservable.hide()

    override fun initializeTabs(): Maybe<List<TabModel>> = Single.fromCallable(bundleStore::retrieve)
        .flatMapObservable { Observable.fromIterable(it) }
        .subscribeOn(diskScheduler)
        .observeOn(mainScheduler)
        .flatMapSingle(::createTab)
        .toList()
        .filter { it.isNotEmpty() }

    override fun freeze() {
        bundleStore.save(tabsList)
    }

    private fun List<TabModel>.forId(id: Int): TabModel = requireNotNull(find { it.id == id })
}
