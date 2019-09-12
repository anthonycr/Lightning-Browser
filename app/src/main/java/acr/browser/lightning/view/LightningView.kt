/*
 * Copyright 2014 A.C.R. Development
 */

package acr.browser.lightning.view

import acr.browser.lightning.constant.DESKTOP_USER_AGENT
import acr.browser.lightning.controller.UIController
import acr.browser.lightning.di.DatabaseScheduler
import acr.browser.lightning.di.MainScheduler
import acr.browser.lightning.di.injector
import acr.browser.lightning.dialog.LightningDialogBuilder
import acr.browser.lightning.download.LightningDownloadListener
import acr.browser.lightning.log.Logger
import acr.browser.lightning.network.NetworkConnectivityModel
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.preference.userAgent
import acr.browser.lightning.ssl.SslState
import acr.browser.lightning.utils.*
import acr.browser.lightning.view.find.FindResults
import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.*
import android.net.http.SslCertificate
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.View.OnTouchListener
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebSettings.LayoutAlgorithm
import android.webkit.WebView
import androidx.collection.ArrayMap
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * [LightningView] acts as a tab for the browser, handling WebView creation and handling logic, as
 * well as properly initialing it. All interactions with the WebView should be made through this
 * class.
 */
class LightningView(
    private val activity: Activity,
    tabInitializer: TabInitializer,
    val isIncognito: Boolean,
    private val homePageInitializer: HomePageInitializer,
    private val bookmarkPageInitializer: BookmarkPageInitializer,
    private val downloadPageInitializer: DownloadPageInitializer,
    private val logger: Logger
) {

    /**
     * The unique ID of the view.
     */
    val id = View.generateViewId()

    /**
     * Getter for the [LightningViewTitle] of the current LightningView instance.
     *
     * @return a NonNull instance of LightningViewTitle
     */
    val titleInfo: LightningViewTitle

    /**
     * Gets the current WebView instance of the tab.
     *
     * @return the WebView instance of the tab, which can be null.
     */
    var webView: WebView? = null
        private set

    private val uiController: UIController
    private val gestureDetector: GestureDetector
    private val paint = Paint()

    /**
     * Sets whether this tab was the result of a new intent sent to the browser.
     */
    var isNewTab: Boolean = false

    /**
     * This method sets the tab as the foreground tab or the background tab.
     */
    var isForegroundTab: Boolean = false
        set(isForeground) {
            field = isForeground
            uiController.tabChanged(this)
        }
    /**
     * Gets whether or not the page rendering is inverted or not. The main purpose of this is to
     * indicate that JavaScript should be run at the end of a page load to invert only the images
     * back to their non-inverted states.
     *
     * @return true if the page is in inverted mode, false otherwise.
     */
    var invertPage = false
        private set
    private var toggleDesktop = false
    private val webViewHandler = WebViewHandler(this)

    /**
     * This method gets the additional headers that should be added with each request the browser
     * makes.
     *
     * @return a non null Map of Strings with the additional request headers.
     */
    internal val requestHeaders = ArrayMap<String, String>()

    private val maxFling: Float

    @Inject internal lateinit var userPreferences: UserPreferences
    @Inject internal lateinit var dialogBuilder: LightningDialogBuilder
    @Inject internal lateinit var proxyUtils: ProxyUtils
    @Inject @field:DatabaseScheduler internal lateinit var databaseScheduler: Scheduler
    @Inject @field:MainScheduler internal lateinit var mainScheduler: Scheduler
    @Inject lateinit var networkConnectivityModel: NetworkConnectivityModel

    private val lightningWebClient: LightningWebClient

    private val networkDisposable: Disposable

    /**
     * This method determines whether the current tab is visible or not.
     *
     * @return true if the WebView is non-null and visible, false otherwise.
     */
    val isShown: Boolean
        get() = webView?.isShown == true

    /**
     * Gets the current progress of the WebView.
     *
     * @return returns a number between 0 and 100 with the current progress of the WebView. If the
     * WebView is null, then the progress returned will be 100.
     */
    val progress: Int
        get() = webView?.progress ?: 100

    /**
     * Get the current user agent used by the WebView.
     *
     * @return retuns the current user agent of the WebView instance, or an empty string if the
     * WebView is null.
     */
    private val userAgent: String
        get() = webView?.settings?.userAgentString ?: ""

    /**
     * Gets the favicon currently in use by the page. If the current page does not have a favicon,
     * it returns a default icon.
     *
     * @return a non-null Bitmap with the current favicon.
     */
    val favicon: Bitmap?
        get() = titleInfo.getFavicon()

    /**
     * Get the current title of the page, retrieved from the title object.
     *
     * @return the title of the page, or an empty string if there is no title.
     */
    val title: String
        get() = titleInfo.getTitle() ?: ""

    /**
     * Get the current [SslCertificate] if there is any associated with the current page.
     */
    val sslCertificate: SslCertificate?
        get() = webView?.certificate

    /**
     * Get the current URL of the WebView, or an empty string if the WebView is null or the URL is
     * null.
     *
     * @return the current URL or an empty string.
     */
    val url: String
        get() = webView?.url ?: ""

    init {
        activity.injector.inject(this)
        uiController = activity as UIController

        titleInfo = LightningViewTitle(activity)

        maxFling = ViewConfiguration.get(activity).scaledMaximumFlingVelocity.toFloat()
        lightningWebClient = LightningWebClient(activity, this)
        gestureDetector = GestureDetector(activity, CustomGestureListener())

        val tab = WebView(activity).also { webView = it }.apply {
            id = this@LightningView.id

            isFocusableInTouchMode = true
            isFocusable = true
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                isAnimationCacheEnabled = false
                isAlwaysDrawnWithCacheEnabled = false
            }
            setBackgroundColor(Color.WHITE)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
            }

            isScrollbarFadingEnabled = true
            isSaveEnabled = true
            setNetworkAvailable(true)
            webChromeClient = LightningChromeClient(activity, this@LightningView)
            webViewClient = lightningWebClient

            setDownloadListener(LightningDownloadListener(activity))
            setOnTouchListener(TouchListener())
            initializeSettings()
        }
        initializePreferences()

        tabInitializer.initialize(tab, requestHeaders)

        networkDisposable = networkConnectivityModel.connectivity()
            .observeOn(mainScheduler)
            .subscribe(::setNetworkAvailable)
    }

    fun currentSslState(): SslState = lightningWebClient.sslState

    fun sslStateObservable(): Observable<SslState> = lightningWebClient.sslStateObservable()

    /**
     * This method loads the homepage for the browser. Either it loads the URL stored as the
     * homepage, or loads the startpage or bookmark page if either of those are set as the homepage.
     */
    fun loadHomePage() {
        reinitialize(homePageInitializer)
    }

    private fun reinitialize(tabInitializer: TabInitializer) {
        webView?.let { tabInitializer.initialize(it, requestHeaders) }
    }

    /**
     * This function loads the bookmark page via the [BookmarkPageInitializer].
     */
    fun loadBookmarkPage() {
        reinitialize(bookmarkPageInitializer)
    }

    /**
     * This function loads the download page via the [DownloadPageInitializer].
     */
    fun loadDownloadsPage() {
        reinitialize(downloadPageInitializer)
    }

    /**
     * Initialize the preference driven settings of the WebView. This method must be called whenever
     * the preferences are changed within SharedPreferences.
     */
    @SuppressLint("NewApi", "SetJavaScriptEnabled")
    fun initializePreferences() {
        val settings = webView?.settings ?: return

        lightningWebClient.updatePreferences()

        val modifiesHeaders = userPreferences.doNotTrackEnabled
            || userPreferences.saveDataEnabled
            || userPreferences.removeIdentifyingHeadersEnabled

        if (userPreferences.doNotTrackEnabled) {
            requestHeaders[HEADER_DNT] = "1"
        } else {
            requestHeaders.remove(HEADER_DNT)
        }

        if (userPreferences.saveDataEnabled) {
            requestHeaders[HEADER_SAVEDATA] = "on"
        } else {
            requestHeaders.remove(HEADER_SAVEDATA)
        }

        if (userPreferences.removeIdentifyingHeadersEnabled) {
            requestHeaders[HEADER_REQUESTED_WITH] = ""
            requestHeaders[HEADER_WAP_PROFILE] = ""
        } else {
            requestHeaders.remove(HEADER_REQUESTED_WITH)
            requestHeaders.remove(HEADER_WAP_PROFILE)
        }

        settings.defaultTextEncodingName = userPreferences.textEncoding
        setColorMode(userPreferences.renderingMode)

        if (!isIncognito) {
            settings.setGeolocationEnabled(userPreferences.locationEnabled)
        } else {
            settings.setGeolocationEnabled(false)
        }

        setUserAgentForPreference(userPreferences)

        settings.saveFormData = userPreferences.savePasswordsEnabled && !isIncognito

        if (userPreferences.javaScriptEnabled) {
            settings.javaScriptEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
        } else {
            settings.javaScriptEnabled = false
            settings.javaScriptCanOpenWindowsAutomatically = false
        }

        if (userPreferences.textReflowEnabled) {
            settings.layoutAlgorithm = LayoutAlgorithm.NARROW_COLUMNS
            try {
                settings.layoutAlgorithm = LayoutAlgorithm.TEXT_AUTOSIZING
            } catch (e: Exception) {
                // This shouldn't be necessary, but there are a number
                // of KitKat devices that crash trying to set this
                logger.log(TAG, "Problem setting LayoutAlgorithm to TEXT_AUTOSIZING")
            }
        } else {
            settings.layoutAlgorithm = LayoutAlgorithm.NORMAL
        }

        settings.blockNetworkImage = userPreferences.blockImagesEnabled
        if (!isIncognito) {
            // Modifying headers causes SEGFAULTS, so disallow multi window if headers are enabled.
            settings.setSupportMultipleWindows(userPreferences.popupsEnabled && !modifiesHeaders)
        } else {
            settings.setSupportMultipleWindows(false)
        }

        settings.useWideViewPort = userPreferences.useWideViewPortEnabled
        settings.loadWithOverviewMode = userPreferences.overviewModeEnabled
        settings.textZoom = when (userPreferences.textSize) {
            0 -> 200
            1 -> 150
            2 -> 125
            3 -> 100
            4 -> 75
            5 -> 50
            else -> throw IllegalArgumentException("Unsupported text size")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView,
                !userPreferences.blockThirdPartyCookiesEnabled)
        }
    }

    /**
     * Initialize the settings of the WebView that are intrinsic to Lightning and cannot be altered
     * by the user. Distinguish between Incognito and Regular tabs here.
     */
    @SuppressLint("NewApi")
    private fun WebView.initializeSettings() {
        settings.apply {
            mediaPlaybackRequiresUserGesture = true

            if (API >= Build.VERSION_CODES.LOLLIPOP && !isIncognito) {
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            } else if (API >= Build.VERSION_CODES.LOLLIPOP) {
                // We're in Incognito mode, reject
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }

            if (!isIncognito) {
                domStorageEnabled = true
                setAppCacheEnabled(true)
                cacheMode = WebSettings.LOAD_DEFAULT
                databaseEnabled = true
            } else {
                domStorageEnabled = false
                setAppCacheEnabled(false)
                databaseEnabled = false
                cacheMode = WebSettings.LOAD_NO_CACHE
            }

            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            allowContentAccess = true
            allowFileAccess = true
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false

            getPathObservable("appcache")
                .subscribeOn(databaseScheduler)
                .observeOn(mainScheduler)
                .subscribe { file ->
                    setAppCachePath(file.path)
                }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                getPathObservable("geolocation")
                    .subscribeOn(databaseScheduler)
                    .observeOn(mainScheduler)
                    .subscribe { file ->
                        setGeolocationDatabasePath(file.path)
                    }
            }
        }

    }

    private fun getPathObservable(subFolder: String) = Single.fromCallable {
        activity.getDir(subFolder, 0)
    }

    /**
     * This method is used to toggle the user agent between desktop and the current preference of
     * the user.
     */
    fun toggleDesktopUA() {
        if (!toggleDesktop) {
            webView?.settings?.userAgentString = DESKTOP_USER_AGENT
        } else {
            setUserAgentForPreference(userPreferences)
        }

        toggleDesktop = !toggleDesktop
    }

    /**
     * This method sets the user agent of the current tab based on the user's preference
     */
    private fun setUserAgentForPreference(userPreferences: UserPreferences) {
        webView?.settings?.userAgentString = userPreferences.userAgent(activity.application)
    }

    /**
     * Save the state of the tab and return it as a [Bundle].
     */
    fun saveState(): Bundle = Bundle(ClassLoader.getSystemClassLoader()).also {
        webView?.saveState(it)
    }

    /**
     * Pause the current WebView instance.
     */
    fun onPause() {
        webView?.onPause()
        logger.log(TAG, "WebView onPause: ${webView?.id}")
    }

    /**
     * Resume the current WebView instance.
     */
    fun onResume() {
        webView?.onResume()
        logger.log(TAG, "WebView onResume: ${webView?.id}")
    }

    /**
     * Notify the WebView to stop the current load.
     */
    fun stopLoading() {
        webView?.stopLoading()
    }

    /**
     * This method forces the layer type to hardware, which
     * enables hardware rendering on the WebView instance
     * of the current LightningView.
     */
    private fun setHardwareRendering() {
        webView?.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
    }

    /**
     * This method sets the layer type to none, which
     * means that either the GPU and CPU can both compose
     * the layers when necessary.
     */
    private fun setNormalRendering() {
        webView?.setLayerType(View.LAYER_TYPE_NONE, null)
    }

    /**
     * This method forces the layer type to software, which
     * disables hardware rendering on the WebView instance
     * of the current LightningView and makes the CPU render
     * the view.
     */
    fun setSoftwareRendering() {
        webView?.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    /**
     * Sets the current rendering color of the WebView instance
     * of the current LightningView. The for modes are normal
     * rendering, inverted rendering, grayscale rendering,
     * and inverted grayscale rendering
     *
     * @param mode the integer mode to set as the rendering mode.
     * see the numbers in documentation above for the
     * values this method accepts.
     */
    private fun setColorMode(mode: RenderingMode) {
        invertPage = false
        when (mode) {
            RenderingMode.NORMAL -> {
                paint.colorFilter = null
                // setSoftwareRendering(); // Some devices get segfaults
                // in the WebView with Hardware Acceleration enabled,
                // the only fix is to disable hardware rendering
                setNormalRendering()
                invertPage = false
            }
            RenderingMode.INVERTED -> {
                val filterInvert = ColorMatrixColorFilter(
                    negativeColorArray)
                paint.colorFilter = filterInvert
                setHardwareRendering()

                invertPage = true
            }
            RenderingMode.GRAYSCALE -> {
                val cm = ColorMatrix()
                cm.setSaturation(0f)
                val filterGray = ColorMatrixColorFilter(cm)
                paint.colorFilter = filterGray
                setHardwareRendering()
            }
            RenderingMode.INVERTED_GRAYSCALE -> {
                val matrix = ColorMatrix()
                matrix.set(negativeColorArray)
                val matrixGray = ColorMatrix()
                matrixGray.setSaturation(0f)
                val concat = ColorMatrix()
                concat.setConcat(matrix, matrixGray)
                val filterInvertGray = ColorMatrixColorFilter(concat)
                paint.colorFilter = filterInvertGray
                setHardwareRendering()

                invertPage = true
            }

            RenderingMode.INCREASE_CONTRAST -> {
                val increaseHighContrast = ColorMatrixColorFilter(increaseContrastColorArray)
                paint.colorFilter = increaseHighContrast
                setHardwareRendering()
            }
        }

    }

    /**
     * Pauses the JavaScript timers of the
     * WebView instance, which will trigger a
     * pause for all WebViews in the app.
     */
    fun pauseTimers() {
        webView?.pauseTimers()
        logger.log(TAG, "Pausing JS timers")
    }

    /**
     * Resumes the JavaScript timers of the
     * WebView instance, which will trigger a
     * resume for all WebViews in the app.
     */
    fun resumeTimers() {
        webView?.resumeTimers()
        logger.log(TAG, "Resuming JS timers")
    }

    /**
     * Requests focus down on the WebView instance
     * if the view does not already have focus.
     */
    fun requestFocus() {
        if (webView?.hasFocus() == false) {
            webView?.requestFocus()
        }
    }

    /**
     * Sets the visibility of the WebView to either
     * View.GONE, View.VISIBLE, or View.INVISIBLE.
     * other values passed in will have no effect.
     *
     * @param visible the visibility to set on the WebView.
     */
    fun setVisibility(visible: Int) {
        webView?.visibility = visible
    }

    /**
     * Tells the WebView to reload the current page.
     * If the proxy settings are not ready then the
     * this method will not have an affect as the
     * proxy must start before the load occurs.
     */
    fun reload() {
        // Check if configured proxy is available
        if (!proxyUtils.isProxyReady(activity)) {
            // User has been notified
            return
        }

        webView?.reload()
    }

    /**
     * Finds all the instances of the text passed to this
     * method and highlights the instances of that text
     * in the WebView.
     *
     * @param text the text to search for.
     */
    @SuppressLint("NewApi")
    fun find(text: String): FindResults {
        webView?.findAllAsync(text)

        return object : FindResults {
            override fun nextResult() {
                webView?.findNext(true)
            }

            override fun previousResult() {
                webView?.findNext(false)
            }

            override fun clearResults() {
                webView?.clearMatches()
            }
        }
    }

    /**
     * Notify the tab to shutdown and destroy
     * its WebView instance and to remove the reference
     * to it. After this method is called, the current
     * instance of the LightningView is useless as
     * the WebView cannot be recreated using the public
     * api.
     */
    // TODO fix bug where WebView.destroy is being called before the tab
    // is removed and would cause a memory leak if the parent check
    // was not in place.
    fun onDestroy() {
        networkDisposable.dispose()
        webView?.let { tab ->
            // Check to make sure the WebView has been removed
            // before calling destroy() so that a memory leak is not created
            val parent = tab.parent as? ViewGroup
            if (parent != null) {
                logger.log(TAG, "WebView was not detached from window before onDestroy")
                parent.removeView(webView)
            }
            tab.stopLoading()
            tab.onPause()
            tab.clearHistory()
            tab.visibility = View.GONE
            tab.removeAllViews()
            tab.destroyDrawingCache()
            tab.destroy()

            webView = null
        }
    }

    /**
     * Tell the WebView to navigate backwards
     * in its history to the previous page.
     */
    fun goBack() {
        webView?.goBack()
    }

    /**
     * Tell the WebView to navigate forwards
     * in its history to the next page.
     */
    fun goForward() {
        webView?.goForward()
    }

    /**
     * Notifies the [WebView] whether the network is available or not.
     */
    private fun setNetworkAvailable(isAvailable: Boolean) {
        webView?.setNetworkAvailable(isAvailable)
    }

    /**
     * Handles a long click on the page and delegates the URL to the
     * proper dialog if it is not null, otherwise, it tries to get the
     * URL using HitTestResult.
     *
     * @param url the url that should have been obtained from the WebView touch node
     * thingy, if it is null, this method tries to deal with it and find
     * a workaround.
     */
    private fun longClickPage(url: String?) {
        val result = webView?.hitTestResult
        val currentUrl = webView?.url
        val newUrl = result?.extra

        if (currentUrl != null && currentUrl.isSpecialUrl()) {
            if (currentUrl.isHistoryUrl()) {
                if (url != null) {
                    dialogBuilder.showLongPressedHistoryLinkDialog(activity, uiController, url)
                } else if (newUrl != null) {
                    dialogBuilder.showLongPressedHistoryLinkDialog(activity, uiController, newUrl)
                }
            } else if (currentUrl.isBookmarkUrl()) {
                if (url != null) {
                    dialogBuilder.showLongPressedDialogForBookmarkUrl(activity, uiController, url)
                } else if (newUrl != null) {
                    dialogBuilder.showLongPressedDialogForBookmarkUrl(activity, uiController, newUrl)
                }
            } else if (currentUrl.isDownloadsUrl()) {
                if (url != null) {
                    dialogBuilder.showLongPressedDialogForDownloadUrl(activity, uiController, url)
                } else if (newUrl != null) {
                    dialogBuilder.showLongPressedDialogForDownloadUrl(activity, uiController, newUrl)
                }
            }
        } else {
            if (url != null) {
                if (result != null) {
                    if (result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE || result.type == WebView.HitTestResult.IMAGE_TYPE) {
                        dialogBuilder.showLongPressImageDialog(activity, uiController, url, userAgent)
                    } else {
                        dialogBuilder.showLongPressLinkDialog(activity, uiController, url)
                    }
                } else {
                    dialogBuilder.showLongPressLinkDialog(activity, uiController, url)
                }
            } else if (newUrl != null) {
                if (result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE || result.type == WebView.HitTestResult.IMAGE_TYPE) {
                    dialogBuilder.showLongPressImageDialog(activity, uiController, newUrl, userAgent)
                } else {
                    dialogBuilder.showLongPressLinkDialog(activity, uiController, newUrl)
                }
            }
        }
    }

    /**
     * Determines whether or not the WebView can go
     * backward or if it as the end of its history.
     *
     * @return true if the WebView can go back, false otherwise.
     */
    fun canGoBack(): Boolean = webView?.canGoBack() == true

    /**
     * Determine whether or not the WebView can go
     * forward or if it is at the front of its history.
     *
     * @return true if it can go forward, false otherwise.
     */
    fun canGoForward(): Boolean = webView?.canGoForward() == true

    /**
     * Loads the URL in the WebView. If the proxy settings
     * are still initializing, then the URL will not load
     * as it is necessary to have the settings initialized
     * before a load occurs.
     *
     * @param url the non-null URL to attempt to load in
     * the WebView.
     */
    fun loadUrl(url: String) {
        // Check if configured proxy is available
        if (!proxyUtils.isProxyReady(activity)) {
            return
        }

        webView?.loadUrl(url, requestHeaders)
    }

    /**
     * The OnTouchListener used by the WebView so we can
     * get scroll events and show/hide the action bar when
     * the page is scrolled up/down.
     */
    private inner class TouchListener : OnTouchListener {

        internal var location: Float = 0f
        internal var y: Float = 0f
        internal var action: Int = 0

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View?, arg1: MotionEvent): Boolean {
            if (view == null) return false

            if (!view.hasFocus()) {
                view.requestFocus()
            }
            action = arg1.action
            y = arg1.y
            if (action == MotionEvent.ACTION_DOWN) {
                location = y
            } else if (action == MotionEvent.ACTION_UP) {
                val distance = y - location
                if (distance > SCROLL_UP_THRESHOLD && view.scrollY < SCROLL_UP_THRESHOLD) {
                    uiController.showActionBar()
                } else if (distance < -SCROLL_UP_THRESHOLD) {
                    uiController.hideActionBar()
                }
                location = 0f
            }
            gestureDetector.onTouchEvent(arg1)

            return false
        }
    }

    /**
     * The SimpleOnGestureListener used by the [TouchListener]
     * in order to delegate show/hide events to the action bar when
     * the user flings the page. Also handles long press events so
     * that we can capture them accurately.
     */
    private inner class CustomGestureListener : SimpleOnGestureListener() {

        /**
         * Without this, onLongPress is not called when user is zooming using
         * two fingers, but is when using only one.
         *
         *
         * The required behaviour is to not trigger this when the user is
         * zooming, it shouldn't matter how much fingers the user's using.
         */
        private var canTriggerLongPress = true

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            val power = (velocityY * 100 / maxFling).toInt()
            if (power < -10) {
                uiController.hideActionBar()
            } else if (power > 15) {
                uiController.showActionBar()
            }
            return super.onFling(e1, e2, velocityX, velocityY)
        }

        override fun onLongPress(e: MotionEvent) {
            if (canTriggerLongPress) {
                val msg = webViewHandler.obtainMessage()
                if (msg != null) {
                    msg.target = webViewHandler
                    webView?.requestFocusNodeHref(msg)
                }
            }
        }

        /**
         * Is called when the user is swiping after the doubletap, which in our
         * case means that he is zooming.
         */
        override fun onDoubleTapEvent(e: MotionEvent): Boolean {
            canTriggerLongPress = false
            return false
        }

        /**
         * Is called when something is starting being pressed, always before
         * onLongPress.
         */
        override fun onShowPress(e: MotionEvent) {
            canTriggerLongPress = true
        }
    }

    /**
     * A Handler used to get the URL from a long click
     * event on the WebView. It does not hold a hard
     * reference to the WebView and therefore will not
     * leak it if the WebView is garbage collected.
     */
    private class WebViewHandler(view: LightningView) : Handler() {

        private val reference: WeakReference<LightningView> = WeakReference(view)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val url = msg.data.getString("url")

            reference.get()?.longClickPage(url)
        }
    }

    companion object {

        private const val TAG = "LightningView"

        const val HEADER_REQUESTED_WITH = "X-Requested-With"
        const val HEADER_WAP_PROFILE = "X-Wap-Profile"
        private const val HEADER_DNT = "DNT"
        private const val HEADER_SAVEDATA = "Save-Data"

        private val API = Build.VERSION.SDK_INT
        private val SCROLL_UP_THRESHOLD = Utils.dpToPx(10f)

        private val negativeColorArray = floatArrayOf(
            -1.0f, 0f, 0f, 0f, 255f, // red
            0f, -1.0f, 0f, 0f, 255f, // green
            0f, 0f, -1.0f, 0f, 255f, // blue
            0f, 0f, 0f, 1.0f, 0f // alpha
        )
        private val increaseContrastColorArray = floatArrayOf(
            2.0f, 0f, 0f, 0f, -160f, // red
            0f, 2.0f, 0f, 0f, -160f, // green
            0f, 0f, 2.0f, 0f, -160f, // blue
            0f, 0f, 0f, 1.0f, 0f // alpha
        )
    }
}
