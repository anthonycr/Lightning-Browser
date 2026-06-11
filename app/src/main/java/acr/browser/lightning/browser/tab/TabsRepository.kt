package acr.browser.lightning.browser.tab

import acr.browser.lightning.browser.BrowserContract
import acr.browser.lightning.browser.di.InitialUrl
import acr.browser.lightning.browser.tab.bundle.BundleStore
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.utils.isFileUrl
import kotlinx.coroutines.CompletableDeferred
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
    @InitialUrl private val initialUrl: String?,
    private val permissionInitializerFactory: PermissionInitializer.Factory,
    private val coroutineDispatchers: CoroutineDispatchers,
) : BrowserContract.Model {

    private val isInitialized = CompletableDeferred<Unit>()
    private val tabsListStateFlow = MutableStateFlow<List<TabModel>>(emptyList())
    private var selectedTab: TabModel? = null

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

    /**
     * Creates a tab without waiting for the browser to be initialized.
     */
    private suspend fun createTabUnsafe(
        tabInitializer: TabInitializer,
        tabType: TabModel.Type,
        emitUpdate: Boolean = true,
    ): TabModel = withContext(coroutineDispatchers.main) {
        val webViewLazy = webViewFactory.createWebView()
        val tabModel = tabFactory.constructTab(tabInitializer, webViewLazy, tabType)
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
                createTabUnsafe(
                    tabInitializer = it,
                    tabType = TabModel.Type.NORMAL,
                    emitUpdate = false
                )
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

            isInitialized.complete(Unit)
            tabsList = if (newTab != null) {
                oldTabs + newTab
            } else {
                oldTabs
            }

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
