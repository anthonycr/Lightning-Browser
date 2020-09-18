package acr.browser.lightning._browser2.di

import acr.browser.lightning._browser2.BrowserContract
import acr.browser.lightning._browser2.BrowserNavigator
import acr.browser.lightning._browser2.history.DefaultHistoryRecord
import acr.browser.lightning._browser2.history.HistoryRecord
import acr.browser.lightning._browser2.image.FaviconImageLoader
import acr.browser.lightning._browser2.image.ImageLoader
import acr.browser.lightning._browser2.tab.TabsRepository
import dagger.Binds
import dagger.Module

/**
 * Created by anthonycr on 9/15/20.
 */
@Module
interface Browser2BindsModule {

    @Binds
    fun bindsBrowserModel(tabsRepository: TabsRepository): BrowserContract.Model

    @Binds
    fun bindsHistoryRecord(defaultHistoryRecord: DefaultHistoryRecord): HistoryRecord

    @Binds
    fun bindsFaviconImageLoader(faviconImageLoader: FaviconImageLoader): ImageLoader

    @Binds
    fun bindsBrowserNavigator(browserNavigator: BrowserNavigator): BrowserContract.Navigator
}
