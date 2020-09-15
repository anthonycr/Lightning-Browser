package acr.browser.lightning._browser2.tab

import acr.browser.lightning._browser2.BrowserContract
import acr.browser.lightning.view.UrlInitializer
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject

/**
 * Created by anthonycr on 9/12/20.
 */
class TabsRepository(
    private val webViewFactory: WebViewFactory,
    private val tabPager: TabPager
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

    override fun createTab(initialUrl: String?): Single<TabModel> = Single.fromCallable<TabModel> {
        val webView = webViewFactory.createWebView(isIncognito = false)
        tabPager.addTab(webView)
        val tabAdapter = TabAdapter(UrlInitializer(initialUrl ?: "https://google.com"), webView)

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
