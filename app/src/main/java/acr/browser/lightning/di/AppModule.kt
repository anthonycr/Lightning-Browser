package acr.browser.lightning.di

import acr.browser.lightning.BrowserApp
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
import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import net.i2p.android.ui.I2PAndroidHelper
import java.util.concurrent.Executors
import javax.inject.Named
import javax.inject.Singleton

@Module
class AppModule(private val app: BrowserApp) {

    @Provides
    fun provideApplication(): Application = app

    @Provides
    fun provideContext(): Context = app.applicationContext

    @Provides
    @Singleton
    fun provideBookmarkModel(bookmarkDatabase: BookmarkDatabase): BookmarkRepository = bookmarkDatabase

    @Provides
    @Singleton
    fun provideDownloadsModel(downloadsDatabase: DownloadsDatabase): DownloadsRepository = downloadsDatabase

    @Provides
    @Singleton
    fun providesHistoryModel(historyDatabase: HistoryDatabase): HistoryRepository = historyDatabase

    @Provides
    @Singleton
    fun providesAdBlockWhitelistModel(adBlockWhitelistDatabase: AdBlockWhitelistDatabase): AdBlockWhitelistRepository = adBlockWhitelistDatabase

    @Provides
    @Singleton
    fun providesWhitelistModel(sessionWhitelistModel: SessionWhitelistModel): WhitelistModel = sessionWhitelistModel

    @Provides
    @Singleton
    fun providesSslWarningPreferences(sessionSslWarningPreferences: SessionSslWarningPreferences): SslWarningPreferences = sessionSslWarningPreferences

    @Provides
    @Named("database")
    @Singleton
    fun providesIoThread(): Scheduler = Schedulers.from(Executors.newFixedThreadPool(2))

    @Provides
    @Singleton
    fun provideI2PAndroidHelper(): I2PAndroidHelper = I2PAndroidHelper(app)

}
