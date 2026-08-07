package acr.browser.lightning.browser.tab

import acr.browser.lightning.browser.BrowserContract
import acr.browser.lightning.browser.tab.bundle.BundleStore
import acr.browser.lightning.browser.tab.settings.TabSettings
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.di.InitialAction
import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.search.SearchEngineProvider
import acr.browser.lightning.search.engine.search
import acr.browser.lightning.tab.TabModel
import acr.browser.lightning.tab.initializer.TabInitializer
import acr.browser.lightning.useragent.UserAgentProvider
import acr.browser.lightning.utils.isFileUrl
import android.webkit.WebView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * The repository for tabs that implements the [BrowserContract.Model] interface. Manages the state
 * of the tabs list and adding new tabs to it or removing tabs from it.
 */
class TabsRepository @Inject constructor(
    private val webViewFactory: WebViewFactory,
    private val tabPager: TabPager,
    private val bundleStore: BundleStore<WebView>,
    private val recentTabModel: RecentTabModel,
    private val tabFactory: TabFactory,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val userAgentProvider: UserAgentProvider,
    @InitialAction private val initialAction: BrowserContract.Action?,
    private val permissionInitializerFactory: PermissionInitializer.Factory,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val searchEngineProvider: SearchEngineProvider,
) : BrowserContract.Model<WebView> {

    private val isInitialized = CompletableDeferred<Unit>()
    private val tabsListStateFlow = MutableStateFlow<List<TabModel<WebView>>>(emptyList())

    override var selectedTab: TabModel<WebView>? = null

    override suspend fun deleteTab(id: Int): Unit = withContext(coroutineDispatchers.main) {
        if (selectedTab?.id == id) {
            tabPager.clearTab()
        }
        val tab = tabsList.forId(id)
        recentTabModel.addClosedTab(tab.freeze())
        tab.destroy()
        tabsList = tabsList - tab

        tabsListStateFlow.emit(tabsList)
    }

    override suspend fun deleteAllTabs(): Unit = withContext(coroutineDispatchers.main) {
        isInitialized.await()
        tabPager.clearTab()

        tabsList.forEach(TabModel<WebView>::destroy)
        tabsList = emptyList()

        tabsListStateFlow.emit(tabsList)
    }

    override suspend fun createTab(
        tabInitializer: TabInitializer<WebView>,
        tabType: TabModel.Type
    ): TabModel<WebView> = withContext(coroutineDispatchers.main) {
        isInitialized.await()
        createTabUnsafe(tabInitializer, tabType)
    }

    /**
     * Creates a tab without waiting for the browser to be initialized.
     */
    private suspend fun createTabUnsafe(
        tabInitializer: TabInitializer<WebView>,
        tabType: TabModel.Type,
        emitUpdate: Boolean = true,
    ): TabModel<WebView> = withContext(coroutineDispatchers.main) {
        val tabSettings = TabSettings.create(userPreferencesDataStore, userAgentProvider)
        val webViewLazy = webViewFactory.createWebView(tabSettings)
        val tabModel = tabFactory.constructTab(tabInitializer, webViewLazy, tabType, tabSettings)
        tabPager.addTab(tabModel.id, webViewLazy)
        tabsList = tabsList + tabModel

        if (emitUpdate) {
            tabsListStateFlow.emit(tabsList)
        }

        tabModel
    }

    override suspend fun reopenTab(): TabModel<WebView>? = withContext(coroutineDispatchers.main) {
        recentTabModel.lastClosed()?.let { createTab(BundleInitializer(it)) }
    }

    override fun selectTab(id: Int): TabModel<WebView> {
        val selected = tabsList.forId(id)
        selectedTab = selected
        tabPager.selectTab(id)

        return selected
    }

    override var tabsList = emptyList<TabModel<WebView>>()
        private set

    override fun tabsListChanges(): Flow<List<TabModel<WebView>>> = tabsListStateFlow

    override suspend fun initializeTabs(): List<TabModel<WebView>> =
        withContext(coroutineDispatchers.default) {
            val oldTabs = bundleStore.retrieve().map {
                async {
                    createTabUnsafe(
                        tabInitializer = it,
                        tabType = TabModel.Type.NORMAL,
                        emitUpdate = false
                    )
                }
            }

            val initialUrl = when (initialAction) {
                is BrowserContract.Action.LoadUrl -> initialAction.url
                is BrowserContract.Action.Search -> searchEngineProvider.provideSearchEngine()
                    .search(initialAction.query)

                else -> null
            }
            val newTabInitializer = if (initialUrl != null && initialUrl.isFileUrl()) {
                permissionInitializerFactory.create(initialUrl)
            } else if (initialUrl != null) {
                UrlInitializer(initialUrl)
            } else {
                null
            }

            val newTab = newTabInitializer?.let {
                createTabUnsafe(
                    tabInitializer = it,
                    tabType = TabModel.Type.EPHEMERAL,
                    emitUpdate = false
                )
            }

            tabsList = if (newTab != null) {
                oldTabs.awaitAll() + newTab
            } else {
                oldTabs.awaitAll()
            }
            isInitialized.complete(Unit)

            tabsListStateFlow.emit(tabsList)

            tabsList
        }

    override fun markAllNonEphemeral() {
        tabsList.forEach { it.tabType = TabModel.Type.NORMAL }
    }

    override suspend fun freeze() {
        if (userPreferencesDataStore.restoreLostTabsEnabled.get()) {
            bundleStore.save(tabsList)
        }
    }

    override suspend fun clean() {
        bundleStore.deleteAll()
    }

    private fun List<TabModel<WebView>>.forId(id: Int): TabModel<WebView> = requireNotNull(find { it.id == id })
}
