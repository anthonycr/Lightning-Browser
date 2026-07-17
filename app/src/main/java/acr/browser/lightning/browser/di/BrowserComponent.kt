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
@BrowserScope
@Subcomponent(modules = [BrowserModule::class, BrowserBindsModule::class])
interface BrowserComponent {

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

        fun build(): BrowserComponent

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
annotation class InitialAction

@Qualifier
annotation class IncognitoMode
