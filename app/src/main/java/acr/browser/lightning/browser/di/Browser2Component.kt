package acr.browser.lightning.browser.di

import acr.browser.lightning.browser.BrowserActivity
import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
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
        fun activity(activity: Activity): Builder

        @BindsInstance
        fun browserFrame(frameLayout: FrameLayout): Builder

        @BindsInstance
        fun toolbarRoot(linearLayout: LinearLayout): Builder

        @BindsInstance
        fun toolbar(toolbar: View): Builder

        @BindsInstance
        fun initialIntent(@InitialIntent intent: Intent): Builder

        @BindsInstance
        fun incognitoMode(@IncognitoMode incognitoMode: Boolean): Builder

        fun build(): Browser2Component

    }

    fun inject(browserActivity: BrowserActivity)

}

@Qualifier
annotation class InitialIntent

@Qualifier
annotation class InitialUrl

@Qualifier
annotation class IncognitoMode
