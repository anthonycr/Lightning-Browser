package acr.browser.lightning._browser2.tab

import acr.browser.lightning._browser2.BrowserContract
import acr.browser.lightning.adblock.AdBlocker
import acr.browser.lightning.adblock.allowlist.AllowListModel
import acr.browser.lightning.view.HomePageInitializer
import acr.browser.lightning.view.TabInitializer
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

/**
 * Created by anthonycr on 9/12/20.
 */
class TabsRepository @Inject constructor(
    private val webViewFactory: WebViewFactory,
    private val tabPager: TabPager,
    private val adBlocker: AdBlocker,
    private val allowListModel: AllowListModel
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

    override fun initializeTabs(): Maybe<List<TabModel>> = Maybe.empty()

    private fun List<TabModel>.forId(id: Int): TabModel = requireNotNull(find { it.id == id })


}
