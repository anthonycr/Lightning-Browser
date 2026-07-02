package acr.browser.lightning.browser.di

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.device.BuildInfo
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

        @BindsInstance
        fun incognitoMode(@IncognitoMode incognitoMode: Boolean): Builder

        fun build(): AppComponent
    }

    fun inject(app: BrowserApp)

    fun inject(activity: SettingsActivity)

    fun browser2ComponentBuilder(): Browser2Component.Builder

}

@Module(subcomponents = [Browser2Component::class])
internal class Submodules
