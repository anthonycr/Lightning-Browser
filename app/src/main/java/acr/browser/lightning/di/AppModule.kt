package acr.browser.lightning.di

import acr.browser.lightning.BrowserApp
import android.app.Application
import android.app.DownloadManager
import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ShortcutManager
import android.net.ConnectivityManager
import android.os.Build
import android.support.annotation.RequiresApi
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import net.i2p.android.ui.I2PAndroidHelper
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
class AppModule(private val app: BrowserApp) {

    @Provides
    fun provideApplication(): Application = app

    @Provides
    fun provideContext(): Context = app.applicationContext

    @Provides
    fun providesClipboardManager() = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    @Provides
    fun providesInputMethodManager() = app.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    @Provides
    fun providesDownloadManager() = app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    @Provides
    fun providesConnectivityManager() = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Provides
    fun providesNotificationManager() = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Provides
    fun providesWindowManager() = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    @Provides
    fun providesShortcutManager() = app.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager

    @Provides
    @Named("database")
    @Singleton
    fun providesIoThread(): Scheduler = Schedulers.from(Executors.newSingleThreadExecutor())

    @Provides
    @Named("disk")
    @Singleton
    fun providesDiskThread(): Scheduler = Schedulers.from(Executors.newSingleThreadExecutor())

    @Provides
    @Named("network")
    @Singleton
    fun providesNetworkThread(): Scheduler = Schedulers.from(ThreadPoolExecutor(0, 4, 60, TimeUnit.SECONDS, LinkedBlockingDeque()))

    @Provides
    @Singleton
    fun provideI2PAndroidHelper(): I2PAndroidHelper = I2PAndroidHelper(app)

}
