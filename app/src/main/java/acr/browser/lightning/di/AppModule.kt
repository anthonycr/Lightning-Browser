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
import acr.browser.lightning.rx.IoSchedulers
import acr.browser.lightning.ssl.SessionSslWarningPreferences
import acr.browser.lightning.ssl.SslWarningPreferences
import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import net.i2p.android.ui.I2PAndroidHelper
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
    fun provideBookmarkModel(): BookmarkRepository = BookmarkDatabase(app)

    @Provides
    @Singleton
    fun provideDownloadsModel(): DownloadsRepository = DownloadsDatabase(app)

    @Provides
    @Singleton
    fun providesHistoryModel(): HistoryRepository = HistoryDatabase(app)

    @Provides
    @Singleton
    fun providesAdBlockWhitelistModel(): AdBlockWhitelistRepository = AdBlockWhitelistDatabase(app)

    @Provides
    @Singleton
    fun providesWhitelistModel(): WhitelistModel = SessionWhitelistModel(providesAdBlockWhitelistModel(), providesIoThread())

    @Provides
    @Singleton
    fun providesSslWarningPreferences(): SslWarningPreferences = SessionSslWarningPreferences()

    @Provides
    @Named("io")
    @Singleton
    fun providesIoThread(): Scheduler = IoSchedulers.database

    @Provides
    @Singleton
    fun provideI2PAndroidHelper(): I2PAndroidHelper = I2PAndroidHelper(app.applicationContext)

}
