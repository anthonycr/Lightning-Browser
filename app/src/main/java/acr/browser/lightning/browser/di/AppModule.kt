package acr.browser.lightning.browser.di

import acr.browser.lightning.R
import acr.browser.lightning.browser.tab.DefaultTabTitle
import acr.browser.lightning.concurrency.AppCoroutineScope
import acr.browser.lightning.concurrency.CoroutineDispatcherProvider
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.device.BuildInfo
import acr.browser.lightning.device.BuildType
import acr.browser.lightning.favicon.FaviconCleanup
import acr.browser.lightning.html.ListPageReader
import acr.browser.lightning.html.bookmark.BookmarkCleanup
import acr.browser.lightning.html.bookmark.BookmarkPageReader
import acr.browser.lightning.html.download.DownloadCleanup
import acr.browser.lightning.html.history.HistoryCleanup
import acr.browser.lightning.html.homepage.HomeCleanup
import acr.browser.lightning.html.homepage.HomePageReader
import acr.browser.lightning.js.InvertPage
import acr.browser.lightning.js.TextReflow
import acr.browser.lightning.js.ThemeColor
import acr.browser.lightning.log.AndroidLogger
import acr.browser.lightning.log.Logger
import acr.browser.lightning.log.NoOpLogger
import acr.browser.lightning.migration.Cleanup
import acr.browser.lightning.search.suggestions.RequestFactory
import acr.browser.lightning.utils.FileUtils
import acr.browser.lightning.utils.ThreadSafeFileProvider
import android.app.ActivityManager
import android.app.Application
import android.app.DownloadManager
import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ShortcutManager
import android.content.res.AssetManager
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import com.anthonycr.mezzanine.mezzanine
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
class AppModule {

    @Provides
    @MainHandler
    fun provideMainHandler() = Handler(Looper.getMainLooper())

    @Provides
    fun provideContext(application: Application): Context = application.applicationContext

    @Provides
    fun providesAssetManager(application: Application): AssetManager = application.assets

    @Provides
    fun providesClipboardManager(application: Application) =
        application.getSystemService<ClipboardManager>()!!

    @Provides
    fun providesInputMethodManager(application: Application) =
        application.getSystemService<InputMethodManager>()!!

    @Provides
    fun providesDownloadManager(application: Application) =
        application.getSystemService<DownloadManager>()!!

    @Provides
    fun providesConnectivityManager(application: Application) =
        application.getSystemService<ConnectivityManager>()!!

    @Provides
    fun providesNotificationManager(application: Application) =
        application.getSystemService<NotificationManager>()!!

    @Provides
    fun providesWindowManager(application: Application) =
        application.getSystemService<WindowManager>()!!

    @Provides
    fun providesShortcutManager(application: Application) =
        application.getSystemService<ShortcutManager>()!!

    @Provides
    fun providesActivityManager(application: Application) =
        application.getSystemService<ActivityManager>()!!

    @Singleton
    @Provides
    fun providesSuggestionsCacheControl(): CacheControl =
        CacheControl.Builder().maxStale(1, TimeUnit.DAYS).build()

    @Singleton
    @Provides
    fun providesSuggestionsRequestFactory(cacheControl: CacheControl): RequestFactory =
        object : RequestFactory {
            override fun createSuggestionsRequest(httpUrl: HttpUrl, encoding: String): Request {
                return Request.Builder().url(httpUrl)
                    .addHeader("Accept-Charset", encoding)
                    .cacheControl(cacheControl)
                    .build()
            }
        }

    private fun createInterceptorWithMaxCacheAge(maxCacheAgeSeconds: Long) = Interceptor { chain ->
        chain.proceed(chain.request()).newBuilder()
            .header("cache-control", "max-age=$maxCacheAgeSeconds, max-stale=$maxCacheAgeSeconds")
            .build()
    }

