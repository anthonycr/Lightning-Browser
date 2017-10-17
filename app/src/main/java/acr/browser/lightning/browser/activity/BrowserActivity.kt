/*
 * Copyright 2015 Anthony Restaino
 */

package acr.browser.lightning.browser.activity

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.IncognitoActivity
import acr.browser.lightning.R
import acr.browser.lightning.browser.*
import acr.browser.lightning.browser.fragment.BookmarksFragment
import acr.browser.lightning.browser.fragment.TabsFragment
import acr.browser.lightning.constant.LOAD_READING_URL
import acr.browser.lightning.controller.UIController
import acr.browser.lightning.database.HistoryItem
import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.database.history.HistoryRepository
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.dialog.DialogItem
import acr.browser.lightning.dialog.LightningDialogBuilder
import acr.browser.lightning.extensions.doOnLayout
import acr.browser.lightning.extensions.removeFromParent
import acr.browser.lightning.extensions.resizeAndShow
import acr.browser.lightning.html.download.DownloadsPage
import acr.browser.lightning.html.history.HistoryPage
import acr.browser.lightning.interpolator.BezierDecelerateInterpolator
import acr.browser.lightning.network.NetworkObservable
import acr.browser.lightning.notifications.IncognitoNotification
import acr.browser.lightning.reading.activity.ReadingActivity
import acr.browser.lightning.search.SearchEngineProvider
import acr.browser.lightning.search.SuggestionsAdapter
import acr.browser.lightning.settings.activity.SettingsActivity
import acr.browser.lightning.ssl.SSLState
import acr.browser.lightning.utils.*
import acr.browser.lightning.view.Handlers
import acr.browser.lightning.view.LightningView
import acr.browser.lightning.view.SearchView
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.provider.MediaStore
import android.support.annotation.ColorInt
import android.support.annotation.IdRes
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v4.widget.DrawerLayout.DrawerListener
import android.support.v7.app.AlertDialog
import android.support.v7.graphics.Palette
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.View.*
import android.view.ViewGroup.LayoutParams
import android.view.animation.Animation
import android.view.animation.Transformation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.CustomViewCallback
import android.webkit.WebIconDatabase
import android.webkit.WebView
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.TextView.OnEditorActionListener
import butterknife.ButterKnife
import com.anthonycr.bonsai.Completable
import com.anthonycr.bonsai.Schedulers
import com.anthonycr.bonsai.SingleOnSubscribe
import com.anthonycr.grant.PermissionsManager
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.browser_content.*
import kotlinx.android.synthetic.main.search_interface.*
import kotlinx.android.synthetic.main.toolbar.*
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

abstract class BrowserActivity : ThemableBrowserActivity(), BrowserView, UIController, OnClickListener {

    // Toolbar Views
    private var searchBackground: View? = null
    private var searchView: SearchView? = null
    private var arrowImageView: ImageView? = null

    // Current tab view being displayed
    private var currentTabView: View? = null

    // Full Screen Video Views
    private var fullscreenContainerView: FrameLayout? = null
    private var videoView: VideoView? = null
    private var customView: View? = null

    // Adapter
    private var suggestionsAdapter: SuggestionsAdapter? = null

    // Callback
    private var customViewCallback: CustomViewCallback? = null
    private var uploadMessageCallback: ValueCallback<Uri>? = null
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    // Primitives
    private var isFullScreen: Boolean = false
    private var hideStatusBar: Boolean = false
    private var isDarkTheme: Boolean = false
    private var isImmersiveMode = false
    private var shouldShowTabsInDrawer: Boolean = false
    private var swapBookmarksAndTabs: Boolean = false

    private var originalOrientation: Int = 0
    private var backgroundColor: Int = 0
    private var iconColor: Int = 0
    private var disabledIconColor: Int = 0
    private var currentUiColor = Color.BLACK
    private var keyDownStartTime: Long = 0
    private var searchText: String? = null
    private var cameraPhotoPath: String? = null

    // The singleton BookmarkManager
    @Inject internal lateinit var bookmarkManager: BookmarkRepository
    @Inject internal lateinit var historyModel: HistoryRepository
    @Inject internal lateinit var bookmarksDialogBuilder: LightningDialogBuilder
    @Inject internal lateinit var searchBoxModel: SearchBoxModel
    @Inject internal lateinit var searchEngineProvider: SearchEngineProvider
    @Inject internal lateinit var networkObservable: NetworkObservable
    @Inject @field:Named("database") internal lateinit var databaseScheduler: Scheduler

    private val tabsManager: TabsManager = TabsManager()

    // Image
    private var webPageBitmap: Bitmap? = null
    private val backgroundDrawable = ColorDrawable()
    private var deleteIconDrawable: Drawable? = null
    private var refreshIconDrawable: Drawable? = null
    private var clearIconDrawable: Drawable? = null
    private var iconDrawable: Drawable? = null
    private var sslDrawable: Drawable? = null

    private var presenter: BrowserPresenter? = null
    private var tabsView: TabsView? = null
    private var bookmarksView: BookmarksView? = null

    // Proxy
    @Inject internal lateinit var proxyUtils: ProxyUtils

    /**
     * Determines if the current browser instance is in incognito mode or not.
     */
    protected abstract val isIncognito: Boolean

    private val bookmarksFragmentViewId: Int
        @IdRes
        get() = if (swapBookmarksAndTabs) R.id.left_drawer else R.id.right_drawer

    private val tabsFragmentViewId: Int
        get() = if (shouldShowTabsInDrawer) {
            if (swapBookmarksAndTabs) R.id.right_drawer else R.id.left_drawer
        } else {
            R.id.tabs_toolbar_container
        }

    private val longPressBackRunnable = Runnable {
        tabsManager.let {
            val currentTab = it.currentTab
            showCloseDialog(it.positionOf(currentTab))
        }
    }

    private val bookmarkDrawer: View
        get() = if (swapBookmarksAndTabs) left_drawer else right_drawer

    private val tabDrawer: View
        get() = if (swapBookmarksAndTabs) right_drawer else left_drawer

    private var backMenuItem: MenuItem? = null
    private var forwardMenuItem: MenuItem? = null


    /**
     * This EventListener notifies each of the WebViews in the browser whether
     * the network is currently connected or not. This is important because some
     * JavaScript properties rely on the WebView knowing the current network state.
     * It is used to help the browser be compliant with the HTML5 spec, sec. 5.7.7
     */
    private val networkListener = object : NetworkObservable.EventListener {
        override fun onNetworkConnectionChange(connected: Boolean) {
            Log.d(TAG, "Network Connected: " + connected)
            tabsManager.notifyConnectionStatus(connected)
        }
    }

    abstract override fun closeActivity()

    abstract override fun updateHistory(title: String?, url: String)

