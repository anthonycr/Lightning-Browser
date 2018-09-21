package acr.browser.lightning.di

import acr.browser.lightning.adblock.whitelist.SessionWhitelistModel
import acr.browser.lightning.adblock.whitelist.WhitelistModel
import acr.browser.lightning.database.bookmark.BookmarkDatabase
import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.database.downloads.DownloadsDatabase
import acr.browser.lightning.database.downloads.DownloadsRepository
import acr.browser.lightning.database.history.HistoryDatabase
import acr.browser.lightning.database.history.HistoryRepository
import acr.browser.lightning.database.whitelist.AdBlockWhitelistDatabase
import acr.browser.lightning.database.whitelist.AdBlockWhitelistRepository
import acr.browser.lightning.ssl.SessionSslWarningPreferences
import acr.browser.lightning.ssl.SslWarningPreferences
import dagger.Binds
import dagger.Module

/**
 * Dependency injection module used to bind implementations to interfaces.
 */
@Module
abstract class AppBindsModule {

    @Binds
    abstract fun provideBookmarkModel(bookmarkDatabase: BookmarkDatabase): BookmarkRepository

    @Binds
    abstract fun provideDownloadsModel(downloadsDatabase: DownloadsDatabase): DownloadsRepository

    @Binds
    abstract fun providesHistoryModel(historyDatabase: HistoryDatabase): HistoryRepository

    @Binds
    abstract fun providesAdBlockWhitelistModel(adBlockWhitelistDatabase: AdBlockWhitelistDatabase): AdBlockWhitelistRepository

    @Binds
    abstract fun providesWhitelistModel(sessionWhitelistModel: SessionWhitelistModel): WhitelistModel

    @Binds
    abstract fun providesSslWarningPreferences(sessionSslWarningPreferences: SessionSslWarningPreferences): SslWarningPreferences

}
