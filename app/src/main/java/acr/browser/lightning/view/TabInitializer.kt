package acr.browser.lightning.view

import acr.browser.lightning.constant.SCHEME_BOOKMARKS
import acr.browser.lightning.constant.SCHEME_HOMEPAGE
import acr.browser.lightning.html.bookmark.BookmarkPage
import acr.browser.lightning.html.homepage.StartPage
import acr.browser.lightning.preference.UserPreferences
import android.app.Activity
import android.os.Bundle
import android.os.Message
import android.webkit.WebView
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy

/**
 * An initializer that is run on a [LightningView] after it is created.
 */
interface TabInitializer {

    /**
     * Initialize the [WebView] instance held by the [LightningView]. If a url is loaded, the
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
class HomePageInitializer(
    private val userPreferences: UserPreferences,
    private val activity: Activity,
    private val databaseScheduler: Scheduler,
    private val foregroundScheduler: Scheduler
) : TabInitializer {

    override fun initialize(webView: WebView, headers: Map<String, String>) {
        val homepage = userPreferences.homepage

        when (homepage) {
            SCHEME_HOMEPAGE -> StartPageInitializer(databaseScheduler, foregroundScheduler)
            SCHEME_BOOKMARKS -> BookmarkPageInitializer(activity, databaseScheduler, foregroundScheduler)
            else -> UrlInitializer(homepage)
        }.initialize(webView, headers)
    }

}

/**
 * An initializer that displays the start page.
 */
class StartPageInitializer(
    private val databaseScheduler: Scheduler,
    private val foregroundScheduler: Scheduler
) : TabInitializer {

    override fun initialize(webView: WebView, headers: Map<String, String>) {
        StartPage()
            .createHomePage()
            .subscribeOn(databaseScheduler)
            .observeOn(foregroundScheduler)
            .subscribeBy(onSuccess = { webView.loadUrl(it, headers) })
    }

}

/**
 * An initializer that displays the bookmark page.
 */
class BookmarkPageInitializer(
    private val activity: Activity,
    private val databaseScheduler: Scheduler,
    private val foregroundScheduler: Scheduler
) : TabInitializer {

    override fun initialize(webView: WebView, headers: Map<String, String>) {
        BookmarkPage(activity)
            .createBookmarkPage()
            .subscribeOn(databaseScheduler)
            .observeOn(foregroundScheduler)
            .subscribeBy(onSuccess = { webView.loadUrl(it, headers) })
    }

}

/**
 * An initializer that loads the url emitted by the [asyncUrl] observable.
 */
class AsyncUrlInitializer(
    private val asyncUrl: Single<String>,
    private val backgroundScheduler: Scheduler,
    private val foregroundScheduler: Scheduler
) : TabInitializer {

    override fun initialize(webView: WebView, headers: Map<String, String>) {
        asyncUrl
            .subscribeOn(backgroundScheduler)
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
class BundleInitializer(private val bundle: Bundle) : TabInitializer {

    override fun initialize(webView: WebView, headers: Map<String, String>) {
        webView.restoreState(bundle)
    }

}

/**
 * An initializer that does not load anything into the [WebView].
 */
class NoOpInitializer : TabInitializer {

    override fun initialize(webView: WebView, headers: Map<String, String>) = Unit

}
