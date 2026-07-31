package acr.browser.lightning.di

import acr.browser.lightning.settings.activity.SettingsActivity
import android.app.Activity
import dagger.BindsInstance
import dagger.Subcomponent

@SettingsScope
@Subcomponent
interface SettingsComponent {

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun activity(activity: Activity): Builder

        fun build(): SettingsComponent
    }

    fun inject(activity: SettingsActivity)

}
