package acr.browser.lightning.di

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.adblock.BloomFilterAdBlocker
import acr.browser.lightning.adblock.NoOpAdBlocker
import acr.browser.lightning.browser.SearchBoxModel
import acr.browser.lightning.browser.activity.BrowserActivity
import acr.browser.lightning.browser.activity.ThemableBrowserActivity
import acr.browser.lightning.browser.bookmarks.BookmarksDrawerView
import acr.browser.lightning.device.BuildInfo
import acr.browser.lightning.dialog.LightningDialogBuilder
import acr.browser.lightning.download.DownloadHandler
import acr.browser.lightning.download.LightningDownloadListener
import acr.browser.lightning.reading.activity.ReadingActivity
import acr.browser.lightning.search.SuggestionsAdapter
import acr.browser.lightning.settings.activity.SettingsActivity
import acr.browser.lightning.settings.activity.ThemableSettingsActivity
import acr.browser.lightning.settings.fragment.*
import acr.browser.lightning.utils.ProxyUtils
import acr.browser.lightning.view.LightningChromeClient
import acr.browser.lightning.view.LightningView
import acr.browser.lightning.view.LightningWebClient
import android.app.Application
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [(AppModule::class), (AppBindsModule::class)])
interface AppComponent {

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun buildInfo(buildInfo: BuildInfo): Builder

        fun build(): AppComponent
    }

    fun inject(activity: BrowserActivity)

    fun inject(fragment: BookmarkSettingsFragment)

    fun inject(builder: LightningDialogBuilder)

    fun inject(lightningView: LightningView)

    fun inject(activity: ThemableBrowserActivity)

    fun inject(advancedSettingsFragment: AdvancedSettingsFragment)

    fun inject(app: BrowserApp)

    fun inject(proxyUtils: ProxyUtils)

    fun inject(activity: ReadingActivity)

    fun inject(webClient: LightningWebClient)

    fun inject(activity: SettingsActivity)

    fun inject(activity: ThemableSettingsActivity)

    fun inject(listener: LightningDownloadListener)

    fun inject(fragment: PrivacySettingsFragment)

    fun inject(fragment: DebugSettingsFragment)

    fun inject(suggestionsAdapter: SuggestionsAdapter)

    fun inject(chromeClient: LightningChromeClient)

    fun inject(downloadHandler: DownloadHandler)

    fun inject(searchBoxModel: SearchBoxModel)

    fun inject(generalSettingsFragment: GeneralSettingsFragment)

    fun inject(displaySettingsFragment: DisplaySettingsFragment)

    fun inject(adBlockSettingsFragment: AdBlockSettingsFragment)

    fun inject(bookmarksView: BookmarksDrawerView)

    fun provideBloomFilterAdBlocker(): BloomFilterAdBlocker

    fun provideNoOpAdBlocker(): NoOpAdBlocker

}
