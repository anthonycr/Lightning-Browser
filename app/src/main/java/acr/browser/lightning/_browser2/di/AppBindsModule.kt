package acr.browser.lightning._browser2.di

import acr.browser.lightning._browser2.theme.LegacyThemeProvider
import acr.browser.lightning._browser2.theme.ThemeProvider
import acr.browser.lightning.adblock.allowlist.AllowListModel
import acr.browser.lightning.adblock.allowlist.SessionAllowListModel
import acr.browser.lightning.adblock.source.AssetsHostsDataSource
import acr.browser.lightning.adblock.source.HostsDataSource
import acr.browser.lightning.adblock.source.HostsDataSourceProvider
import acr.browser.lightning.adblock.source.PreferencesHostsDataSourceProvider
import acr.browser.lightning.browser.cleanup.DelegatingExitCleanup
import acr.browser.lightning.browser.cleanup.ExitCleanup
import acr.browser.lightning.database.adblock.HostsDatabase
import acr.browser.lightning.database.adblock.HostsRepository
import acr.browser.lightning.database.allowlist.AdBlockAllowListDatabase
import acr.browser.lightning.database.allowlist.AdBlockAllowListRepository
import acr.browser.lightning.database.bookmark.BookmarkDatabase
import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.database.downloads.DownloadsDatabase
import acr.browser.lightning.database.downloads.DownloadsRepository
import acr.browser.lightning.database.history.HistoryDatabase
import acr.browser.lightning.database.history.HistoryRepository
import acr.browser.lightning.ssl.SessionSslWarningPreferences
import acr.browser.lightning.ssl.SslWarningPreferences
import dagger.Binds
import dagger.Module

/**
 * Dependency injection module used to bind implementations to interfaces.
 */
@Module
interface AppBindsModule {

    @Binds
    fun bindsExitCleanup(delegatingExitCleanup: DelegatingExitCleanup): ExitCleanup

    @Binds
    fun bindsBookmarkModel(bookmarkDatabase: BookmarkDatabase): BookmarkRepository

    @Binds
    fun bindsDownloadsModel(downloadsDatabase: DownloadsDatabase): DownloadsRepository

    @Binds
    fun bindsHistoryModel(historyDatabase: HistoryDatabase): HistoryRepository

    @Binds
    fun bindsAdBlockAllowListModel(adBlockAllowListDatabase: AdBlockAllowListDatabase): AdBlockAllowListRepository

    @Binds
    fun bindsAllowListModel(sessionAllowListModel: SessionAllowListModel): AllowListModel

    @Binds
    fun bindsSslWarningPreferences(sessionSslWarningPreferences: SessionSslWarningPreferences): SslWarningPreferences

    @Binds
    fun bindsHostsDataSource(assetsHostsDataSource: AssetsHostsDataSource): HostsDataSource

    @Binds
    fun bindsHostsRepository(hostsDatabase: HostsDatabase): HostsRepository

    @Binds
    fun bindsHostsDataSourceProvider(preferencesHostsDataSourceProvider: PreferencesHostsDataSourceProvider): HostsDataSourceProvider

    @Binds
    fun bindsThemeProvider(legacyThemeProvider: LegacyThemeProvider): ThemeProvider
}
