package acr.browser.lightning.browser.di

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.ThemableBrowserActivity
import acr.browser.lightning.browser.search.SearchBoxModel
import acr.browser.lightning.device.BuildInfo
import acr.browser.lightning.dialog.LightningDialogBuilder
import acr.browser.lightning.search.SuggestionsAdapter
import acr.browser.lightning.settings.activity.SettingsActivity
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

    fun inject(builder: LightningDialogBuilder)

    fun inject(activity: ThemableBrowserActivity)

    fun inject(app: BrowserApp)

    fun inject(activity: SettingsActivity)

    fun inject(suggestionsAdapter: SuggestionsAdapter)

    fun inject(searchBoxModel: SearchBoxModel)

    fun browser2ComponentBuilder(): Browser2Component.Builder

}

@Module(subcomponents = [Browser2Component::class])
internal class Submodules
