package acr.browser.lightning.di

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.BuildConfig
import acr.browser.lightning.device.BuildInfo
import acr.browser.lightning.html.ListPageReader
import acr.browser.lightning.html.bookmark.BookmarkPageReader
import acr.browser.lightning.html.homepage.HomePageReader
import acr.browser.lightning.log.AndroidLogger
import acr.browser.lightning.log.Logger
import acr.browser.lightning.log.NoOpLogger
import acr.browser.lightning.search.suggestions.RequestFactory
import acr.browser.lightning.utils.FileUtils
import android.app.Application
import android.app.DownloadManager
import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ShortcutManager
import android.content.res.AssetManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.anthonycr.mezzanine.MezzanineGenerator
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import net.i2p.android.ui.I2PAndroidHelper
import okhttp3.*
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
class AppModule(private val browserApp: BrowserApp, private val buildInfo: BuildInfo) {

    @Provides
    fun provideBuildInfo() = buildInfo

    @Provides
    @MainHandler
    fun provideMainHandler() = Handler(Looper.getMainLooper())

    @Provides
    fun provideApplication(): Application = browserApp

    @Provides
    fun provideContext(): Context = browserApp.applicationContext

    @Provides
    @UserPrefs
    fun provideUserPreferences(): SharedPreferences = browserApp.getSharedPreferences("settings", 0)

    @Provides
    @DevPrefs
    fun provideDebugPreferences(): SharedPreferences = browserApp.getSharedPreferences("developer_settings", 0)

    @Provides
    @AdBlockPrefs
    fun provideAdBlockPreferences(): SharedPreferences = browserApp.getSharedPreferences("ad_block_settings", 0)


    @Provides
    fun providesAssetManager(): AssetManager = browserApp.assets

    @Provides
    fun providesClipboardManager() = browserApp.getSystemService<ClipboardManager>()!!

    @Provides
    fun providesInputMethodManager() = browserApp.getSystemService<InputMethodManager>()!!

    @Provides
    fun providesDownloadManager() = browserApp.getSystemService<DownloadManager>()!!

    @Provides
    fun providesConnectivityManager() = browserApp.getSystemService<ConnectivityManager>()!!

    @Provides
    fun providesNotificationManager() = browserApp.getSystemService<NotificationManager>()!!

    @Provides
    fun providesWindowManager() = browserApp.getSystemService<WindowManager>()!!

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    @Provides
    fun providesShortcutManager() = browserApp.getSystemService<ShortcutManager>()!!

    @Provides
    @DatabaseScheduler
    @Singleton
    fun providesIoThread(): Scheduler = Schedulers.from(Executors.newSingleThreadExecutor())

    @Provides
    @DiskScheduler
    @Singleton
    fun providesDiskThread(): Scheduler = Schedulers.from(Executors.newSingleThreadExecutor())

    @Provides
    @NetworkScheduler
    @Singleton
    fun providesNetworkThread(): Scheduler = Schedulers.from(ThreadPoolExecutor(0, 4, 60, TimeUnit.SECONDS, LinkedBlockingDeque()))

    @Provides
    @MainScheduler
    @Singleton
    fun providesMainThread(): Scheduler = AndroidSchedulers.mainThread()

    @Singleton
    @Provides
    fun providesSuggestionsCacheControl(): CacheControl = CacheControl.Builder().maxStale(1, TimeUnit.DAYS).build()

    @Singleton
    @Provides
    fun providesSuggestionsRequestFactory(cacheControl: CacheControl): RequestFactory = object : RequestFactory {

        override fun createSuggestionsRequest(httpUrl: HttpUrl, encoding: String): Request {
            return Request.Builder().url(httpUrl)
                .addHeader("Accept-Charset", encoding)
                .cacheControl(cacheControl)
                .build()
        }

    }

    @Singleton
    @Provides
    @SuggestionsClient
    fun providesSuggestionsHttpClient(): OkHttpClient {
        val intervalDay = TimeUnit.DAYS.toSeconds(1)

        val rewriteCacheControlInterceptor = Interceptor { chain ->
            val originalResponse = chain.proceed(chain.request())
            originalResponse.newBuilder()
                .header("cache-control", "max-age=$intervalDay, max-stale=$intervalDay")
                .build()
        }

        val suggestionsCache = File(browserApp.cacheDir, "suggestion_responses")

        return OkHttpClient.Builder()
            .cache(Cache(suggestionsCache, FileUtils.megabytesToBytes(1)))
            .addNetworkInterceptor(rewriteCacheControlInterceptor)
            .build()
    }

    @Singleton
    @Provides
    @GeneralClient
    fun providesGeneralHttpClient(): OkHttpClient {
        val intervalDay = TimeUnit.DAYS.toSeconds(1)

        val rewriteCacheControlInterceptor = Interceptor { chain ->
            val originalResponse = chain.proceed(chain.request())
            originalResponse.newBuilder()
                .header("cache-control", "max-age=$intervalDay, max-stale=$intervalDay")
                .build()
        }

        val suggestionsCache = File(browserApp.cacheDir, "okhttp")

        return OkHttpClient.Builder()
            .cache(Cache(suggestionsCache, FileUtils.megabytesToBytes(1)))
            .addNetworkInterceptor(rewriteCacheControlInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideLogger(): Logger = if (BuildConfig.DEBUG) {
        AndroidLogger()
    } else {
        NoOpLogger()
    }

    @Provides
    @Singleton
    fun provideI2PAndroidHelper(): I2PAndroidHelper = I2PAndroidHelper(browserApp)

    @Provides
    fun providesListPageReader(): ListPageReader = MezzanineGenerator.ListPageReader()

    @Provides
    fun providesHomePageReader(): HomePageReader = MezzanineGenerator.HomePageReader()

    @Provides
    fun providesBookmarkPageReader(): BookmarkPageReader = MezzanineGenerator.BookmarkPageReader()

}

@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class SuggestionsClient

@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class GeneralClient

@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class MainHandler

@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class UserPrefs

@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class AdBlockPrefs

@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class DevPrefs

@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class MainScheduler

@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class DiskScheduler

@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class NetworkScheduler

@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class DatabaseScheduler
