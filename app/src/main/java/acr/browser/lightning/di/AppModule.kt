package acr.browser.lightning.di

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.adblock.whitelist.SessionWhitelistModel
import acr.browser.lightning.adblock.whitelist.WhitelistModel
import acr.browser.lightning.database.bookmark.BookmarkDatabase
import acr.browser.lightning.database.bookmark.BookmarkModel
import acr.browser.lightning.database.downloads.DownloadsDatabase
import acr.browser.lightning.database.downloads.DownloadsModel
import acr.browser.lightning.database.history.HistoryDatabase
import acr.browser.lightning.database.history.HistoryModel
import acr.browser.lightning.database.whitelist.AdBlockWhitelistDatabase
import acr.browser.lightning.database.whitelist.AdBlockWhitelistModel
import acr.browser.lightning.ssl.SessionSslWarningPreferences
import acr.browser.lightning.ssl.SslWarningPreferences
import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import net.i2p.android.ui.I2PAndroidHelper
import javax.inject.Singleton

@Module
class AppModule(private val app: BrowserApp) {

    @Provides
    fun provideApplication(): Application = app

    @Provides
    fun provideContext(): Context = app.applicationContext

    @Provides
    @Singleton
    fun provideBookmarkModel(): BookmarkModel = BookmarkDatabase(app)

    @Provides
    @Singleton
    fun provideDownloadsModel(): DownloadsModel = DownloadsDatabase(app)

    @Provides
    @Singleton
    fun providesHistoryModel(): HistoryModel = HistoryDatabase(app)

    @Provides
    @Singleton
    fun providesAdBlockWhitelistModel(): AdBlockWhitelistModel = AdBlockWhitelistDatabase(app)

    @Provides
    @Singleton
    fun providesWhitelistModel(): WhitelistModel = SessionWhitelistModel(providesAdBlockWhitelistModel())

    @Provides
    @Singleton
    fun providesSslWarningPreferences(): SslWarningPreferences = SessionSslWarningPreferences()

    @Provides
    @Singleton
    fun provideI2PAndroidHelper(): I2PAndroidHelper = I2PAndroidHelper(app.applicationContext)

}
