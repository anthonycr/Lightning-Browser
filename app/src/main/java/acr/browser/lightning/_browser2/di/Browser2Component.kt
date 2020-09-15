package acr.browser.lightning._browser2.di

import acr.browser.lightning._browser2.BrowserActivity
import android.app.Activity
import android.widget.FrameLayout
import dagger.BindsInstance
import dagger.Subcomponent

/**
 * Created by anthonycr on 9/15/20.
 */
@Subcomponent(modules = [Browser2Module::class])
interface Browser2Component {

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun activity(activity: Activity): Builder

        @BindsInstance
        fun browserFrame(frameLayout: FrameLayout): Builder

        fun build(): Browser2Component

    }

    fun inject(browserActivity: BrowserActivity)

}
