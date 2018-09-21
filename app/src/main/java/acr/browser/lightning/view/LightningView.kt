/*
 * Copyright 2014 A.C.R. Development
 */

package acr.browser.lightning.view

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.constant.DESKTOP_USER_AGENT
import acr.browser.lightning.constant.MOBILE_USER_AGENT
import acr.browser.lightning.constant.SCHEME_BOOKMARKS
import acr.browser.lightning.constant.SCHEME_HOMEPAGE
import acr.browser.lightning.controller.UIController
import acr.browser.lightning.di.DatabaseScheduler
import acr.browser.lightning.di.MainScheduler
import acr.browser.lightning.dialog.LightningDialogBuilder
import acr.browser.lightning.download.LightningDownloadListener
import acr.browser.lightning.html.bookmark.BookmarkPage
import acr.browser.lightning.html.download.DownloadsPage
import acr.browser.lightning.html.homepage.StartPage
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.ssl.SSLState
import acr.browser.lightning.utils.ProxyUtils
import acr.browser.lightning.utils.UrlUtils
import acr.browser.lightning.utils.Utils
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.Message
import android.support.v4.util.ArrayMap
import android.util.Log
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.View.OnTouchListener
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebSettings.LayoutAlgorithm
import android.webkit.WebView
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
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
    val isIncognito: Boolean
) {

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

    private var homepage: String
    private val maxFling: Float

    @Inject internal lateinit var userPreferences: UserPreferences
    @Inject internal lateinit var dialogBuilder: LightningDialogBuilder
    @Inject internal lateinit var proxyUtils: ProxyUtils
    @Inject @field:DatabaseScheduler internal lateinit var databaseScheduler: Scheduler
    @Inject @field:MainScheduler internal lateinit var mainScheduler: Scheduler

    private val lightningWebClient: LightningWebClient

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
    val favicon: Bitmap
        get() = titleInfo.getFavicon(uiController.getUseDarkTheme())

    /**
     * Get the current title of the page, retrieved from the title object.
     *
     * @return the title of the page, or an empty string if there is no title.
     */
    val title: String
        get() = titleInfo.getTitle() ?: ""

    /**
     * Get the current URL of the WebView, or an empty string if the WebView is null or the URL is
     * null.
     *
     * @return the current URL or an empty string.
     */
    val url: String
        get() = webView?.url ?: ""

    init {
        BrowserApp.appComponent.inject(this)
        uiController = activity as UIController
        val tab = WebView(activity).also { webView = it }

        homepage = userPreferences.homepage

        tab.id = View.generateViewId()
        titleInfo = LightningViewTitle(activity)

        maxFling = ViewConfiguration.get(activity).scaledMaximumFlingVelocity.toFloat()

        tab.drawingCacheBackgroundColor = Color.WHITE
        tab.isFocusableInTouchMode = true
        tab.isFocusable = true
        tab.isDrawingCacheEnabled = false
        tab.setWillNotCacheDrawing(true)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            tab.isAnimationCacheEnabled = false
            tab.isAlwaysDrawnWithCacheEnabled = false
        }
        tab.setBackgroundColor(Color.WHITE)

        tab.isScrollbarFadingEnabled = true
        tab.isSaveEnabled = true
        tab.setNetworkAvailable(true)
        tab.webChromeClient = LightningChromeClient(activity, this)
        lightningWebClient = LightningWebClient(activity, this)
        tab.webViewClient = lightningWebClient
        tab.setDownloadListener(LightningDownloadListener(activity))
        gestureDetector = GestureDetector(activity, CustomGestureListener())
        tab.setOnTouchListener(TouchListener())
        initializeSettings()
        initializePreferences(activity)

        tabInitializer.initialize(tab, requestHeaders)
    }

    fun currentSslState(): SSLState = lightningWebClient.sslState

    fun sslStateObservable(): Observable<SSLState> = lightningWebClient.sslStateObservable()

    /**
     * This method loads the homepage for the browser. Either it loads the URL stored as the
     * homepage, or loads the startpage or bookmark page if either of those are set as the homepage.
     */
    fun loadHomePage() {
        if (webView == null) {
            return
        }

        when (homepage) {
            SCHEME_HOMEPAGE -> loadStartPage()
            SCHEME_BOOKMARKS -> loadBookmarkPage()
            else -> webView?.loadUrl(homepage, requestHeaders)
        }
    }

    /**
     * This method gets the HomePage URL from the [StartPage] class asynchronously and loads the
     * URL in the WebView on the UI thread.
     */
    private fun loadStartPage() {
        StartPage()
            .createHomePage()
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribe(this::loadUrl)
    }

    /**
     * This method gets the bookmark page URL from the [BookmarkPage] class asynchronously and loads
     * the URL in the WebView on the UI thread. It also caches the default folder icon locally.
     */
    fun loadBookmarkPage() {
        BookmarkPage(activity)
            .createBookmarkPage()
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribe(this::loadUrl)
    }

    /**
     * This method gets the bookmark page URL from the [BookmarkPage] class asynchronously and loads
     * the URL in the WebView on the UI thread. It also caches the default folder icon locally.
     */
    fun loadDownloadsPage() {
        DownloadsPage()
            .getDownloadsPage()
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribe(this::loadUrl)
    }

    /**
     * Initialize the preference driven settings of the WebView. This method must be called whenever
     * the preferences are changed within SharedPreferences.
     *
     * @param context the context in which the WebView was created, it is used to get the default
     * UserAgent for the WebView.
     */
    @SuppressLint("NewApi", "SetJavaScriptEnabled")
    fun initializePreferences(context: Context) {
        val settings = webView?.settings ?: return

        lightningWebClient.updatePreferences()

        if (userPreferences.doNotTrackEnabled) {
            requestHeaders[HEADER_DNT] = "1"
        } else {
            requestHeaders.remove(HEADER_DNT)
        }

        if (userPreferences.removeIdentifyingHeadersEnabled) {
            requestHeaders[HEADER_REQUESTED_WITH] = ""
            requestHeaders[HEADER_WAP_PROFILE] = ""
        } else {
            requestHeaders.remove(HEADER_REQUESTED_WITH)
            requestHeaders.remove(HEADER_WAP_PROFILE)
        }

        settings.defaultTextEncodingName = userPreferences.textEncoding
        homepage = userPreferences.homepage
        setColorMode(userPreferences.renderingMode)

        if (!isIncognito) {
            settings.setGeolocationEnabled(userPreferences.locationEnabled)
        } else {
            settings.setGeolocationEnabled(false)
        }

        setUserAgent(context, userPreferences.userAgentChoice)

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
                Log.e(TAG, "Problem setting LayoutAlgorithm to TEXT_AUTOSIZING")
            }
        } else {
            settings.layoutAlgorithm = LayoutAlgorithm.NORMAL
        }

        settings.blockNetworkImage = userPreferences.blockImagesEnabled
        if (!isIncognito) {
            settings.setSupportMultipleWindows(userPreferences.popupsEnabled)
        } else {
            settings.setSupportMultipleWindows(false)
        }

        settings.useWideViewPort = userPreferences.useWideViewportEnabled
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
    private fun initializeSettings() {
        val settings = webView?.settings ?: return

        settings.mediaPlaybackRequiresUserGesture = true

        if (API >= Build.VERSION_CODES.LOLLIPOP && !isIncognito) {
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        } else if (API >= Build.VERSION_CODES.LOLLIPOP) {
            // We're in Incognito mode, reject
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }

        if (!isIncognito) {
            settings.domStorageEnabled = true
            settings.setAppCacheEnabled(true)
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.databaseEnabled = true
        } else {
            settings.domStorageEnabled = false
            settings.setAppCacheEnabled(false)
            settings.databaseEnabled = false
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
        }

        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.allowContentAccess = true
        settings.allowFileAccess = true
        settings.allowFileAccessFromFileURLs = false
        settings.allowUniversalAccessFromFileURLs = false

        getPathObservable("appcache")
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribe { file ->
                settings.setAppCachePath(file.path)
            }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            getPathObservable("geolocation")
                .subscribeOn(databaseScheduler)
                .observeOn(mainScheduler)
                .subscribe { file ->
                    settings.setGeolocationDatabasePath(file.path)
                }
        }

    }

    private fun getPathObservable(subFolder: String) = Single.fromCallable {
        activity.getDir(subFolder, 0)
    }

    /**
     * This method is used to toggle the user agent between desktop and the current preference of
     * the user.
     *
     * @param context the Context needed to set the user agent
     */
    fun toggleDesktopUA(context: Context) {
        if (!toggleDesktop) {
            webView?.settings?.userAgentString = DESKTOP_USER_AGENT
        } else {
            setUserAgent(context, userPreferences.userAgentChoice)
        }

        toggleDesktop = !toggleDesktop
    }

    /**
     * This method sets the user agent of the current tab. There are four options, 1, 2, 3, 4.
     *
     * 1. use the default user agent
     * 2. use the desktop user agent
     * 3. use the mobile user agent
     * 4. use a custom user agent, or the default user agent if none was set.
     *
     * @param context the context needed to get the default user agent.
     * @param choice  the choice of user agent to use, see above comments.
     */
    @SuppressLint("NewApi")
    private fun setUserAgent(context: Context, choice: Int) {
        val settings = webView?.settings ?: return

        when (choice) {
            1 -> settings.userAgentString = WebSettings.getDefaultUserAgent(context)
            2 -> settings.userAgentString = DESKTOP_USER_AGENT
            3 -> settings.userAgentString = MOBILE_USER_AGENT
            4 -> {
                var ua = userPreferences.userAgentString
                if (ua.isEmpty()) {
                    ua = " "
                }
                settings.userAgentString = ua
            }
        }
    }

    /**
     * Pause the current WebView instance.
     */
    fun onPause() {
        webView?.onPause()
        Log.d(TAG, "WebView onPause: " + webView?.id)
    }

    /**
     * Resume the current WebView instance.
     */
    fun onResume() {
        webView?.onResume()
        Log.d(TAG, "WebView onResume: " + webView?.id)
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
     * rendering (0), inverted rendering (1), grayscale rendering (2),
     * and inverted grayscale rendering (3)
     *
     * @param mode the integer mode to set as the rendering mode.
     * see the numbers in documentation above for the
     * values this method accepts.
     */
    private fun setColorMode(mode: Int) {
        invertPage = false
        when (mode) {
            0 -> {
                paint.colorFilter = null
                // setSoftwareRendering(); // Some devices get segfaults
                // in the WebView with Hardware Acceleration enabled,
                // the only fix is to disable hardware rendering
                setNormalRendering()
                invertPage = false
            }
            1 -> {
                val filterInvert = ColorMatrixColorFilter(
                    sNegativeColorArray)
                paint.colorFilter = filterInvert
                setHardwareRendering()

                invertPage = true
            }
            2 -> {
                val cm = ColorMatrix()
                cm.setSaturation(0f)
                val filterGray = ColorMatrixColorFilter(cm)
                paint.colorFilter = filterGray
                setHardwareRendering()
            }
            3 -> {
                val matrix = ColorMatrix()
                matrix.set(sNegativeColorArray)
                val matrixGray = ColorMatrix()
                matrixGray.setSaturation(0f)
                val concat = ColorMatrix()
                concat.setConcat(matrix, matrixGray)
                val filterInvertGray = ColorMatrixColorFilter(concat)
                paint.colorFilter = filterInvertGray
                setHardwareRendering()

                invertPage = true
            }

            4 -> {
                val increaseHighContrast = ColorMatrixColorFilter(sIncreaseContrastColorArray)
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
        Log.d(TAG, "Pausing JS timers")
    }

    /**
     * Resumes the JavaScript timers of the
     * WebView instance, which will trigger a
     * resume for all WebViews in the app.
     */
    fun resumeTimers() {
        webView?.resumeTimers()
        Log.d(TAG, "Resuming JS timers")
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
    fun find(text: String) {
        webView?.findAllAsync(text)
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
        webView?.let { tab ->
            // Check to make sure the WebView has been removed
            // before calling destroy() so that a memory leak is not created
            val parent = tab.parent as? ViewGroup
            if (parent != null) {
                Log.e(TAG, "WebView was not detached from window before onDestroy")
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
     * Move the highlighted text in the WebView
     * to the next matched text. This method will
     * only have an affect after [LightningView.find]
     * is called. Otherwise it will do nothing.
     */
    fun findNext() {
        webView?.findNext(true)
    }

    /**
     * Move the highlighted text in the WebView
     * to the previous matched text. This method will
     * only have an affect after [LightningView.find]
     * is called. Otherwise it will do nothing.
     */
    fun findPrevious() {
        webView?.findNext(false)
    }

    /**
     * Clear the highlighted text in the WebView after
     * [LightningView.find] has been called.
     * Otherwise it will have no affect.
     */
    fun clearFindMatches() {
        webView?.clearMatches()
    }

    /**
     * Notifies the [WebView] whether the network is available or not.
     */
    fun setNetworkAvailable(isAvailable: Boolean) {
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

        if (currentUrl != null && UrlUtils.isSpecialUrl(currentUrl)) {
            if (UrlUtils.isHistoryUrl(currentUrl)) {
                if (url != null) {
                    dialogBuilder.showLongPressedHistoryLinkDialog(activity, uiController, url)
                } else if (result != null && result.extra != null) {
                    val newUrl = result.extra
                    dialogBuilder.showLongPressedHistoryLinkDialog(activity, uiController, newUrl)
                }
            } else if (UrlUtils.isBookmarkUrl(currentUrl)) {
                if (url != null) {
                    dialogBuilder.showLongPressedDialogForBookmarkUrl(activity, uiController, url)
                } else if (result != null && result.extra != null) {
                    val newUrl = result.extra
                    dialogBuilder.showLongPressedDialogForBookmarkUrl(activity, uiController, newUrl)
                }
            } else if (UrlUtils.isDownloadsUrl(currentUrl)) {
                if (url != null) {
                    dialogBuilder.showLongPressedDialogForDownloadUrl(activity, uiController, url)
                } else if (result != null && result.extra != null) {
                    val newUrl = result.extra
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
            } else if (result != null && result.extra != null) {
                val newUrl = result.extra
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

        private val API = android.os.Build.VERSION.SDK_INT
        private val SCROLL_UP_THRESHOLD = Utils.dpToPx(10f)

        private val sNegativeColorArray = floatArrayOf(-1.0f, 0f, 0f, 0f, 255f, // red
            0f, -1.0f, 0f, 0f, 255f, // green
            0f, 0f, -1.0f, 0f, 255f, // blue
            0f, 0f, 0f, 1.0f, 0f // alpha
        )
        private val sIncreaseContrastColorArray = floatArrayOf(2.0f, 0f, 0f, 0f, -160f, // red
            0f, 2.0f, 0f, 0f, -160f, // green
            0f, 0f, 2.0f, 0f, -160f, // blue
            0f, 0f, 0f, 1.0f, 0f // alpha
        )
    }
}
