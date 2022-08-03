package acr.browser.lightning.browser.di

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.adblock.BloomFilterAdBlocker
import acr.browser.lightning.adblock.NoOpAdBlocker
import acr.browser.lightning.browser.search.SearchBoxModel
import acr.browser.lightning.browser.ThemableBrowserActivity
import acr.browser.lightning.device.BuildInfo
import acr.browser.lightning.dialog.LightningDialogBuilder
import acr.browser.lightning.download.LightningDownloadListener
import acr.browser.lightning.reading.activity.ReadingActivity
import acr.browser.lightning.search.SuggestionsAdapter
import acr.browser.lightning.settings.activity.SettingsActivity
import acr.browser.lightning.settings.activity.ThemableSettingsActivity
import acr.browser.lightning.settings.fragment.*
import android.app.Application
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, AppBindsModule::class, Submodules::class])
interface AppComponent {

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun buildInfo(buildInfo: BuildInfo): Builder

        fun build(): AppComponent
    }

    fun inject(fragment: BookmarkSettingsFragment)

    fun inject(builder: LightningDialogBuilder)

    fun inject(activity: ThemableBrowserActivity)

    fun inject(advancedSettingsFragment: AdvancedSettingsFragment)

    fun inject(app: BrowserApp)

    fun inject(activity: ReadingActivity)

    fun inject(activity: SettingsActivity)

    fun inject(activity: ThemableSettingsActivity)

    fun inject(listener: LightningDownloadListener)

    fun inject(fragment: PrivacySettingsFragment)

    fun inject(fragment: DebugSettingsFragment)

    fun inject(suggestionsAdapter: SuggestionsAdapter)

    fun inject(searchBoxModel: SearchBoxModel)

    fun inject(generalSettingsFragment: GeneralSettingsFragment)

    fun inject(displaySettingsFragment: DisplaySettingsFragment)

    fun inject(adBlockSettingsFragment: AdBlockSettingsFragment)

    fun provideBloomFilterAdBlocker(): BloomFilterAdBlocker

    fun provideNoOpAdBlocker(): NoOpAdBlocker

    fun browser2ComponentBuilder(): Browser2Component.Builder

}

@Module(subcomponents = [Browser2Component::class])
internal class Submodules
