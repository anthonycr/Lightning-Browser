package acr.browser.lightning.browser.tab

import acr.browser.lightning.browser.BrowserContract
import acr.browser.lightning.browser.di.DiskScheduler
import acr.browser.lightning.browser.di.InitialUrl
import acr.browser.lightning.browser.di.MainScheduler
import acr.browser.lightning.browser.tab.bundle.BundleStore
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.utils.isFileUrl
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

/**
 * The repository for tabs that implements the [BrowserContract.Model] interface. Manages the state
 * of the tabs list and adding new tabs to it or removing tabs from it.
 */
class TabsRepository @Inject constructor(
    private val webViewFactory: WebViewFactory,
    private val tabPager: TabPager,
    @DiskScheduler private val diskScheduler: Scheduler,
    @MainScheduler private val mainScheduler: Scheduler,
    private val bundleStore: BundleStore,
    private val recentTabModel: RecentTabModel,
    private val tabFactory: TabFactory,
    private val userPreferences: UserPreferences,
    @InitialUrl private val initialUrl: String?,
    private val permissionInitializerFactory: PermissionInitializer.Factory
) : BrowserContract.Model {

    private var isInitialized = BehaviorSubject.createDefault(false)
    private var selectedTab: TabModel? = null
    private val tabsListObservable = PublishSubject.create<List<TabModel>>()

    private fun afterInitialization(): Single<Boolean> =
        isInitialized.filter { it }.firstOrError()

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
    }.subscribeOn(mainScheduler)

    override fun deleteAllTabs(): Completable =
        afterInitialization().flatMapCompletable {
            Completable.fromAction {
                tabPager.clearTab()

                tabsList.forEach(TabModel::destroy)
                tabsList = emptyList()
            }
        }.doOnComplete {
            tabsListObservable.onNext(tabsList)
        }.subscribeOn(mainScheduler)

    override fun createTab(tabInitializer: TabInitializer): Single<TabModel> =
        afterInitialization().flatMap { createTabUnsafe(tabInitializer) }
            .subscribeOn(mainScheduler)

    /**
     * Creates a tab without waiting for the browser to be initialized.
     */
    private fun createTabUnsafe(tabInitializer: TabInitializer): Single<TabModel> =
        Single.fromCallable {
            val webView = webViewFactory.createWebView()
            tabPager.addTab(webView)
            val tabAdapter = tabFactory.constructTab(tabInitializer, webView)

            tabsList = tabsList + tabAdapter

            return@fromCallable tabAdapter
        }.doOnSuccess {
            tabsListObservable.onNext(tabsList)
        }.subscribeOn(mainScheduler)

    override fun reopenTab(): Maybe<TabModel> = Maybe.fromCallable(recentTabModel::lastClosed)
        .flatMapSingleElement { createTab(BundleInitializer(it)) }
        .subscribeOn(mainScheduler)

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
            .subscribeOn(diskScheduler)
            .observeOn(mainScheduler)
            .flatMapObservable { Observable.fromIterable(it) }
            .concatWith(Maybe.fromCallable { initialUrl }.map {
                if (it.isFileUrl()) {
                    permissionInitializerFactory.create(it)
                } else {
                    UrlInitializer(it)
                }
            })
            .flatMapSingle(::createTabUnsafe)
            .toList()
            .filter(List<TabModel>::isNotEmpty)
            .doAfterSuccess {
                isInitialized.onNext(true)
            }

    override fun freeze() {
        if (userPreferences.restoreLostTabsEnabled) {
            bundleStore.save(tabsList)
        }
    }

    override fun clean() {
        bundleStore.deleteAll()
    }

    private fun List<TabModel>.forId(id: Int): TabModel = requireNotNull(find { it.id == id })
}
