package acr.browser.lightning.browser.tab

import acr.browser.lightning.browser.BrowserContract
import acr.browser.lightning.browser.tab.bundle.BundleStore
import acr.browser.lightning.browser.tab.settings.TabSettings
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.di.InitialAction
import acr.browser.lightning.ids.ViewIdGenerator
import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.search.SearchEngineProvider
import acr.browser.lightning.search.engine.search
import acr.browser.lightning.useragent.UserAgentProvider
import acr.browser.lightning.utils.isFileUrl
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
    private val bundleStore: BundleStore,
    private val recentTabModel: RecentTabModel,
    private val tabFactory: TabFactory,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val userAgentProvider: UserAgentProvider,
    @InitialAction private val initialAction: BrowserContract.Action?,
    private val permissionInitializerFactory: PermissionInitializer.Factory,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val searchEngineProvider: SearchEngineProvider,
    private val viewIdGenerator: ViewIdGenerator,
) : BrowserContract.Model {

    private val isInitialized = CompletableDeferred<Unit>()
    private val tabsListStateFlow = MutableStateFlow<List<TabModel>>(emptyList())

    override var selectedTab: TabModel? = null

    override suspend fun deleteTab(id: Int): Unit = withContext(coroutineDispatchers.main) {
        if (selectedTab?.id == id) {
            tabPager.clearTab(id)
        } else {
            tabPager.removeTab(id)
        }
        val tab = tabsList.forId(id)
        recentTabModel.addClosedTab(tab.freeze())
        tab.destroy()
        tabsList = tabsList - tab

        tabsListStateFlow.emit(tabsList)
    }

    override suspend fun deleteAllTabs(): Unit = withContext(coroutineDispatchers.main) {
        isInitialized.await()
        tabPager.clearAllTabs()

        tabsList.forEach(TabModel::destroy)
        tabsList = emptyList()

        tabsListStateFlow.emit(tabsList)
    }

    override suspend fun createTab(
        tabInitializer: TabInitializer,
        tabType: TabModel.Type
    ): TabModel = withContext(coroutineDispatchers.main) {
        isInitialized.await()
        createTabUnsafe(tabInitializer, tabType)
    }

    private fun TabInitializer.tabId(): Int = if (this is FreezableInitializer) {
        val frozenId = this.id.takeIf { it != -1 }?.also {
            viewIdGenerator.claimViewId(it)
        } ?: viewIdGenerator.generateViewId()
        frozenId
    } else {
        viewIdGenerator.generateViewId()
    }

    /**
     * Creates a tab without waiting for the browser to be initialized.
     */
    private suspend fun createTabUnsafe(
        tabInitializer: TabInitializer,
        tabType: TabModel.Type,
        emitUpdate: Boolean = true,
    ): TabModel = withContext(coroutineDispatchers.main) {
        val id = tabInitializer.tabId()
        val tabSettings = TabSettings.create(userPreferencesDataStore, userAgentProvider)
        val webViewLazy = webViewFactory.createWebView(tabSettings)
        val tabModel = tabFactory.constructTab(
            id = id,
            tabInitializer = tabInitializer,
            webView = webViewLazy,
            tabType = tabType,
            tabSettings = tabSettings
        )
        tabPager.addTab(tabModel.id, webViewLazy)
        tabsList = tabsList + tabModel

        if (emitUpdate) {
            tabsListStateFlow.emit(tabsList)
        }

        tabModel
    }

    override suspend fun reopenTab(): TabModel? = withContext(coroutineDispatchers.main) {
        recentTabModel.lastClosed()?.let { createTab(BundleInitializer(it)) }
    }

    override fun selectTab(id: Int): TabModel {
        val selected = tabsList.forId(id)
        selectedTab = selected
        tabPager.selectTab(id)

        return selected
    }

    override var tabsList = emptyList<TabModel>()
        private set

    override fun tabsListChanges(): Flow<List<TabModel>> = tabsListStateFlow

    override suspend fun initializeTabs(): List<TabModel> =
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

    private fun List<TabModel>.forId(id: Int): TabModel = requireNotNull(find { it.id == id })
}
