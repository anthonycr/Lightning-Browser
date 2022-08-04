package acr.browser.lightning.browser.tab

import acr.browser.lightning.browser.download.PendingDownload
import acr.browser.lightning.ssl.SslCertificateInfo
import acr.browser.lightning.ssl.SslState
import acr.browser.lightning.utils.Option
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.annotation.ColorInt
import io.reactivex.Observable

/**
 * The representation of a browser tab.
 */
interface TabModel {

    /**
     * The tab identifier.
     */
    val id: Int

    // Navigation

    /**
     * Load a [url] in the tab.
     */
    fun loadUrl(url: String)

    /**
     * Load a URL using the provided [tabInitializer].
     */
    fun loadFromInitializer(tabInitializer: TabInitializer)

    /**
     * Go back in the navigation tree.
     */
    fun goBack()

    /**
     * True if [goBack] has something to go back to, false otherwise.
     */
    fun canGoBack(): Boolean

    /**
     * Emits changes to the [canGoBack] status.
     */
    fun canGoBackChanges(): Observable<Boolean>

    /**
     * Go forward in the navigation tree.
     */
    fun goForward()

    /**
     * True if [goForward] has something to go forward to, false otherwise.
     */
    fun canGoForward(): Boolean

    /**
     * Emits changes to the [canGoForward] status.
     */
    fun canGoForwardChanges(): Observable<Boolean>

    /**
     * Toggle the user agent used by the browser to a desktop one or back to the default one.
     */
    fun toggleDesktopAgent()

    /**
     * Reload the page the browser is currently showing.
     */
    fun reload()

    /**
     * Stop loading the current page if it is loading. If the page is not loading, has no effect.
     */
    fun stopLoading()

    /**
     * Highlight words in the webpage that match the [query].
     */
    fun find(query: String)

    /**
     * Move to the next word highlighted by [find].
     */
    fun findNext()

    /**
     * Move to the previous word highlighted by [find].
     */
    fun findPrevious()

    /**
     * Remove highlighting from all words highlighted by [find].
     */
    fun clearFindMatches()

    /**
     * The current query that is being highlighted by [find].
     */
    val findQuery: String?

    // Data

    /**
     * The current favicon of the webpage or null if there isn't one.
     */
    val favicon: Bitmap?

    /**
     * Emits changes to the [favicon].
     */
    fun faviconChanges(): Observable<Option<Bitmap>>

    /**
     * The thematic color of the current webpage.
     */
    @get:ColorInt
    val themeColor: Int

    /**
     * Emits changes to the [themeColor].
     */
    fun themeColorChanges(): Observable<Int>

    /**
     * The URL of the currently displayed webpage.
     */
    val url: String

    /**
     * Emits changes to the [url].
     */
    fun urlChanges(): Observable<String>

    /**
     * The title of the current webpage.
     */
    val title: String

    /**
     * Emits changes to the [title].
     */
    fun titleChanges(): Observable<String>

    /**
     * The current SSL certificate information about the webpage.
     */
    val sslCertificateInfo: SslCertificateInfo?

    /**
     * The current state of the SSL certificate.
     */
    val sslState: SslState

    /**
     * Emits changes to [sslState].
     */
    fun sslChanges(): Observable<SslState>

    /**
     * The loading progress for the current webpage on a scale of 0-100. If the page is completely
     * loaded, then the progress will be 100.
     */
    val loadingProgress: Int

    /**
     * Emits changes to [sslState].
     */
    fun loadingProgress(): Observable<Int>

    // Lifecycle

    /**
     * Emits requests to download a file represented by [PendingDownload] that are triggered by the
     * browser.
     */
    fun downloadRequests(): Observable<PendingDownload>

    /**
     * Emits requests to open the file chooser that are triggered by the browser.
     */
    fun fileChooserRequests(): Observable<Intent>

    /**
     * Handle a resulting file to upload after selecting a file from the file chooser.
     */
    fun handleFileChooserResult(activityResult: ActivityResult)

    /**
     * Emits requests by the browser to display a custom view (i.e. full screen video) over the
     * regular webpage content.
     */
    fun showCustomViewRequests(): Observable<View>

    /**
     * Emits requests by the browser to hide the custom view it previously requested to display via
     * [showCustomViewRequests].
     */
    fun hideCustomViewRequests(): Observable<Unit>

    /**
     * Notify the browser that we are manually hiding the custom view requested to be shown by
     * [showCustomViewRequests].
     */
    fun hideCustomView()

    /**
     * Emits requests by the browser to automatically open a new tab and load the URL provided by
     * the [TabInitializer].
     */
    fun createWindowRequests(): Observable<TabInitializer>

    /**
     * Emits requests by the browser to automatically close the current tab.
     */
    fun closeWindowRequests(): Observable<Unit>

    /**
     * True if the tab is in the foreground, false if it is in the background. Used to prevent
     * background tabs from consuming disproportionate amounts of resources when they are unused.
     */
    var isForeground: Boolean

    /**
     * Teardown the current tab and release held resources.
     */
    fun destroy()

    /**
     * Freeze the current state of the tab and return it as a [Bundle].
     */
    fun freeze(): Bundle
}
