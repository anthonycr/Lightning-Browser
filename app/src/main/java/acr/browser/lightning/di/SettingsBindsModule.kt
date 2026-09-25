package acr.browser.lightning.di

import acr.browser.lightning.theme.DefaultThemeProvider
import acr.browser.lightning.theme.ThemeProvider
import dagger.Binds
import dagger.Module

@Module
interface SettingsBindsModule {

    @Binds
    fun bindsThemeProvider(themeProvider: DefaultThemeProvider): ThemeProvider
}
