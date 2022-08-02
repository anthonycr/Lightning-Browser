package acr.browser.lightning._browser2.tab

import acr.browser.lightning._browser2.BrowserContract
import acr.browser.lightning._browser2.image.IconFreeze
import acr.browser.lightning._browser2.tab.bundle.BundleStore
import acr.browser.lightning.adblock.AdBlocker
import acr.browser.lightning.adblock.allowlist.AllowListModel
import acr.browser.lightning.browser.RecentTabModel
import acr.browser.lightning.di.DiskScheduler
import acr.browser.lightning.di.MainScheduler
import acr.browser.lightning.favicon.FaviconModel
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.view.*
import android.app.Application
import android.graphics.Bitmap
import android.os.Bundle
import io.reactivex.*
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

/**
 * Created by anthonycr on 9/12/20.
 */
class TabsRepository @Inject constructor(
    private val application: Application,
    private val webViewFactory: WebViewFactory,
    private val tabPager: TabPager,
    private val adBlocker: AdBlocker,
    private val allowListModel: AllowListModel,
    private val faviconModel: FaviconModel,
    @DiskScheduler private val diskScheduler: Scheduler,
    @MainScheduler private val mainScheduler: Scheduler,
    private val bundleStore: BundleStore,
    private val urlHandler: UrlHandler,
    private val userPreferences: UserPreferences,
    @DefaultUserAgent private val defaultUserAgent: String,
    @IconFreeze private val iconFreeze: Bitmap,
    private val recentTabModel: RecentTabModel
) : BrowserContract.Model {

    private var selectedTab: TabModel? = null
    private val tabsListObservable = PublishSubject.create<List<TabModel>>()

    override fun deleteTab(id: Int): Completable = Completable.fromAction {
        if (selectedTab?.id == id) {
            tabPager.clearTab()
        }
        val tab = tabsList.forId(id)
        recentTabModel.addClosedTab(tab.freeze())
        tab.destroy()
        tabsList = tabsList - tab
    }.doOnComplete {
        tabsListObservable.onNext(tabsList)
    }

    override fun deleteAllTabs(): Completable = Completable.fromAction {
        tabPager.clearTab()

        tabsList.forEach(TabModel::destroy)
        tabsList = emptyList()
    }.doOnComplete {
        tabsListObservable.onNext(tabsList)
    }

    override fun createTab(tabInitializer: TabInitializer): Single<TabModel> =
        Single.fromCallable<TabModel> {
            val webView = webViewFactory.createWebView()
            val headers = webViewFactory.createRequestHeaders()
            tabPager.addTab(webView)
            val tabAdapter = TabAdapter(
                tabInitializer,
                webView,
                headers,
                TabWebViewClient(adBlocker, allowListModel, urlHandler, headers),
                TabWebChromeClient(application, faviconModel, diskScheduler),
                userPreferences,
                defaultUserAgent,
                iconFreeze
            )

            tabsList = tabsList + tabAdapter

            return@fromCallable tabAdapter
        }.doOnSuccess {
            tabsListObservable.onNext(tabsList)
        }

    override fun reopenTab(): Maybe<TabModel> = Maybe.fromCallable(recentTabModel::lastClosed)
        .flatMapSingleElement { createTab(BundleInitializer(it)) }

    override fun selectTab(id: Int): TabModel {
        val selected = tabsList.forId(id)
        selectedTab = selected
        tabPager.selectTab(id)

        return selected
    }

    override var tabsList = emptyList<TabModel>()
        private set

    override fun tabsListChanges(): Observable<List<TabModel>> = tabsListObservable.hide()

    override fun initializeTabs(): Maybe<List<TabModel>> =
        Single.fromCallable(bundleStore::retrieve)
            .flatMapObservable { Observable.fromIterable(it) }
            .subscribeOn(diskScheduler)
            .observeOn(mainScheduler)
            .flatMapSingle(::createTab)
            .toList()
            .filter(MutableList<TabModel>::isNotEmpty)

    override fun freeze() {
        bundleStore.save(tabsList)
    }

    override fun clean() {
        bundleStore.deleteAll()
    }

    private fun List<TabModel>.forId(id: Int): TabModel = requireNotNull(find { it.id == id })
}
