package acr.browser.lightning.di

import acr.browser.lightning.adblock.allowlist.AllowListModel
import acr.browser.lightning.adblock.allowlist.SessionAllowListModel
import acr.browser.lightning.adblock.source.AssetsHostsDataSource
import acr.browser.lightning.adblock.source.HostsDataSource
import acr.browser.lightning.adblock.source.HostsDataSourceProvider
import acr.browser.lightning.adblock.source.PreferencesHostsDataSourceProvider
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
    fun provideBookmarkModel(bookmarkDatabase: BookmarkDatabase): BookmarkRepository

    @Binds
    fun provideDownloadsModel(downloadsDatabase: DownloadsDatabase): DownloadsRepository

    @Binds
    fun providesHistoryModel(historyDatabase: HistoryDatabase): HistoryRepository

    @Binds
    fun providesAdBlockAllowListModel(adBlockAllowListDatabase: AdBlockAllowListDatabase): AdBlockAllowListRepository

    @Binds
    fun providesAllowListModel(sessionAllowListModel: SessionAllowListModel): AllowListModel

    @Binds
    fun providesSslWarningPreferences(sessionSslWarningPreferences: SessionSslWarningPreferences): SslWarningPreferences

    @Binds
    fun providesHostsDataSource(assetsHostsDataSource: AssetsHostsDataSource): HostsDataSource

    @Binds
    fun providesHostsRepository(hostsDatabase: HostsDatabase): HostsRepository

    @Binds
    fun providesHostsDataSourceProvider(preferencesHostsDataSourceProvider: PreferencesHostsDataSourceProvider): HostsDataSourceProvider
}