    @Singleton
    @Provides
    @SuggestionsClient
    fun providesSuggestionsCoroutineHttpClient(
        application: Application,
        appCoroutineScope: AppCoroutineScope,
        coroutineDispatchers: CoroutineDispatchers,
    ): Deferred<OkHttpClient> = appCoroutineScope.async(coroutineDispatchers.io) {
        val intervalDay = TimeUnit.DAYS.toSeconds(1)
        val suggestionsCache = File(application.cacheDir, "suggestion_responses")

        OkHttpClient.Builder()
            .cache(Cache(suggestionsCache, FileUtils.megabytesToBytes(1)))
            .addNetworkInterceptor(createInterceptorWithMaxCacheAge(intervalDay))
            .build()
    }

    @Singleton
    @Provides
    @HostsClient
    fun providesHostsHttpClient(
        application: Application,
        appCoroutineScope: AppCoroutineScope,
        coroutineDispatchers: CoroutineDispatchers,
    ): Deferred<OkHttpClient> = appCoroutineScope.async(coroutineDispatchers.io) {
        val intervalYear = TimeUnit.DAYS.toSeconds(365)
        val suggestionsCache = File(application.cacheDir, "hosts_cache")

        OkHttpClient.Builder()
            .cache(Cache(suggestionsCache, FileUtils.megabytesToBytes(5)))
            .addNetworkInterceptor(createInterceptorWithMaxCacheAge(intervalYear))
            .build()
    }

    @Provides
    @Singleton
    fun provideLogger(buildInfo: BuildInfo): Logger = if (buildInfo.buildType == BuildType.DEBUG) {
        AndroidLogger()
    } else {
        NoOpLogger()
    }

    @Provides
    fun providesListPageReader(): ListPageReader = mezzanine()

    @Provides
    fun providesHomePageReader(): HomePageReader = mezzanine()

    @Provides
    fun providesBookmarkPageReader(): BookmarkPageReader = mezzanine()

    @Provides
    fun providesTextReflow(): TextReflow = mezzanine()

    @Provides
    fun providesThemeColor(): ThemeColor = mezzanine()

    @Provides
    fun providesInvertPage(): InvertPage = mezzanine()

    @DefaultTabTitle
    @Provides
    fun providesDefaultTabTitle(application: Application): String =
        application.getString(R.string.untitled)

    @Provides
    fun providesCleanupList(
        faviconCleanup: FaviconCleanup,
        bookmarkCleanup: BookmarkCleanup,
        downloadCleanup: DownloadCleanup,
        historyCleanup: HistoryCleanup,
        homeCleanup: HomeCleanup
    ): List<@JvmSuppressWildcards Cleanup.Action> =
        listOf(faviconCleanup, bookmarkCleanup, downloadCleanup, historyCleanup, homeCleanup)

    @FilesDir
    @Provides
    fun providesFilesDir(
        application: Application,
        threadSafeFileProviderFactory: ThreadSafeFileProvider.Factory
    ): ThreadSafeFileProvider = threadSafeFileProviderFactory.create {
        application.filesDir
    }

    @DataDir
    @Provides
    fun providesDataDir(
        application: Application,
        threadSafeFileProviderFactory: ThreadSafeFileProvider.Factory
    ): ThreadSafeFileProvider = threadSafeFileProviderFactory.create {
        application.dataDir
    }

    @CacheDir
    @Provides
    fun providesCacheDir(
        application: Application,
        threadSafeFileProviderFactory: ThreadSafeFileProvider.Factory
    ): ThreadSafeFileProvider = threadSafeFileProviderFactory.create {
        application.cacheDir
    }

    @Singleton
    @OptIn(DelicateCoroutinesApi::class)
    @Provides
    fun providesAppCoroutineScope(coroutineDispatchers: CoroutineDispatchers): AppCoroutineScope =
        AppCoroutineScope(CoroutineScope(coroutineDispatchers.main + SupervisorJob()))

    @Singleton
    @Provides
    fun providesDispatchers(): CoroutineDispatchers = CoroutineDispatcherProvider(
        main = Dispatchers.Main,
        io = Dispatchers.IO,
        default = Dispatchers.Default
    )
}

@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class SuggestionsClient

@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class HostsClient

@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class MainHandler

@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class FilesDir

@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class DataDir

@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class CacheDir
