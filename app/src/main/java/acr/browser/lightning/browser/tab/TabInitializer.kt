package acr.browser.lightning.browser.tab

import acr.browser.lightning.R
import acr.browser.lightning.constant.SCHEME_BOOKMARKS
import acr.browser.lightning.constant.SCHEME_HOMEPAGE
import acr.browser.lightning.browser.di.DiskScheduler
import acr.browser.lightning.browser.di.MainScheduler
import acr.browser.lightning.extensions.resizeAndShow
import acr.browser.lightning.html.HtmlPageFactory
import acr.browser.lightning.html.bookmark.BookmarkPageFactory
import acr.browser.lightning.html.download.DownloadPageFactory
import acr.browser.lightning.html.history.HistoryPageFactory
import acr.browser.lightning.html.homepage.HomePageFactory
import acr.browser.lightning.preference.UserPreferences
import android.app.Activity
import android.os.Bundle
import android.os.Message
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import dagger.Reusable
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.reactivex.Scheduler
import io.reactivex.rxkotlin.subscribeBy
import javax.inject.Inject

/**
 * An initializer that is run on a [WebView] after it is created.
 */
interface TabInitializer {

    /**
     * Initialize the [WebView] instance held by the tab. If a url is loaded, the
     * provided [headers] should be used to load the url.
     */
    fun initialize(webView: WebView, headers: Map<String, String>)

}

/**
 * An initializer that loads a [url].
 */
class UrlInitializer(private val url: String) : TabInitializer {

    override fun initialize(webView: WebView, headers: Map<String, String>) {
        webView.loadUrl(url, headers)
    }

}

/**
 * An initializer that displays the page set as the user's homepage preference.
 */
@Reusable
class HomePageInitializer @Inject constructor(
    private val userPreferences: UserPreferences,
    private val startPageInitializer: StartPageInitializer,
    private val bookmarkPageInitializer: BookmarkPageInitializer
) : TabInitializer {

    override fun initialize(webView: WebView, headers: Map<String, String>) {
        val homepage = userPreferences.homepage

        when (homepage) {
            SCHEME_HOMEPAGE -> startPageInitializer
            SCHEME_BOOKMARKS -> bookmarkPageInitializer
            else -> UrlInitializer(homepage)
        }.initialize(webView, headers)
    }

}

/**
 * An initializer that displays the start page.
 */
@Reusable
class StartPageInitializer @Inject constructor(
    homePageFactory: HomePageFactory,
    @DiskScheduler diskScheduler: Scheduler,
    @MainScheduler foregroundScheduler: Scheduler
) : HtmlPageFactoryInitializer(homePageFactory, diskScheduler, foregroundScheduler)

/**
 * An initializer that displays the bookmark page.
 */
@Reusable
class BookmarkPageInitializer @Inject constructor(
    bookmarkPageFactory: BookmarkPageFactory,
    @DiskScheduler diskScheduler: Scheduler,
    @MainScheduler foregroundScheduler: Scheduler
) : HtmlPageFactoryInitializer(bookmarkPageFactory, diskScheduler, foregroundScheduler)

/**
 * An initializer that displays the download page.
 */
@Reusable
class DownloadPageInitializer @Inject constructor(
    downloadPageFactory: DownloadPageFactory,
    @DiskScheduler diskScheduler: Scheduler,
    @MainScheduler foregroundScheduler: Scheduler
) : HtmlPageFactoryInitializer(downloadPageFactory, diskScheduler, foregroundScheduler)

/**
 * An initializer that displays the history page.
 */
@Reusable
class HistoryPageInitializer @Inject constructor(
    historyPageFactory: HistoryPageFactory,
    @DiskScheduler diskScheduler: Scheduler,
    @MainScheduler foregroundScheduler: Scheduler
) : HtmlPageFactoryInitializer(historyPageFactory, diskScheduler, foregroundScheduler)

/**
 * An initializer that loads the url built by the [HtmlPageFactory].
 */
abstract class HtmlPageFactoryInitializer(
    private val htmlPageFactory: HtmlPageFactory,
    @DiskScheduler private val diskScheduler: Scheduler,
    @MainScheduler private val foregroundScheduler: Scheduler
) : TabInitializer {

    override fun initialize(webView: WebView, headers: Map<String, String>) {
        htmlPageFactory
            .buildPage()
            .subscribeOn(diskScheduler)
            .observeOn(foregroundScheduler)
            .subscribeBy(onSuccess = { webView.loadUrl(it, headers) })
    }

}

/**
 * An initializer that sets the [WebView] as the target of the [resultMessage]. Used for
 * `target="_blank"` links.
 */
class ResultMessageInitializer(private val resultMessage: Message) : TabInitializer {

    override fun initialize(webView: WebView, headers: Map<String, String>) {
        resultMessage.apply {
            (obj as WebView.WebViewTransport).webView = webView
        }.sendToTarget()
    }

}

/**
 * An initializer that restores the [WebView] state using the [bundle].
 */
open class BundleInitializer(private val bundle: Bundle) : TabInitializer {

    override fun initialize(webView: WebView, headers: Map<String, String>) {
        webView.restoreState(bundle)
    }

}

/**
 * An initializer that can be delayed until the view is attached. [initialTitle] is the title that
 * should be initially set on the tab.
 */
class FreezableBundleInitializer(
    val bundle: Bundle,
    val initialTitle: String
) : BundleInitializer(bundle)

/**
 * An initializer that does not load anything into the [WebView].
 */
class NoOpInitializer : TabInitializer {

    override fun initialize(webView: WebView, headers: Map<String, String>) = Unit

}

/**
 * Ask the user's permission before loading the [url] and load the homepage instead if they deny
 * permission. Useful for scenarios where another app may attempt to open a malicious URL in the
 * browser via an intent.
 */
class PermissionInitializer @AssistedInject constructor(
    @Assisted private val url: String,
    private val activity: Activity,
    private val homePageInitializer: HomePageInitializer
) : TabInitializer {

    override fun initialize(webView: WebView, headers: Map<String, String>) {
        AlertDialog.Builder(activity).apply {
            setTitle(R.string.title_warning)
            setMessage(R.string.message_blocked_local)
            setCancelable(false)
            setOnDismissListener {
                homePageInitializer.initialize(webView, headers)
            }
            setNegativeButton(android.R.string.cancel, null)
            setPositiveButton(R.string.action_open) { _, _ ->
                UrlInitializer(url).initialize(webView, headers)
            }
        }.resizeAndShow()
    }

    /**
     * The factory for constructing the permission initializer.
     */
    @AssistedFactory
    interface Factory {

        /**
         * Creates the initializer.
         */
        fun create(url: String): PermissionInitializer

    }

}
