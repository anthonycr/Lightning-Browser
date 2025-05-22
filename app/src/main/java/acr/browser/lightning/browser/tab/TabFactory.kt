package acr.browser.lightning.browser.tab

import acr.browser.lightning.browser.di.CacheDir
import acr.browser.lightning.browser.di.DiskScheduler
import acr.browser.lightning.browser.di.FilesDir
import acr.browser.lightning.browser.di.MainScheduler
import acr.browser.lightning.utils.ThreadSafeFileProvider
import android.app.Application
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader.InternalStoragePathHandler
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import java.io.File
import javax.inject.Inject

/**
 * Constructs a [TabModel].
 */
class TabFactory @Inject constructor(
    private val app: Application,
    private val webViewFactory: WebViewFactory,
    private val tabWebViewClientFactory: TabWebViewClient.Factory,
    private val tabAdapterFactory: TabAdapter.Factory,
    @DiskScheduler private val diskScheduler: Scheduler,
    @MainScheduler private val mainScheduler: Scheduler,
    @CacheDir private val cacheDirThreadSafeFileProvider: ThreadSafeFileProvider,
    @FilesDir private val filesDirThreadSafeFileProvider: ThreadSafeFileProvider,
) {

    /**
     * Constructs a tab from the [webView] with the provided [tabInitializer].
     */
    fun constructTab(
        tabInitializer: TabInitializer,
        webView: WebView,
        tabType: TabModel.Type
    ): Single<TabModel> {
        val faviconHandler = cacheDirThreadSafeFileProvider.file()
            .map { InternalStoragePathHandler(app, File(it, "favicon-cache")) }
        val htmlHandler = filesDirThreadSafeFileProvider.file()
            .map { InternalStoragePathHandler(app, File(it, "generated-html")) }

        return faviconHandler.zipWith(htmlHandler, ::Pair)
            .subscribeOn(diskScheduler)
            .observeOn(mainScheduler)
            .map { (faviconHandler, htmlHandler) ->
                val headers = webViewFactory.createRequestHeaders()
                tabAdapterFactory.create(
                    tabInitializer = tabInitializer,
                    webView = webView,
                    requestHeaders = headers,
                    tabWebViewClient = tabWebViewClientFactory.create(
                        headers,
                        faviconHandler,
                        htmlHandler
                    ),
                    tabType,
                )
            }
    }
}
