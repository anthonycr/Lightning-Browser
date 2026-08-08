package acr.browser.lightning.browser.tab

import acr.browser.lightning.R
import acr.browser.lightning.constant.SCHEME_BOOKMARKS
import acr.browser.lightning.constant.SCHEME_HOMEPAGE
import acr.browser.lightning.extensions.resizeAndShow
import acr.browser.lightning.html.HtmlPageFactory
import acr.browser.lightning.html.bookmark.BookmarkPageFactory
import acr.browser.lightning.html.download.DownloadPageFactory
import acr.browser.lightning.html.history.HistoryPageFactory
import acr.browser.lightning.html.homepage.HomePageFactory
import acr.browser.lightning.preference.UserPreferencesDataStore
import android.app.Activity
import android.os.Bundle
import android.os.Message
import androidx.appcompat.app.AlertDialog
import dagger.Reusable
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * An initializer that is run on a [TabModel] after it is created.
 */
interface TabInitializer {

    /**
     * Initialize the [TabModel] instance held by the tab.
     */
    suspend fun initialize(tab: TabModel)

}

/**
 * An initializer that loads a [url].
 */
class UrlInitializer(private val url: String) : TabInitializer {

    override suspend fun initialize(tab: TabModel) {
        tab.loadUrl(url)
    }

}

/**
 * An initializer that displays the page set as the user's homepage preference.
 */
@Reusable
class HomePageInitializer @Inject constructor(
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val startPageInitializer: StartPageInitializer,
    private val bookmarkPageInitializer: BookmarkPageInitializer
) : TabInitializer {

    override suspend fun initialize(tab: TabModel) {
        val homepage = userPreferencesDataStore.homepage.get()

        when (homepage) {
            SCHEME_HOMEPAGE -> startPageInitializer
            SCHEME_BOOKMARKS -> bookmarkPageInitializer
            else -> UrlInitializer(homepage)
        }.initialize(tab)
    }

}

/**
 * An initializer that displays the start page.
 */
@Reusable
class StartPageInitializer @Inject constructor(
    homePageFactory: HomePageFactory
) : HtmlPageFactoryInitializer(homePageFactory)

/**
 * An initializer that displays the bookmark page.
 */
@Reusable
class BookmarkPageInitializer @Inject constructor(
    bookmarkPageFactory: BookmarkPageFactory
) : HtmlPageFactoryInitializer(bookmarkPageFactory)

/**
 * An initializer that displays the download page.
 */
@Reusable
class DownloadPageInitializer @Inject constructor(
    downloadPageFactory: DownloadPageFactory
) : HtmlPageFactoryInitializer(downloadPageFactory)

/**
 * An initializer that displays the history page.
 */
@Reusable
class HistoryPageInitializer @Inject constructor(
    historyPageFactory: HistoryPageFactory
) : HtmlPageFactoryInitializer(historyPageFactory)

/**
 * An initializer that loads the url built by the [HtmlPageFactory].
 */
abstract class HtmlPageFactoryInitializer(
    private val htmlPageFactory: HtmlPageFactory
) : TabInitializer {

    override suspend fun initialize(
        tab: TabModel
    ) {
        val page = htmlPageFactory.buildPage()
        tab.loadUrl(page)
    }

}

/**
 * An initializer that sets the [TabModel] as the target of the [resultMessage]. Used for
 * `target="_blank"` links.
 */
class ResultMessageInitializer(private val resultMessage: Message) : TabInitializer {

    override suspend fun initialize(tab: TabModel) {
        tab.handleMessage(resultMessage)
    }

}

/**
 * An initializer that restores the [TabModel] state using the [bundle].
 */
class BundleInitializer(private val bundle: Bundle) : TabInitializer {

    override suspend fun initialize(tab: TabModel) {
        tab.restore(bundle)
    }

}

/**
 * An initializer that can be delayed until the view is attached. [initialTitle] is the title that
 * should be initially set on the tab.
 */
class FreezableInitializer(
    val bundle: Bundle,
    val delegate: TabInitializer,
    val initialTitle: String,
    val id: Int,
) : TabInitializer {

    override suspend fun initialize(
        tab: TabModel
    ) {
        delegate.initialize(tab)
    }

}

/**
 * An initializer that does not load anything into the [TabModel].
 */
class NoOpInitializer : TabInitializer {

    override suspend fun initialize(tab: TabModel) = Unit

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

    override suspend fun initialize(tab: TabModel) {
        val dialogChoice = showDialog()
        when (dialogChoice) {
            DialogChoice.DISMISS -> homePageInitializer.initialize(tab)
            DialogChoice.OPEN -> UrlInitializer(url).initialize(tab)
        }
    }

    private enum class DialogChoice {
        DISMISS,
        OPEN
    }

    private suspend fun showDialog(): DialogChoice = suspendCancellableCoroutine { continuation ->
        AlertDialog.Builder(activity).apply {
            setTitle(R.string.title_warning)
            setMessage(R.string.message_blocked_local)
            setCancelable(false)
            setOnDismissListener {
                continuation.resume(DialogChoice.DISMISS)
            }
            setNegativeButton(android.R.string.cancel, null)
            setPositiveButton(R.string.action_open) { _, _ ->
                continuation.resume(DialogChoice.OPEN)
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
