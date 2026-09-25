package acr.browser.lightning.di

import acr.browser.lightning.AppTheme
import acr.browser.lightning.concurrency.AppCoroutineScope
import acr.browser.lightning.theme.ThemeProvider
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Named

@Module
class SettingsModule {

    @Named("theme")
    @Provides
    fun providesAppThemeStateFlow(
        themeProvider: ThemeProvider,
        appCoroutineScope: AppCoroutineScope,
    ): StateFlow<AppTheme?> = themeProvider.appThemeValues()
        .stateIn(appCoroutineScope, SharingStarted.Eagerly, null)
}