    protected abstract fun updateCookiePreference(): Completable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BrowserApp.appComponent.inject(this)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)

        val incognitoNotification = IncognitoNotification(this)
        tabsManager.addTabNumberChangedListener {
            if (isIncognito) {
                if (it == 0) {
                    incognitoNotification.hide()
                } else {
                    incognitoNotification.show(it)
                }
            }
        }

        presenter = BrowserPresenter(this, isIncognito)

        initialize(savedInstanceState)
    }

    @Synchronized
    private fun initialize(savedInstanceState: Bundle?) {
        initializeToolbarHeight(resources.configuration)
        setSupportActionBar(toolbar)
        val actionBar = requireNotNull(supportActionBar)

        //TODO make sure dark theme flag gets set correctly
        isDarkTheme = preferences.useTheme != 0 || isIncognito
        iconColor = if (isDarkTheme) ThemeUtils.getIconDarkThemeColor(this) else ThemeUtils.getIconLightThemeColor(this)
        disabledIconColor = if (isDarkTheme) {
            ContextCompat.getColor(this, R.color.icon_dark_theme_disabled)
        } else {
            ContextCompat.getColor(this, R.color.icon_light_theme_disabled)
        }
        shouldShowTabsInDrawer = preferences.getShowTabsInDrawer(!isTablet)
        swapBookmarksAndTabs = preferences.bookmarksAndTabsSwapped

        // initialize background ColorDrawable
        val primaryColor = ThemeUtils.getPrimaryColor(this)
        backgroundDrawable.color = primaryColor

        // Drawer stutters otherwise
        left_drawer.setLayerType(View.LAYER_TYPE_NONE, null)
        right_drawer.setLayerType(View.LAYER_TYPE_NONE, null)

        drawer_layout.addDrawerListener(object : DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit

            override fun onDrawerOpened(drawerView: View) = Unit

            override fun onDrawerClosed(drawerView: View) = Unit

            override fun onDrawerStateChanged(newState: Int) {
                if (newState == DrawerLayout.STATE_DRAGGING) {
                    left_drawer.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    right_drawer.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                } else if (newState == DrawerLayout.STATE_IDLE) {
                    left_drawer.setLayerType(View.LAYER_TYPE_NONE, null)
                    right_drawer.setLayerType(View.LAYER_TYPE_NONE, null)
                }
            }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !shouldShowTabsInDrawer) {
            window.statusBarColor = Color.BLACK
        }

        setNavigationDrawerWidth()
        drawer_layout.addDrawerListener(DrawerLocker())

        webPageBitmap = ThemeUtils.getThemedBitmap(this, R.drawable.ic_webpage, isDarkTheme)

        val fragmentManager = supportFragmentManager

        val tabsFragment: TabsFragment? = fragmentManager.findFragmentByTag(TAG_TABS_FRAGMENT) as? TabsFragment
        val bookmarksFragment: BookmarksFragment? = fragmentManager.findFragmentByTag(TAG_BOOKMARK_FRAGMENT) as? BookmarksFragment

        if (tabsFragment != null) {
            fragmentManager.beginTransaction().remove(tabsFragment).commit()
        }

        tabsView = tabsFragment ?: TabsFragment.createTabsFragment(isIncognito, shouldShowTabsInDrawer)

        if (bookmarksFragment != null) {
            fragmentManager.beginTransaction().remove(bookmarksFragment).commit()
        }

        bookmarksView = bookmarksFragment ?: BookmarksFragment.createFragment(isIncognito)

        fragmentManager.executePendingTransactions()

        fragmentManager
                .beginTransaction()
                .replace(tabsFragmentViewId, tabsView as Fragment, TAG_TABS_FRAGMENT)
                .replace(bookmarksFragmentViewId, bookmarksView as Fragment, TAG_BOOKMARK_FRAGMENT)
                .commit()
        if (shouldShowTabsInDrawer) {
            toolbar_layout.removeView(findViewById(R.id.tabs_toolbar_container))
        }

        // set display options of the ActionBar
        actionBar.setDisplayShowTitleEnabled(false)
        actionBar.setDisplayShowHomeEnabled(false)
        actionBar.setDisplayShowCustomEnabled(true)
        actionBar.setCustomView(R.layout.toolbar_content)

        val customView = actionBar.customView
        val lp = customView.layoutParams
        lp.width = LayoutParams.MATCH_PARENT
        lp.height = LayoutParams.MATCH_PARENT
        customView.layoutParams = lp

        arrowImageView = customView.findViewById<ImageView>(R.id.arrow).also {
            if (shouldShowTabsInDrawer) {
                if (it.width <= 0) {
                    it.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                }
                updateTabNumber(0)

                // Post drawer locking in case the activity is being recreated
                Handlers.MAIN.post { drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, tabDrawer) }
            } else {

                // Post drawer locking in case the activity is being recreated
                Handlers.MAIN.post { drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, tabDrawer) }
                it.setImageResource(R.drawable.ic_action_home)
                it.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
            }
        }

        // Post drawer locking in case the activity is being recreated
        Handlers.MAIN.post { drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, bookmarkDrawer) }

        customView.findViewById<FrameLayout>(R.id.arrow_button).setOnClickListener(this)

        val iconBounds = Utils.dpToPx(24f)
        backgroundColor = ThemeUtils.getPrimaryColor(this)
        deleteIconDrawable = ThemeUtils.getThemedDrawable(this, R.drawable.ic_action_delete, isDarkTheme).apply {
            setBounds(0, 0, iconBounds, iconBounds)
        }
        refreshIconDrawable = ThemeUtils.getThemedDrawable(this, R.drawable.ic_action_refresh, isDarkTheme).apply {
            setBounds(0, 0, iconBounds, iconBounds)
        }
        clearIconDrawable = ThemeUtils.getThemedDrawable(this, R.drawable.ic_action_delete, isDarkTheme).apply {
            setBounds(0, 0, iconBounds, iconBounds)
        }

        // create the search EditText in the ToolBar
        searchView = customView.findViewById<SearchView>(R.id.search).apply {
            setHintTextColor(ThemeUtils.getThemedTextHintColor(isDarkTheme))
            setTextColor(if (isDarkTheme) Color.WHITE else Color.BLACK)
            iconDrawable = refreshIconDrawable
            compoundDrawablePadding = Utils.dpToPx(3f)
            setCompoundDrawablesWithIntrinsicBounds(sslDrawable, null, refreshIconDrawable, null)

            val searchListener = SearchListenerClass()
            setOnKeyListener(searchListener)
            onFocusChangeListener = searchListener
            setOnEditorActionListener(searchListener)
            setOnTouchListener(searchListener)
            onPreFocusListener = searchListener

            initializeSearchSuggestions(this)
        }

        searchBackground = customView.findViewById<View>(R.id.search_container).apply {
            // initialize search background color
            background.setColorFilter(getSearchBarColor(primaryColor, primaryColor), PorterDuff.Mode.SRC_IN)
        }

        drawer_layout.setDrawerShadow(R.drawable.drawer_right_shadow, GravityCompat.END)
        drawer_layout.setDrawerShadow(R.drawable.drawer_left_shadow, GravityCompat.START)

        if (API <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            WebIconDatabase.getInstance().open(getDir("icons", Context.MODE_PRIVATE).path)
        }

        var intent: Intent? = if (savedInstanceState == null) intent else null

        val launchedFromHistory = intent != null && intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0

        if (isPanicTrigger(intent)) {
            setIntent(null)
            panicClean()
        } else {
            if (launchedFromHistory) {
                intent = null
            }
            presenter?.setupTabs(intent)
            setIntent(null)
            proxyUtils.checkForProxy(this)
        }
    }

    protected fun panicClean() {
        Log.d(TAG, "Closing browser")
        tabsManager.let {
            it.newTab(this, "", false)
            it.switchToTab(0)
            it.clearSavedState()
        }

        HistoryPage.deleteHistoryPage(application).subscribe()
        closeBrowser()
        // System exit needed in the case of receiving
        // the panic intent since finish() isn't completely
        // closing the browser
        System.exit(1)
    }

    private inner class SearchListenerClass : OnKeyListener, OnEditorActionListener, OnFocusChangeListener, OnTouchListener, SearchView.PreFocusListener {

        override fun onKey(view: View, keyCode: Int, keyEvent: KeyEvent): Boolean {

            when (keyCode) {
                KeyEvent.KEYCODE_ENTER -> {
                    searchView?.let {
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(it.windowToken, 0)
                        searchTheWeb(it.text.toString())
                    }

                    tabsManager.currentTab?.requestFocus()
                    return true
                }
                else -> {
                }
            }
            return false
        }

        override fun onEditorAction(arg0: TextView, actionId: Int, arg2: KeyEvent?): Boolean {
            // hide the keyboard and search the web when the enter key
            // button is pressed
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_NEXT
                    || actionId == EditorInfo.IME_ACTION_SEND
                    || actionId == EditorInfo.IME_ACTION_SEARCH
                    || arg2?.action == KeyEvent.KEYCODE_ENTER) {
                searchView?.let {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(it.windowToken, 0)
                    searchTheWeb(it.text.toString())
                }

                tabsManager.currentTab?.requestFocus()
                return true
            }
            return false
        }

        override fun onFocusChange(v: View, hasFocus: Boolean) {
            val currentView = tabsManager.currentTab
            if (!hasFocus && currentView != null) {
                setIsLoading(currentView.progress < 100)
                updateUrl(currentView.url, false)
            } else if (hasFocus && currentView != null) {

                // Hack to make sure the text gets selected
                (v as SearchView).selectAll()
                iconDrawable = clearIconDrawable
                searchView?.setCompoundDrawablesWithIntrinsicBounds(null, null, clearIconDrawable, null)
            }

            if (!hasFocus) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                searchView?.let {
                    imm.hideSoftInputFromWindow(it.windowToken, 0)
                }
            }
        }

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            searchView?.let {
                if (it.compoundDrawables[2] != null) {
                    val iconWidth = iconDrawable?.intrinsicWidth ?: 0
                    val tappedX = event.x > (it.width - it.paddingRight - iconWidth)
                    if (tappedX) {
                        if (event.action == MotionEvent.ACTION_UP) {
                            if (it.hasFocus()) {
                                it.setText("")
                            } else {
                                refreshOrStop()
                            }
                        }
                        return true
                    }
                }
            }

            return false
        }

        override fun onPreFocus() {
            val currentView = tabsManager.currentTab ?: return
            val url = currentView.url
            if (!UrlUtils.isSpecialUrl(url)) {
                if (searchView?.hasFocus() == false) {
                    searchView?.setText(url)
                }
            }
        }
    }

    private inner class DrawerLocker : DrawerListener {

        override fun onDrawerClosed(v: View) {
            val tabsDrawer = tabDrawer
            val bookmarksDrawer = bookmarkDrawer

            if (v === tabsDrawer) {
                drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, bookmarksDrawer)
            } else if (shouldShowTabsInDrawer) {
                drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, tabsDrawer)
            }
        }

        override fun onDrawerOpened(v: View) {
            val tabsDrawer = tabDrawer
            val bookmarksDrawer = bookmarkDrawer

            if (v === tabsDrawer) {
                drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, bookmarksDrawer)
            } else {
                drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, tabsDrawer)
            }
        }

        override fun onDrawerSlide(v: View, arg: Float) = Unit

        override fun onDrawerStateChanged(arg: Int) = Unit

    }

    private fun setNavigationDrawerWidth() {
        val width = resources.displayMetrics.widthPixels - Utils.dpToPx(56f)
        val maxWidth = if (isTablet) {
            Utils.dpToPx(320f)
        } else {
            Utils.dpToPx(300f)
        }
        if (width > maxWidth) {
            val params = left_drawer
                    .layoutParams as android.support.v4.widget.DrawerLayout.LayoutParams
            params.width = maxWidth
            left_drawer.layoutParams = params
            left_drawer.requestLayout()
            val paramsRight = right_drawer
                    .layoutParams as android.support.v4.widget.DrawerLayout.LayoutParams
            paramsRight.width = maxWidth
            right_drawer.layoutParams = paramsRight
            right_drawer.requestLayout()
        } else {
            val params = left_drawer
                    .layoutParams as android.support.v4.widget.DrawerLayout.LayoutParams
            params.width = width
            left_drawer.layoutParams = params
            left_drawer.requestLayout()
            val paramsRight = right_drawer
                    .layoutParams as android.support.v4.widget.DrawerLayout.LayoutParams
            paramsRight.width = width
            right_drawer.layoutParams = paramsRight
            right_drawer.requestLayout()
        }
    }

    private fun initializePreferences() {
        val currentView = tabsManager.currentTab
        isFullScreen = preferences.fullScreenEnabled
        val colorMode = preferences.colorModeEnabled && !isDarkTheme

        webPageBitmap?.let { webBitmap ->
            if (!isIncognito && !colorMode && !isDarkTheme) {
                changeToolbarBackground(webBitmap, null)
            } else if (!isIncognito && currentView != null && !isDarkTheme) {
                changeToolbarBackground(currentView.favicon, null)
            } else if (!isIncognito && !isDarkTheme) {
                changeToolbarBackground(webBitmap, null)
            }
        }

        val manager = supportFragmentManager
        val tabsFragment = manager.findFragmentByTag(TAG_TABS_FRAGMENT)
        (tabsFragment as? TabsFragment)?.reinitializePreferences()
        val bookmarksFragment = manager.findFragmentByTag(TAG_BOOKMARK_FRAGMENT)
        (bookmarksFragment as? BookmarksFragment)?.reinitializePreferences()

        // TODO layout transition causing memory leak
        //        content_frame.setLayoutTransition(new LayoutTransition());

        setFullscreen(preferences.hideStatusBarEnabled, false)

        val currentSearchEngine = searchEngineProvider.getCurrentSearchEngine()
        searchText = currentSearchEngine.queryUrl

        updateCookiePreference().subscribeOn(Schedulers.worker()).subscribe()
        proxyUtils.updateProxySettings(this)
    }

    public override fun onWindowVisibleToUserAfterResume() {
        super.onWindowVisibleToUserAfterResume()
        toolbar_layout.translationY = 0f
        setWebViewTranslation(toolbar_layout.height.toFloat())
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (searchView?.hasFocus() == true) {
                searchView?.let { searchTheWeb(it.text.toString()) }
            }
        } else if (keyCode == KeyEvent.KEYCODE_MENU
                && Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN
                && Build.MANUFACTURER.compareTo("LGE") == 0) {
            // Workaround for stupid LG devices that crash
            return true
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            keyDownStartTime = System.currentTimeMillis()
            Handlers.MAIN.postDelayed(longPressBackRunnable, ViewConfiguration.getLongPressTimeout().toLong())
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU
                && Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN
                && Build.MANUFACTURER.compareTo("LGE") == 0) {
            // Workaround for stupid LG devices that crash
            openOptionsMenu()
            return true
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            Handlers.MAIN.removeCallbacks(longPressBackRunnable)
            if (System.currentTimeMillis() - keyDownStartTime > ViewConfiguration.getLongPressTimeout()) {
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Keyboard shortcuts
        if (event.action == KeyEvent.ACTION_DOWN) {
            when {
                event.isCtrlPressed -> when (event.keyCode) {
                    KeyEvent.KEYCODE_F -> {
                        // Search in page
                        findInPage()
                        return true
                    }
                    KeyEvent.KEYCODE_T -> {
                        // Open new tab
                        newTab(null, true)
                        return true
                    }
                    KeyEvent.KEYCODE_W -> {
                        // Close current tab
                        tabsManager.let { presenter?.deleteTab(it.indexOfCurrentTab()) }
                        return true
                    }
                    KeyEvent.KEYCODE_Q -> {
                        // Close browser
                        closeBrowser()
                        return true
                    }
                    KeyEvent.KEYCODE_R -> {
                        // Refresh current tab
                        tabsManager.currentTab?.reload()
                        return true
                    }
                    KeyEvent.KEYCODE_TAB -> {
                        tabsManager.let {
                            val nextIndex = if (event.isShiftPressed) {
                                // Go back one tab
                                if (it.indexOfCurrentTab() > 0) {
                                    it.indexOfCurrentTab() - 1
                                } else {
                                    it.last()
                                }
                            } else {
                                // Go forward one tab
                                if (it.indexOfCurrentTab() < it.last()) {
                                    it.indexOfCurrentTab() + 1
                                } else {
                                    0
                                }
                            }

                            presenter?.tabChanged(nextIndex)
                        }

                        return true
                    }
                }
                event.keyCode == KeyEvent.KEYCODE_SEARCH -> {
                    // Highlight search field
                    searchView?.requestFocus()
                    searchView?.selectAll()
                    return true
                }
                event.isAltPressed -> // Alt + tab number
                    tabsManager.let {
                        if (KeyEvent.KEYCODE_0 <= event.keyCode && event.keyCode <= KeyEvent.KEYCODE_9) {
                            val nextIndex = if (event.keyCode > it.last() + KeyEvent.KEYCODE_1 || event.keyCode == KeyEvent.KEYCODE_0) {
                                it.last()
                            } else {
                                event.keyCode - KeyEvent.KEYCODE_1
                            }
                            presenter?.tabChanged(nextIndex)
                            return true
                        }
                    }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val currentView = tabsManager.currentTab
        val currentUrl = currentView?.url
        // Handle action buttons
        when (item.itemId) {
            android.R.id.home -> {
                if (drawer_layout.isDrawerOpen(bookmarkDrawer)) {
                    drawer_layout.closeDrawer(bookmarkDrawer)
                }
                return true
            }
            R.id.action_back -> {
                if (currentView != null && currentView.canGoBack()) {
                    currentView.goBack()
                }
                return true
            }
            R.id.action_forward -> {
                if (currentView != null && currentView.canGoForward()) {
                    currentView.goForward()
                }
                return true
            }
            R.id.action_add_to_homescreen -> {
                if (currentView != null) {
                    val shortcut = HistoryItem(currentView.url, currentView.title)
                    shortcut.bitmap = currentView.favicon
                    Utils.createShortcut(this, shortcut)
                }
                return true
            }
            R.id.action_new_tab -> {
                newTab(null, true)
                return true
            }
            R.id.action_incognito -> {
                startActivity(IncognitoActivity.createIntent(this))
                overridePendingTransition(R.anim.slide_up_in, R.anim.fade_out_scale)
                return true
            }
            R.id.action_share -> {
                IntentUtils(this).shareUrl(currentUrl, currentView?.title)
                return true
            }
            R.id.action_bookmarks -> {
                openBookmarks()
                return true
            }
            R.id.action_copy -> {
                if (currentUrl != null && !UrlUtils.isSpecialUrl(currentUrl)) {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("label", currentUrl)
                    clipboard.primaryClip = clip
                    Utils.showSnackbar(this, R.string.message_link_copied)
                }
                return true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
            R.id.action_history -> {
                openHistory()
                return true
            }
            R.id.action_downloads -> {
                openDownloads()
                return true
            }
            R.id.action_add_bookmark -> {
                if (currentUrl != null && !UrlUtils.isSpecialUrl(currentUrl)) {
                    addBookmark(currentView.title, currentUrl)
                }
                return true
            }
            R.id.action_find -> {
                findInPage()
                return true
            }
            R.id.action_reading_mode -> {
                if (currentUrl != null) {
                    val read = Intent(this, ReadingActivity::class.java)
                    read.putExtra(LOAD_READING_URL, currentUrl)
                    startActivity(read)
                }
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    // By using a manager, adds a bookmark and notifies third parties about that
    private fun addBookmark(title: String, url: String) {

        val item = HistoryItem(url, title)
        bookmarkManager.addBookmarkIfNotExists(item)
                .subscribeOn(databaseScheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { boolean ->
                    if (boolean) {
                        suggestionsAdapter?.refreshBookmarks()
                        bookmarksView?.handleUpdatedUrl(url)
                        Utils.showToast(this@BrowserActivity, R.string.message_bookmark_added)
                    }
                }
    }

    private fun deleteBookmark(title: String, url: String) {
        val item = HistoryItem(url, title)

        bookmarkManager.deleteBookmark(item)
                .subscribeOn(databaseScheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { boolean ->
                    if (boolean) {
                        suggestionsAdapter?.refreshBookmarks()
                        bookmarksView?.handleUpdatedUrl(url)
                    }
                }
    }

    private fun putToolbarInRoot() {
        if (toolbar_layout.parent !== ui_layout) {
            if (toolbar_layout.parent != null) {
                (toolbar_layout.parent as ViewGroup).removeView(toolbar_layout)
            }

            ui_layout.addView(toolbar_layout, 0)
            ui_layout.requestLayout()
        }
        setWebViewTranslation(0f)
    }

    private fun overlayToolbarOnWebView() {
        if (toolbar_layout.parent !== content_frame) {
            if (toolbar_layout.parent != null) {
                (toolbar_layout.parent as ViewGroup).removeView(toolbar_layout)
            }

            content_frame.addView(toolbar_layout)
            content_frame.requestLayout()
        }
        setWebViewTranslation(toolbar_layout.height.toFloat())
    }

    private fun setWebViewTranslation(translation: Float) {
        if (isFullScreen && currentTabView != null) {
            currentTabView?.translationY = translation
        } else if (currentTabView != null) {
            currentTabView?.translationY = 0f
        }
    }

    /**
     * method that shows a dialog asking what string the user wishes to search
     * for. It highlights the text entered.
     */
    private fun findInPage() = BrowserDialog.showEditText(this,
            R.string.action_find,
            R.string.search_hint,
            R.string.search_hint, object : BrowserDialog.EditorListener {
        override fun onClick(text: String) {
            if (!TextUtils.isEmpty(text)) {
                presenter?.findInPage(text)
                showFindInPageControls(text)
            }
        }
    })

    private fun showFindInPageControls(text: String) {
        search_bar.visibility = View.VISIBLE

        val tw = findViewById<TextView>(R.id.search_query)
        tw.text = "'$text'"

        val up = findViewById<ImageButton>(R.id.button_next)
        up.setOnClickListener(this)

        val down = findViewById<ImageButton>(R.id.button_back)
        down.setOnClickListener(this)

        val quit = findViewById<ImageButton>(R.id.button_quit)
        quit.setOnClickListener(this)
    }

    override fun getTabModel(): TabsManager = tabsManager

    override fun showCloseDialog(position: Int) {
        if (position < 0) {
            return
        }
        BrowserDialog.show(this, R.string.dialog_title_close_browser,
                DialogItem(R.string.close_tab) {
                    presenter?.deleteTab(position)
                },
                DialogItem(R.string.close_other_tabs) {
                    presenter?.closeAllOtherTabs()
                },
                DialogItem(title = R.string.close_all_tabs, onClick = this::closeBrowser))
    }

    override fun notifyTabViewRemoved(position: Int) {
        Log.d(TAG, "Notify Tab Removed: " + position)
        tabsView?.tabRemoved(position)
    }

    override fun notifyTabViewAdded() {
        Log.d(TAG, "Notify Tab Added")
        tabsView?.tabAdded()
    }

    override fun notifyTabViewChanged(position: Int) {
        Log.d(TAG, "Notify Tab Changed: " + position)
        tabsView?.tabChanged(position)
    }

    override fun notifyTabViewInitialized() {
        Log.d(TAG, "Notify Tabs Initialized")
        tabsView?.tabsInitialized()
    }

    override fun updateSslState(sslState: SSLState) {
        sslDrawable = when (sslState) {
            is SSLState.None -> null
            is SSLState.Valid -> {
                val bitmap = DrawableUtils.getImageInsetInRoundedSquare(this, R.drawable.ic_secured, R.color.ssl_secured)
                val securedDrawable = BitmapDrawable(resources, bitmap)
                securedDrawable
            }
            is SSLState.Invalid -> {
                val bitmap = DrawableUtils.getImageInsetInRoundedSquare(this, R.drawable.ic_unsecured, R.color.ssl_unsecured)
                val unsecuredDrawable = BitmapDrawable(resources, bitmap)
                unsecuredDrawable
            }
        }

        searchView?.setCompoundDrawablesWithIntrinsicBounds(sslDrawable, null, iconDrawable, null)
    }

    override fun tabChanged(tab: LightningView) {
        presenter?.tabChangeOccurred(tab)
    }

    override fun removeTabView() {

        Log.d(TAG, "Remove the tab view")

        // Set the background color so the color mode color doesn't show through
        content_frame.setBackgroundColor(backgroundColor)

        currentTabView.removeFromParent()

        currentTabView = null

        // Use a delayed handler to make the transition smooth
        // otherwise it will get caught up with the showTab code
        // and cause a janky motion
        Handlers.MAIN.postDelayed(drawer_layout::closeDrawers, 200)

    }

    override fun setTabView(view: View) {
        if (currentTabView === view) {
            return
        }

        Log.d(TAG, "Setting the tab view")

        // Set the background color so the color mode color doesn't show through
        content_frame.setBackgroundColor(backgroundColor)

        view.removeFromParent()
        currentTabView.removeFromParent()

        content_frame.addView(view, 0, MATCH_PARENT)
        if (isFullScreen) {
            view.translationY = toolbar_layout.height + toolbar_layout.translationY
        } else {
            view.translationY = 0f
        }

        view.requestFocus()

        currentTabView = view

        showActionBar()

        // Use a delayed handler to make the transition smooth
        // otherwise it will get caught up with the showTab code
        // and cause a janky motion
        Handlers.MAIN.postDelayed(drawer_layout::closeDrawers, 200)

        // Handlers.MAIN.postDelayed(new Runnable() {
        //     @Override
        //     public void run() {
        // Remove browser frame background to reduce overdraw
        //TODO evaluate performance
        //         content_frame.setBackgroundColor(Color.TRANSPARENT);
        //     }
        // }, 300);
    }

    override fun showBlockedLocalFileDialog(onPositiveClick: Function0<Unit>) =
            AlertDialog.Builder(this).apply {
                setCancelable(true)
                setTitle(R.string.title_warning)
                setMessage(R.string.message_blocked_local)
                setNegativeButton(android.R.string.cancel, null)
                setPositiveButton(R.string.action_open) { _, _ -> onPositiveClick.invoke() }
            }.resizeAndShow()

    override fun showSnackbar(@StringRes resource: Int) = Utils.showSnackbar(this, resource)

    override fun tabCloseClicked(position: Int) {
        presenter?.deleteTab(position)
    }

    override fun tabClicked(position: Int) = showTab(position)

    override fun newTabButtonClicked() {
        presenter?.newTab(null, true)
    }

    override fun newTabButtonLongClicked() {
        val url = preferences.savedUrl
        if (url != null) {
            newTab(url, true)

            Utils.showSnackbar(this, R.string.deleted_tab)
        }
        preferences.savedUrl = null
    }

    override fun bookmarkButtonClicked() {
        val currentTab = tabsManager.currentTab
        val url = currentTab?.url
        val title = currentTab?.title
        if (url == null || title == null) {
            return
        }

        if (!UrlUtils.isSpecialUrl(url)) {
            bookmarkManager.isBookmark(url)
                    .subscribeOn(databaseScheduler)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { boolean ->
                        if (boolean) {
                            deleteBookmark(title, url)
                        } else {
                            addBookmark(title, url)
                        }
                    }
        }
    }

    override fun bookmarkItemClicked(item: HistoryItem) {
        presenter?.loadUrlInCurrentView(item.url)
        // keep any jank from happening when the drawer is closed after the
        // URL starts to load
        Handlers.MAIN.postDelayed({ closeDrawers(null) }, 150)
    }

    override fun handleHistoryChange() = openHistory()

    /**
     * displays the WebView contained in the LightningView Also handles the
     * removal of previous views
     *
     * @param position the position of the tab to display
     */
    // TODO move to presenter
    @Synchronized private fun showTab(position: Int) {
        presenter?.tabChanged(position)
    }

    protected fun handleNewIntent(intent: Intent) {
        presenter?.onNewIntent(intent)
    }

    override fun closeEmptyTab() =// Currently do nothing
            // Possibly closing the current tab might close the browser
            // and mess stuff up
            Unit

    override fun onTrimMemory(level: Int) {
        if (level > TRIM_MEMORY_MODERATE && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Log.d(TAG, "Low Memory, Free Memory")
            presenter?.onAppLowMemory()
        }
    }

    // TODO move to presenter
    @Synchronized private fun newTab(url: String?, show: Boolean): Boolean =
            presenter?.newTab(url, show) != false

    protected fun performExitCleanUp() {
        val currentTab = tabsManager.currentTab
        if (preferences.clearCacheExit && currentTab != null && !isIncognito) {
            WebUtils.clearCache(currentTab.webView)
            Log.d(TAG, "Cache Cleared")
        }
        if (preferences.clearHistoryExitEnabled && !isIncognito) {
            WebUtils.clearHistory(this, historyModel, databaseScheduler)
            Log.d(TAG, "History Cleared")
        }
        if (preferences.clearCookiesExitEnabled && !isIncognito) {
            WebUtils.clearCookies(this)
            Log.d(TAG, "Cookies Cleared")
        }
        if (preferences.clearWebStorageExitEnabled && !isIncognito) {
            WebUtils.clearWebStorage()
            Log.d(TAG, "WebStorage Cleared")
        } else if (isIncognito) {
            WebUtils.clearWebStorage()     // We want to make sure incognito mode is secure
        }
        suggestionsAdapter?.clearCache()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        Log.d(TAG, "onConfigurationChanged")

        if (isFullScreen) {
            showActionBar()
            toolbar_layout.translationY = 0f
            setWebViewTranslation(toolbar_layout.height.toFloat())
        }

        supportInvalidateOptionsMenu()
        initializeToolbarHeight(newConfig)
    }

    private fun initializeToolbarHeight(configuration: Configuration) =// TODO externalize the dimensions
            ui_layout.doOnLayout {
                val toolbarSize = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    // In portrait toolbar should be 56 dp tall
                    Utils.dpToPx(56f)
                } else {
                    // In landscape toolbar should be 48 dp tall
                    Utils.dpToPx(52f)
                }
                toolbar.layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, toolbarSize)
                toolbar.minimumHeight = toolbarSize
                toolbar.doOnLayout { setWebViewTranslation(toolbar_layout.height.toFloat()) }
                toolbar.requestLayout()
            }

    override fun closeBrowser() {
        content_frame.setBackgroundColor(backgroundColor)
        currentTabView.removeFromParent()
        performExitCleanUp()
        val size = tabsManager.size()
        tabsManager.shutdown()
        currentTabView = null
        for (n in 0 until size) {
            tabsView?.tabRemoved(0)
        }
        finish()
    }

    @Synchronized override fun onBackPressed() {
        val currentTab = tabsManager.currentTab
        if (drawer_layout.isDrawerOpen(tabDrawer)) {
            drawer_layout.closeDrawer(tabDrawer)
        } else if (drawer_layout.isDrawerOpen(bookmarkDrawer)) {
            bookmarksView?.navigateBack()
        } else {
            if (currentTab != null) {
                Log.d(TAG, "onBackPressed")
                if (searchView?.hasFocus() == true) {
                    currentTab.requestFocus()
                } else if (currentTab.canGoBack()) {
                    if (!currentTab.isShown) {
                        onHideCustomView()
                    } else {
                        currentTab.goBack()
                    }
                } else {
                    if (customView != null || customViewCallback != null) {
                        onHideCustomView()
                    } else {
                        tabsManager.let { presenter?.deleteTab(it.positionOf(currentTab)) }
                    }
                }
            } else {
                Log.e(TAG, "This shouldn't happen ever")
                super.onBackPressed()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
        tabsManager.pauseAll()

        networkObservable.stopListening(networkListener)

        if (isIncognito && isFinishing) {
            overridePendingTransition(R.anim.fade_in_scale, R.anim.slide_down_out)
        }
    }

    protected fun saveOpenTabs() {
        if (preferences.restoreLostTabsEnabled) {
            tabsManager.saveState()
        }
    }

    override fun onStop() {
        super.onStop()
        proxyUtils.onStop()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")

        Handlers.MAIN.removeCallbacksAndMessages(null)

        presenter?.shutdown()

        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        proxyUtils.onStart(this)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        tabsManager.shutdown()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        if (swapBookmarksAndTabs != preferences.bookmarksAndTabsSwapped) {
            restart()
        }

        suggestionsAdapter?.let {
            it.refreshPreferences()
            it.refreshBookmarks()
        }
        tabsManager.resumeAll(this)
        initializePreferences()

        supportInvalidateOptionsMenu()

        networkObservable.beginListening(networkListener)

        if (isFullScreen) {
            overlayToolbarOnWebView()
        } else {
            putToolbarInRoot()
        }
    }

    /**
     * searches the web for the query fixing any and all problems with the input
     * checks if it is a search, url, etc.
     */
    private fun searchTheWeb(query: String) {
        val currentTab = tabsManager.currentTab
        if (query.isEmpty()) {
            return
        }
        val searchUrl = searchText + UrlUtils.QUERY_PLACE_HOLDER
        if (currentTab != null) {
            currentTab.stopLoading()
            presenter?.loadUrlInCurrentView(UrlUtils.smartUrlFilter(query.trim(), true, searchUrl))
        }
    }

    /**
     * Animates the color of the toolbar from one color to another. Optionally animates
     * the color of the tab background, for use when the tabs are displayed on the top
     * of the screen.
     *
     * @param favicon       the Bitmap to extract the color from
     * @param drawable the optional LinearLayout to color
     */
    override fun changeToolbarBackground(favicon: Bitmap, drawable: Drawable?) {
        val defaultColor = ContextCompat.getColor(this, R.color.primary_color)
        if (currentUiColor == Color.BLACK) {
            currentUiColor = defaultColor
        }
        Palette.from(favicon).generate { palette ->
            // OR with opaque black to remove transparency glitches
            val color = Color.BLACK or palette.getVibrantColor(defaultColor)

            // Lighten up the dark color if it is too dark
            val finalColor = if (!shouldShowTabsInDrawer || Utils.isColorTooDark(color)) {
                Utils.mixTwoColors(defaultColor, color, 0.25f)
            } else {
                color
            }

            val window = window
            if (!shouldShowTabsInDrawer) {
                window.setBackgroundDrawable(ColorDrawable(Color.BLACK))
            }

            val startSearchColor = getSearchBarColor(currentUiColor, defaultColor)
            val finalSearchColor = getSearchBarColor(finalColor, defaultColor)

            val animation = object : Animation() {
                override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                    val animatedColor = DrawableUtils.mixColor(interpolatedTime, currentUiColor, finalColor)
                    if (shouldShowTabsInDrawer) {
                        backgroundDrawable.color = animatedColor
                        Handlers.MAIN.post { window.setBackgroundDrawable(backgroundDrawable) }
                    } else drawable?.setColorFilter(animatedColor, PorterDuff.Mode.SRC_IN)
                    currentUiColor = animatedColor
                    toolbar_layout.setBackgroundColor(animatedColor)
                    searchBackground?.background?.setColorFilter(DrawableUtils.mixColor(interpolatedTime,
                            startSearchColor, finalSearchColor), PorterDuff.Mode.SRC_IN)
                }
            }
            animation.duration = 300
            toolbar_layout.startAnimation(animation)
        }
    }

    private fun getSearchBarColor(requestedColor: Int, defaultColor: Int): Int =
            if (requestedColor == defaultColor) {
                if (isDarkTheme) DrawableUtils.mixColor(0.25f, defaultColor, Color.WHITE) else Color.WHITE
            } else {
                DrawableUtils.mixColor(0.25f, requestedColor, Color.WHITE)
            }

    override fun getUseDarkTheme(): Boolean = isDarkTheme

    @ColorInt
    override fun getUiColor(): Int = currentUiColor

    override fun updateUrl(url: String?, isLoading: Boolean) {
        if (url == null || searchView?.hasFocus() != false) {
            return
        }
        val currentTab = tabsManager.currentTab
        bookmarksView?.handleUpdatedUrl(url)

        val currentTitle = currentTab?.title

        searchView?.setText(searchBoxModel.getDisplayContent(url, currentTitle, isLoading))
    }

    override fun updateTabNumber(number: Int) {
        if (shouldShowTabsInDrawer) {
            if (isIncognito) {
                arrowImageView?.setImageDrawable(ThemeUtils.getThemedDrawable(this, R.drawable.incognito_mode, true))
            } else {
                arrowImageView?.setImageBitmap(DrawableUtils.getRoundedNumberImage(number, Utils.dpToPx(24f),
                        Utils.dpToPx(24f), ThemeUtils.getIconThemeColor(this, isDarkTheme), Utils.dpToPx(2.5f)))
            }
        }
    }

    override fun updateProgress(progress: Int) {
        setIsLoading(progress < 100)
        progress_view.progress = progress
    }

    protected fun addItemToHistory(title: String?, url: String) {
        if (UrlUtils.isSpecialUrl(url)) {
            return
        }

        historyModel.visitHistoryItem(url, title)
                .subscribeOn(databaseScheduler)
                .subscribe()
    }

    /**
     * method to generate search suggestions for the AutoCompleteTextView from
     * previously searched URLs
     */
    private fun initializeSearchSuggestions(getUrl: AutoCompleteTextView) {

        suggestionsAdapter = SuggestionsAdapter(this, isDarkTheme, isIncognito)

        getUrl.threshold = 1
        getUrl.dropDownWidth = -1
        getUrl.dropDownAnchor = R.id.toolbar_layout
        getUrl.onItemClickListener = OnItemClickListener { _, view, _, _ ->
            var url: String? = null
            val urlString = (view.findViewById<View>(R.id.url) as TextView).text
            if (urlString != null) {
                url = urlString.toString()
            }
            if (url == null || url.startsWith(getString(R.string.suggestion))) {
                val searchString = (view.findViewById<View>(R.id.title) as TextView).text
                if (searchString != null) {
                    url = searchString.toString()
                }
            }
            if (url == null) {
                return@OnItemClickListener
            }
            getUrl.setText(url)
            searchTheWeb(url)
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(getUrl.windowToken, 0)
            presenter?.onAutoCompleteItemPressed()
        }

        getUrl.setSelectAllOnFocus(true)
        getUrl.setAdapter<SuggestionsAdapter>(suggestionsAdapter)
    }

    /**
     * function that opens the HTML history page in the browser
     */
    private fun openHistory() {
        HistoryPage().createHistoryPage()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.main())
                .subscribe(object : SingleOnSubscribe<String>() {
                    override fun onItem(item: String?) {
                        tabsManager.let {
                            for (i in 0 until it.size()) {
                                val lightningView = it.getTabAtPosition(i)
                                val url = lightningView?.url
                                if (UrlUtils.isHistoryUrl(url)) {
                                    presenter?.tabChanged(i)
                                    return
                                }
                            }
                            newTab(requireNotNull(item), true)
                        }
                    }
                })
    }

    private fun openDownloads() {
        DownloadsPage().getDownloadsPage()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.main())
                .subscribe(object : SingleOnSubscribe<String>() {
                    override fun onItem(item: String?) {
                        val url = requireNotNull(item)
                        tabsManager.currentTab?.loadUrl(url)
                    }
                })
    }

    /**
     * helper function that opens the bookmark drawer
     */
    private fun openBookmarks() {
        if (drawer_layout.isDrawerOpen(tabDrawer)) {
            drawer_layout.closeDrawers()
        }
        drawer_layout.openDrawer(bookmarkDrawer)
    }

    /**
     * This method closes any open drawer and executes
     * the runnable after the drawers are completely closed.
     *
     * @param runnable an optional runnable to run after
     * the drawers are closed.
     */
    protected fun closeDrawers(runnable: (() -> Unit)?) {
        if (!drawer_layout.isDrawerOpen(left_drawer) && !drawer_layout.isDrawerOpen(right_drawer)) {
            if (runnable != null) {
                runnable()
                return
            }
        }
        drawer_layout.closeDrawers()

        drawer_layout.addDrawerListener(object : DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit

            override fun onDrawerOpened(drawerView: View) = Unit

            override fun onDrawerClosed(drawerView: View) {
                runnable?.invoke()
                drawer_layout.removeDrawerListener(this)
            }

            override fun onDrawerStateChanged(newState: Int) = Unit
        })
    }

    override fun setForwardButtonEnabled(enabled: Boolean) {
        val colorFilter = if (enabled) {
            iconColor
        } else {
            disabledIconColor
        }
        forwardMenuItem?.icon?.setColorFilter(colorFilter, PorterDuff.Mode.SRC_IN)
        forwardMenuItem?.icon = forwardMenuItem?.icon
    }

    override fun setBackButtonEnabled(enabled: Boolean) {
        val colorFilter = if (enabled) {
            iconColor
        } else {
            disabledIconColor
        }
        backMenuItem?.icon?.setColorFilter(colorFilter, PorterDuff.Mode.SRC_IN)
        backMenuItem?.icon = backMenuItem?.icon
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        backMenuItem = menu.findItem(R.id.action_back)
        forwardMenuItem = menu.findItem(R.id.action_forward)
        backMenuItem?.icon?.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
        forwardMenuItem?.icon?.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * opens a file chooser
     * param ValueCallback is the message from the WebView indicating a file chooser
     * should be opened
     */
    override fun openFileChooser(uploadMsg: ValueCallback<Uri>) {
        uploadMessageCallback = uploadMsg
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "*/*"
        startActivityForResult(Intent.createChooser(i, getString(R.string.title_file_chooser)), 1)
    }

    /**
     * used to allow uploading into the browser
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (API < Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == 1) {
                val result = if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
                uploadMessageCallback?.onReceiveValue(result)
                uploadMessageCallback = null

            }
        }

        if (requestCode != 1 || filePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, intent)
            return
        }

        var results: Array<Uri>? = null

        // Check that the response is a good one
        if (resultCode == Activity.RESULT_OK) {
            if (intent == null) {
                // If there is not data, then we may have taken a photo
                if (cameraPhotoPath != null) {
                    results = arrayOf(Uri.parse(cameraPhotoPath))
                }
            } else {
                val dataString = intent.dataString
                if (dataString != null) {
                    results = arrayOf(Uri.parse(dataString))
                }
            }
        }

        filePathCallback?.onReceiveValue(results)
        filePathCallback = null
    }

    override fun showFileChooser(filePathCallback: ValueCallback<Array<Uri>>) {
        this.filePathCallback?.onReceiveValue(null)
        this.filePathCallback = filePathCallback

        var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Create the File where the photo should go
        var photoFile: File? = null
        try {
            photoFile = Utils.createImageFile()
            takePictureIntent?.putExtra("PhotoPath", cameraPhotoPath)
        } catch (ex: IOException) {
            // Error occurred while creating the File
            Log.e(TAG, "Unable to create Image File", ex)
        }

        // Continue only if the File was successfully created
        if (photoFile != null) {
            cameraPhotoPath = "file:" + photoFile.absolutePath
            takePictureIntent?.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
        } else {
            takePictureIntent = null
        }

        val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
        contentSelectionIntent.type = "*/*"

        val intentArray = if (takePictureIntent != null) {
            arrayOf(takePictureIntent)
        } else {
            arrayOf()
        }

        val chooserIntent = Intent(Intent.ACTION_CHOOSER)
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)

        startActivityForResult(chooserIntent, 1)
    }

    @Synchronized override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        originalOrientation = requestedOrientation
        val requestedOrientation = originalOrientation
        onShowCustomView(view, callback, requestedOrientation)
    }

    @Synchronized override fun onShowCustomView(view: View, callback: CustomViewCallback, requestedOrientation: Int) {
        val currentTab = tabsManager.currentTab
        if (customView != null) {
            try {
                callback.onCustomViewHidden()
            } catch (e: Exception) {
                Log.e(TAG, "Error hiding custom view", e)
            }

            return
        }

        try {
            view.keepScreenOn = true
        } catch (e: SecurityException) {
            Log.e(TAG, "WebView is not allowed to keep the screen on")
        }

        originalOrientation = getRequestedOrientation()
        customViewCallback = callback
        customView = view

        setRequestedOrientation(requestedOrientation)
        val decorView = window.decorView as FrameLayout

        fullscreenContainerView = FrameLayout(this)
        fullscreenContainerView?.setBackgroundColor(ContextCompat.getColor(this, R.color.black))
        if (view is FrameLayout) {
            val child = view.focusedChild
            if (child is VideoView) {
                videoView = child
                child.setOnErrorListener(VideoCompletionListener())
                child.setOnCompletionListener(VideoCompletionListener())
            }
        } else if (view is VideoView) {
            videoView = view
            view.setOnErrorListener(VideoCompletionListener())
            view.setOnCompletionListener(VideoCompletionListener())
        }
        decorView.addView(fullscreenContainerView, COVER_SCREEN_PARAMS)
        fullscreenContainerView?.addView(customView, COVER_SCREEN_PARAMS)
        decorView.requestLayout()
        setFullscreen(true, true)
        currentTab?.setVisibility(View.INVISIBLE)
    }

    override fun closeBookmarksDrawer() = drawer_layout.closeDrawer(bookmarkDrawer)

    override fun onHideCustomView() {
        val currentTab = tabsManager.currentTab
        if (customView == null || customViewCallback == null || currentTab == null) {
            if (customViewCallback != null) {
                try {
                    customViewCallback?.onCustomViewHidden()
                } catch (e: Exception) {
                    Log.e(TAG, "Error hiding custom view", e)
                }

                customViewCallback = null
            }
            return
        }
        Log.d(TAG, "onHideCustomView")
        currentTab.setVisibility(View.VISIBLE)
        try {
            customView?.keepScreenOn = false
        } catch (e: SecurityException) {
            Log.e(TAG, "WebView is not allowed to keep the screen on")
        }

        setFullscreen(preferences.hideStatusBarEnabled, false)
        if (fullscreenContainerView != null) {
            val parent = fullscreenContainerView?.parent as ViewGroup
            parent.removeView(fullscreenContainerView)
            fullscreenContainerView?.removeAllViews()
        }

        fullscreenContainerView = null
        customView = null
        if (videoView != null) {
            Log.d(TAG, "VideoView is being stopped")
            videoView?.stopPlayback()
            videoView?.setOnErrorListener(null)
            videoView?.setOnCompletionListener(null)
            videoView = null
        }
        if (customViewCallback != null) {
            try {
                customViewCallback?.onCustomViewHidden()
            } catch (e: Exception) {
                Log.e(TAG, "Error hiding custom view", e)
            }

        }
        customViewCallback = null
        requestedOrientation = originalOrientation
    }

    private inner class VideoCompletionListener : MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

        override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean = false

        override fun onCompletion(mp: MediaPlayer) = onHideCustomView()

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Log.d(TAG, "onWindowFocusChanged")
        if (hasFocus) {
            setFullscreen(hideStatusBar, isImmersiveMode)
        }
    }

    override fun onBackButtonPressed() {
        val currentTab = tabsManager.currentTab
        if (currentTab != null) {
            if (currentTab.canGoBack()) {
                currentTab.goBack()
                closeDrawers(null)
            } else {
                tabsManager.let { presenter?.deleteTab(it.positionOf(currentTab)) }
            }
        }
    }

    override fun onForwardButtonPressed() {
        val currentTab = tabsManager.currentTab
        if (currentTab != null) {
            if (currentTab.canGoForward()) {
                currentTab.goForward()
                closeDrawers(null)
            }
        }
    }

    override fun onHomeButtonPressed() {
        val currentTab = tabsManager.currentTab
        if (currentTab != null) {
            currentTab.loadHomepage()
            closeDrawers(null)
        }
    }

    /**
     * This method sets whether or not the activity will display
     * in full-screen mode (i.e. the ActionBar will be hidden) and
     * whether or not immersive mode should be set. This is used to
     * set both parameters correctly as during a full-screen video,
     * both need to be set, but other-wise we leave it up to user
     * preference.
     *
     * @param enabled   true to enable full-screen, false otherwise
     * @param immersive true to enable immersive mode, false otherwise
     */
    private fun setFullscreen(enabled: Boolean, immersive: Boolean) {
        hideStatusBar = enabled
        isImmersiveMode = immersive
        val window = window
        val decor = window.decorView
        if (enabled) {
            if (immersive) {
                decor.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            } else {
                decor.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            decor.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    /**
     * This method handles the JavaScript callback to create a new tab.
     * Basically this handles the event that JavaScript needs to create
     * a popup.
     *
     * @param resultMsg the transport message used to send the URL to
     * the newly created WebView.
     */
    @Synchronized override fun onCreateWindow(resultMsg: Message) {
        if (newTab("", true)) {
            tabsManager.let {
                val newTab = it.getTabAtPosition(it.size() - 1)
                if (newTab != null) {
                    val webView = newTab.webView
                    if (webView != null) {
                        val transport = resultMsg.obj as WebView.WebViewTransport
                        transport.webView = webView
                        resultMsg.sendToTarget()
                    }
                }
            }
        }
    }

    /**
     * Closes the specified [LightningView]. This implements
     * the JavaScript callback that asks the tab to close itself and
     * is especially helpful when a page creates a redirect and does
     * not need the tab to stay open any longer.
     *
     * @param view the LightningView to close, delete it.
     */
    override fun onCloseWindow(view: LightningView) {
        tabsManager.let { presenter?.deleteTab(it.positionOf(view)) }
    }

    /**
     * Hide the ActionBar using an animation if we are in full-screen
     * mode. This method also re-parents the ActionBar if its parent is
     * incorrect so that the animation can happen correctly.
     */
    override fun hideActionBar() {
        if (isFullScreen) {
            if (toolbar_layout == null || content_frame == null)
                return

            val height = toolbar_layout.height
            if (toolbar_layout.translationY > -0.01f) {
                val hideAnimation = object : Animation() {
                    override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                        val trans = interpolatedTime * height
                        toolbar_layout.translationY = -trans
                        setWebViewTranslation(height - trans)
                    }
                }
                hideAnimation.duration = 250
                hideAnimation.interpolator = BezierDecelerateInterpolator()
                content_frame.startAnimation(hideAnimation)
            }
        }
    }

    /**
     * Display the ActionBar using an animation if we are in full-screen
     * mode. This method also re-parents the ActionBar if its parent is
     * incorrect so that the animation can happen correctly.
     */
    override fun showActionBar() {
        if (isFullScreen) {
            Log.d(TAG, "showActionBar")
            if (toolbar_layout == null)
                return

            var height = toolbar_layout.height
            if (height == 0) {
                toolbar_layout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                height = toolbar_layout.measuredHeight
            }

            val totalHeight = height
            if (toolbar_layout.translationY < -(height - 0.01f)) {
                val show = object : Animation() {
                    override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                        val trans = interpolatedTime * totalHeight
                        toolbar_layout.translationY = trans - totalHeight
                        setWebViewTranslation(trans)
                    }
                }
                show.duration = 250
                show.interpolator = BezierDecelerateInterpolator()
                content_frame.startAnimation(show)
            }
        }
    }

    override fun handleBookmarksChange() {
        val currentTab = tabsManager.currentTab
        if (currentTab != null && UrlUtils.isBookmarkUrl(currentTab.url)) {
            currentTab.loadBookmarkpage()
        }
        if (currentTab != null) {
            bookmarksView?.handleUpdatedUrl(currentTab.url)
        }
    }

    override fun handleDownloadDeleted() {
        val currentTab = tabsManager.currentTab
        if (currentTab != null && UrlUtils.isDownloadsUrl(currentTab.url)) {
            currentTab.loadDownloadspage()
        }
        if (currentTab != null) {
            bookmarksView?.handleUpdatedUrl(currentTab.url)
        }
    }

    override fun handleBookmarkDeleted(item: HistoryItem) {
        bookmarksView?.handleBookmarkDeleted(item)
        handleBookmarksChange()
    }

    override fun handleNewTab(newTabType: LightningDialogBuilder.NewTab, url: String) {
        drawer_layout.closeDrawers()
        when (newTabType) {
            LightningDialogBuilder.NewTab.FOREGROUND -> newTab(url, true)
            LightningDialogBuilder.NewTab.BACKGROUND -> newTab(url, false)
            LightningDialogBuilder.NewTab.INCOGNITO -> {
                val intent = IncognitoActivity.createIntent(this).apply { data = Uri.parse(url) }
                startActivity(intent)
                overridePendingTransition(R.anim.slide_up_in, R.anim.fade_out_scale)
            }
        }
    }

    /**
     * This method lets the search bar know that the page is currently loading
     * and that it should display the stop icon to indicate to the user that
     * pressing it stops the page from loading
     */
    private fun setIsLoading(isLoading: Boolean) {
        if (searchView?.hasFocus() == false) {
            iconDrawable = if (isLoading) deleteIconDrawable else refreshIconDrawable
            searchView?.setCompoundDrawablesWithIntrinsicBounds(sslDrawable, null, iconDrawable, null)
        }
    }

    /**
     * handle presses on the refresh icon in the search bar, if the page is
     * loading, stop the page, if it is done loading refresh the page.
     * See setIsFinishedLoading and setIsLoading for displaying the correct icon
     */
    private fun refreshOrStop() {
        val currentTab = tabsManager.currentTab
        if (currentTab != null) {
            if (currentTab.progress < 100) {
                currentTab.stopLoading()
            } else {
                currentTab.reload()
            }
        }
    }

    /**
     * Handle the click event for the views that are using
     * this class as a click listener. This method should
     * distinguish between the various views using their IDs.
     *
     * @param v the view that the user has clicked
     */
    override fun onClick(v: View) {
        val currentTab = tabsManager.currentTab ?: return
        when (v.id) {
            R.id.arrow_button -> when {
                searchView?.hasFocus() == true -> currentTab.requestFocus()
                shouldShowTabsInDrawer -> drawer_layout.openDrawer(tabDrawer)
                else -> currentTab.loadHomepage()
            }
            R.id.button_next -> currentTab.findNext()
            R.id.button_back -> currentTab.findPrevious()
            R.id.button_quit -> {
                currentTab.clearFindMatches()
                search_bar.visibility = View.GONE
            }
            R.id.action_reading -> {
                val read = Intent(this, ReadingActivity::class.java)
                read.putExtra(LOAD_READING_URL, currentTab.url)
                startActivity(read)
            }
            R.id.action_toggle_desktop -> {
                currentTab.toggleDesktopUA(this)
                currentTab.reload()
                closeDrawers(null)
            }
        }
    }

    /**
     * Handle the callback that permissions requested have been granted or not.
     * This method should act upon the results of the permissions request.
     *
     * @param requestCode  the request code sent when initially making the request
     * @param permissions  the array of the permissions that was requested
     * @param grantResults the results of the permissions requests that provides
     * information on whether the request was granted or not
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {

        private const val TAG = "BrowserActivity"

        private const val INTENT_PANIC_TRIGGER = "info.guardianproject.panic.action.TRIGGER"

        private const val TAG_BOOKMARK_FRAGMENT = "TAG_BOOKMARK_FRAGMENT"
        private const val TAG_TABS_FRAGMENT = "TAG_TABS_FRAGMENT"

        // Constant
        private val API = android.os.Build.VERSION.SDK_INT
        private val MATCH_PARENT = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        private val COVER_SCREEN_PARAMS = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        /**
         * Determines if an intent is originating
         * from a panic trigger.
         *
         * @param intent the intent to check.
         * @return true if the panic trigger sent
         * the intent, false otherwise.
         */
        fun isPanicTrigger(intent: Intent?): Boolean =
                intent != null && INTENT_PANIC_TRIGGER == intent.action

    }

}
