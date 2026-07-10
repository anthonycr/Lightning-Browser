package acr.browser.lightning.browser.di

import acr.browser.lightning.browser.BrowserActivity
import android.content.Intent
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Qualifier

/**
 * The component for the browser scope.
 */
@Browser2Scope
@Subcomponent(modules = [Browser2Module::class, Browser2BindsModule::class])
interface Browser2Component {

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun activity(activity: FragmentActivity): Builder

        @BindsInstance
        fun browserFrame(@BrowserFrame frameLayout: FrameLayout): Builder

        @BindsInstance
        fun customFrame(@CustomFrame frameLayout: FrameLayout): Builder

        @BindsInstance
        fun initialIntent(@InitialIntent intent: Intent?): Builder

        fun build(): Browser2Component

    }

    fun inject(browserActivity: BrowserActivity)

}

@Qualifier
annotation class BrowserFrame

@Qualifier
annotation class CustomFrame

@Qualifier
annotation class InitialIntent

@Qualifier
annotation class InitialUrl

@Qualifier
annotation class IncognitoMode
