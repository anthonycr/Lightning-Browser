package acr.browser.lightning

import acr.browser.lightning.database.bookmark.BookmarkExporter
import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.di.*
import acr.browser.lightning.preference.DeveloperPreferences
import acr.browser.lightning.utils.FileUtils
import acr.browser.lightning.utils.MemoryLeakUtils
import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.StrictMode
import android.util.Log
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import com.squareup.leakcanary.LeakCanary
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.plugins.RxJavaPlugins
import javax.inject.Inject

class BrowserApp : Application() {

    @Inject internal lateinit var developerPreferences: DeveloperPreferences
    @Inject internal lateinit var bookmarkModel: BookmarkRepository
    @Inject @field:DatabaseScheduler internal lateinit var databaseScheduler: Scheduler

    val applicationComponent: AppComponent by lazy { appComponent }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build())
        }

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, ex ->
            if (BuildConfig.DEBUG) {
                FileUtils.writeCrashToStorage(ex)
            }

            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, ex)
            } else {
                System.exit(2)
            }
        }

        RxJavaPlugins.setErrorHandler { throwable: Throwable? ->
            if (BuildConfig.DEBUG && throwable != null) {
                FileUtils.writeCrashToStorage(throwable)
                throw throwable
            }
        }

        appComponent = DaggerAppComponent.builder().appModule(AppModule(this)).build()
        injector.inject(this)

        Single.fromCallable(bookmarkModel::count)
            .filter { it == 0L }
            .flatMapCompletable {
                val assetsBookmarks = BookmarkExporter.importBookmarksFromAssets(this@BrowserApp)
                bookmarkModel.addBookmarkList(assetsBookmarks)
            }
            .subscribeOn(databaseScheduler)
            .subscribe()

        if (developerPreferences.useLeakCanary && !isRelease) {
            LeakCanary.install(this)
        }
        if (!isRelease) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        registerActivityLifecycleCallbacks(object : MemoryLeakUtils.LifecycleAdapter() {
            override fun onActivityDestroyed(activity: Activity) {
                Log.d(TAG, "Cleaning up after the Android framework")
                MemoryLeakUtils.clearNextServedView(activity, this@BrowserApp)
            }
        })
    }

    companion object {

        private const val TAG = "BrowserApp"

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT)
        }

        @JvmStatic
        lateinit var appComponent: AppComponent

        /**
         * Determines whether this is a release build.
         *
         * @return true if this is a release build, false otherwise.
         */
        @JvmStatic
        val isRelease: Boolean
            get() = !BuildConfig.DEBUG || BuildConfig.BUILD_TYPE.toLowerCase() == "release"

    }

}
