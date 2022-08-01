package acr.browser.lightning._browser2.di

import acr.browser.lightning._browser2.BrowserContract
import acr.browser.lightning._browser2.BrowserNavigator
import acr.browser.lightning._browser2.history.DefaultHistoryRecord
import acr.browser.lightning._browser2.history.HistoryRecord
import acr.browser.lightning._browser2.image.FaviconImageLoader
import acr.browser.lightning._browser2.image.ImageLoader
import acr.browser.lightning._browser2.tab.TabsRepository
import acr.browser.lightning._browser2.theme.LegacyThemeProvider
import acr.browser.lightning._browser2.theme.ThemeProvider
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
    fun bindsThemeProvider(legacyThemeProvider: LegacyThemeProvider): ThemeProvider
}
