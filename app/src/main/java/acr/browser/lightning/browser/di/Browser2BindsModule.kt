package acr.browser.lightning.browser.di

import acr.browser.lightning.browser.BrowserContract
import acr.browser.lightning.browser.BrowserNavigator
import acr.browser.lightning.browser.image.FaviconImageLoader
import acr.browser.lightning.browser.image.ImageLoader
import acr.browser.lightning.browser.proxy.Proxy
import acr.browser.lightning.browser.proxy.ProxyAdapter
import acr.browser.lightning.browser.tab.TabsRepository
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
    fun bindsFaviconImageLoader(faviconImageLoader: FaviconImageLoader): ImageLoader

    @Binds
    fun bindsBrowserNavigator(browserNavigator: BrowserNavigator): BrowserContract.Navigator

    @Binds
    fun bindsProxy(proxyAdapter: ProxyAdapter): Proxy
}
