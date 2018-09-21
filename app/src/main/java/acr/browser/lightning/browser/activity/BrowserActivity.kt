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
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.HistoryEntry
import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.database.history.HistoryRepository
import acr.browser.lightning.di.DatabaseScheduler
import acr.browser.lightning.di.MainScheduler
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.dialog.DialogItem
import acr.browser.lightning.dialog.LightningDialogBuilder
import acr.browser.lightning.extensions.doOnLayout
import acr.browser.lightning.extensions.removeFromParent
import acr.browser.lightning.extensions.resizeAndShow
import acr.browser.lightning.extensions.snackbar
import acr.browser.lightning.html.download.DownloadsPage
import acr.browser.lightning.html.history.HistoryPage
import acr.browser.lightning.interpolator.BezierDecelerateInterpolator
import acr.browser.lightning.network.NetworkConnectivityModel
import acr.browser.lightning.notifications.IncognitoNotification
import acr.browser.lightning.reading.activity.ReadingActivity
import acr.browser.lightning.search.SearchEngineProvider
import acr.browser.lightning.search.SuggestionsAdapter
import acr.browser.lightning.settings.activity.SettingsActivity
import acr.browser.lightning.ssl.SSLState
import acr.browser.lightning.utils.*
import acr.browser.lightning.view.*
import acr.browser.lightning.view.SearchView
import android.app.Activity
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
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
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v4.widget.DrawerLayout.DrawerListener
import android.support.v7.app.AlertDialog
import android.support.v7.graphics.Palette
import android.text.Editable
import android.text.TextWatcher
import android.text.style.CharacterStyle
import android.text.style.ParagraphStyle
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
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.TextView.OnEditorActionListener
import androidx.core.net.toUri
import androidx.core.widget.toast
import butterknife.ButterKnife
import com.anthonycr.grant.PermissionsManager
import io.reactivex.Completable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.browser_content.*
import kotlinx.android.synthetic.main.search_interface.*
import kotlinx.android.synthetic.main.toolbar.*
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
    @Inject internal lateinit var networkConnectivityModel: NetworkConnectivityModel
    @Inject internal lateinit var inputMethodManager: InputMethodManager
    @Inject internal lateinit var clipboardManager: ClipboardManager
    @Inject internal lateinit var notificationManager: NotificationManager
    @Inject @field:DatabaseScheduler internal lateinit var databaseScheduler: Scheduler
    @Inject @field:MainScheduler internal lateinit var mainScheduler: Scheduler

    private val tabsManager: TabsManager = TabsManager()

    // Subscriptions
    private var networkDisposable: Disposable? = null

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

    // Menu
    private var backMenuItem: MenuItem? = null
    private var forwardMenuItem: MenuItem? = null

    private val longPressBackRunnable = Runnable {
        showCloseDialog(tabsManager.positionOf(tabsManager.currentTab))
    }

    // Proxy
    @Inject internal lateinit var proxyUtils: ProxyUtils

    /**
     * Determines if the current browser instance is in incognito mode or not.
     */
    protected abstract fun isIncognito(): Boolean

    /**
     * Choose the behavior when the controller closes the view.
     */
    abstract override fun closeActivity()

    /**
     * Choose what to do when the browser visits a website.
     *
     * @param title the title of the site visited.
     * @param url the url of the site visited.
     */
    abstract override fun updateHistory(title: String?, url: String)

    /**
     * An observable which asynchronously updates the user's cookie preferences.
     */
    protected abstract fun updateCookiePreference(): Completable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BrowserApp.appComponent.inject(this)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)

        val incognitoNotification = IncognitoNotification(this, notificationManager)
        tabsManager.addTabNumberChangedListener {
            if (isIncognito()) {
                if (it == 0) {
                    incognitoNotification.hide()
                } else {
                    incognitoNotification.show(it)
                }
            }
        }

        presenter = BrowserPresenter(this, isIncognito())

        initialize(savedInstanceState)
    }

    private fun initialize(savedInstanceState: Bundle?) {
        initializeToolbarHeight(resources.configuration)
        setSupportActionBar(toolbar)
        val actionBar = requireNotNull(supportActionBar)

        //TODO make sure dark theme flag gets set correctly
        isDarkTheme = userPreferences.useTheme != 0 || isIncognito()
        iconColor = ThemeUtils.getIconThemeColor(this, isDarkTheme)
        disabledIconColor = if (isDarkTheme) {
            ContextCompat.getColor(this, R.color.icon_dark_theme_disabled)
        } else {
            ContextCompat.getColor(this, R.color.icon_light_theme_disabled)
        }
        shouldShowTabsInDrawer = userPreferences.showTabsInDrawer
        swapBookmarksAndTabs = userPreferences.bookmarksAndTabsSwapped

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

        tabsView = tabsFragment ?: TabsFragment.createTabsFragment(isIncognito(), shouldShowTabsInDrawer)

        if (bookmarksFragment != null) {
            fragmentManager.beginTransaction().remove(bookmarksFragment).commit()
        }

        bookmarksView = bookmarksFragment ?: BookmarksFragment.createFragment(isIncognito())

        fragmentManager.executePendingTransactions()

        fragmentManager
            .beginTransaction()
            .replace(getTabsFragmentViewId(), tabsView as Fragment, TAG_TABS_FRAGMENT)
            .replace(getBookmarksFragmentViewId(), bookmarksView as Fragment, TAG_BOOKMARK_FRAGMENT)
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
        customView.layoutParams = customView.layoutParams.apply {
            width = LayoutParams.MATCH_PARENT
            height = LayoutParams.MATCH_PARENT
        }

        arrowImageView = customView.findViewById<ImageView>(R.id.arrow).also {
            if (shouldShowTabsInDrawer) {
                if (it.width <= 0) {
                    it.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                }
                updateTabNumber(0)

                // Post drawer locking in case the activity is being recreated
                Handlers.MAIN.post { drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, getTabDrawer()) }
            } else {

                // Post drawer locking in case the activity is being recreated
                Handlers.MAIN.post { drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, getTabDrawer()) }
                it.setImageResource(R.drawable.ic_action_home)
                it.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
            }
        }

        // Post drawer locking in case the activity is being recreated
        Handlers.MAIN.post { drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, getBookmarkDrawer()) }

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
            onPreFocusListener = searchListener
            addTextChangedListener(searchListener)

            initializeSearchSuggestions(this)
        }

        searchView?.onRightDrawableClickListener = {
            if (it.hasFocus()) {
                it.setText("")
            } else {
                refreshOrStop()
            }
        }

        searchBackground = customView.findViewById<View>(R.id.search_container).apply {
            // initialize search background color
            background.setColorFilter(getSearchBarColor(primaryColor, primaryColor), PorterDuff.Mode.SRC_IN)
        }

        drawer_layout.setDrawerShadow(R.drawable.drawer_right_shadow, GravityCompat.END)
        drawer_layout.setDrawerShadow(R.drawable.drawer_left_shadow, GravityCompat.START)

        var intent: Intent? = if (savedInstanceState == null) {
            intent
        } else {
            null
        }

        val launchedFromHistory = intent != null && intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0

        if (intent?.action == INTENT_PANIC_TRIGGER) {
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

    private fun getBookmarksFragmentViewId(): Int = if (swapBookmarksAndTabs) {
        R.id.left_drawer
    } else {
        R.id.right_drawer
    }

    private fun getTabsFragmentViewId(): Int = if (shouldShowTabsInDrawer) {
        if (swapBookmarksAndTabs) {
            R.id.right_drawer
        } else {
            R.id.left_drawer
        }
    } else {
        R.id.tabs_toolbar_container
    }

    private fun getBookmarkDrawer(): View = if (swapBookmarksAndTabs) {
        left_drawer
    } else {
        right_drawer
    }

    private fun getTabDrawer(): View = if (swapBookmarksAndTabs) {
        right_drawer
    } else {
        left_drawer
    }

    protected fun panicClean() {
        Log.d(TAG, "Closing browser")
        tabsManager.newTab(this, NoOpInitializer(), false)
        tabsManager.switchToTab(0)
        tabsManager.clearSavedState()

        HistoryPage.deleteHistoryPage(application).subscribe()
        closeBrowser()
        // System exit needed in the case of receiving
        // the panic intent since finish() isn't completely
        // closing the browser
        System.exit(1)
    }

    private inner class SearchListenerClass : OnKeyListener,
        OnEditorActionListener,
        OnFocusChangeListener,
        SearchView.PreFocusListener,
        TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit

        override fun afterTextChanged(e: Editable) {
            e.getSpans(0, e.length, CharacterStyle::class.java).forEach(e::removeSpan)
            e.getSpans(0, e.length, ParagraphStyle::class.java).forEach(e::removeSpan)
        }

        override fun onKey(view: View, keyCode: Int, keyEvent: KeyEvent): Boolean {

            when (keyCode) {
                KeyEvent.KEYCODE_ENTER -> {
                    searchView?.let {
                        inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
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
            if (actionId == EditorInfo.IME_ACTION_GO
                || actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_ACTION_NEXT
                || actionId == EditorInfo.IME_ACTION_SEND
                || actionId == EditorInfo.IME_ACTION_SEARCH
                || arg2?.action == KeyEvent.KEYCODE_ENTER) {
                searchView?.let {
                    inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
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
                searchView?.let {
                    inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
                }
            }
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
            val tabsDrawer = getTabDrawer()
            val bookmarksDrawer = getBookmarkDrawer()

            if (v === tabsDrawer) {
                drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, bookmarksDrawer)
            } else if (shouldShowTabsInDrawer) {
                drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, tabsDrawer)
            }
        }

        override fun onDrawerOpened(v: View) {
            val tabsDrawer = getTabDrawer()
            val bookmarksDrawer = getBookmarkDrawer()

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
            val params = left_drawer.layoutParams as DrawerLayout.LayoutParams
            params.width = maxWidth
            left_drawer.layoutParams = params
            left_drawer.requestLayout()
            val paramsRight = right_drawer.layoutParams as DrawerLayout.LayoutParams
            paramsRight.width = maxWidth
            right_drawer.layoutParams = paramsRight
            right_drawer.requestLayout()
        } else {
            val params = left_drawer.layoutParams as DrawerLayout.LayoutParams
            params.width = width
            left_drawer.layoutParams = params
            left_drawer.requestLayout()
            val paramsRight = right_drawer.layoutParams as DrawerLayout.LayoutParams
            paramsRight.width = width
            right_drawer.layoutParams = paramsRight
            right_drawer.requestLayout()
        }
    }

    private fun initializePreferences() {
        val currentView = tabsManager.currentTab
        isFullScreen = userPreferences.fullScreenEnabled
        val colorMode = userPreferences.colorModeEnabled && !isDarkTheme

        webPageBitmap?.let { webBitmap ->
            if (!isIncognito() && !colorMode && !isDarkTheme) {
                changeToolbarBackground(webBitmap, null)
            } else if (!isIncognito() && currentView != null && !isDarkTheme) {
                changeToolbarBackground(currentView.favicon, null)
            } else if (!isIncognito() && !isDarkTheme) {
                changeToolbarBackground(webBitmap, null)
            }
        }

        val manager = supportFragmentManager
        val tabsFragment = manager.findFragmentByTag(TAG_TABS_FRAGMENT) as? TabsFragment
        tabsFragment?.reinitializePreferences()
        val bookmarksFragment = manager.findFragmentByTag(TAG_BOOKMARK_FRAGMENT)as? BookmarksFragment
        bookmarksFragment?.reinitializePreferences()

        // TODO layout transition causing memory leak
        //        content_frame.setLayoutTransition(new LayoutTransition());

        setFullscreen(userPreferences.hideStatusBarEnabled, false)

        val currentSearchEngine = searchEngineProvider.provideSearchEngine()
        searchText = currentSearchEngine.queryUrl

        updateCookiePreference().subscribeOn(Schedulers.computation()).subscribe()
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
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            keyDownStartTime = System.currentTimeMillis()
            Handlers.MAIN.postDelayed(longPressBackRunnable, ViewConfiguration.getLongPressTimeout().toLong())
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
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
                        presenter?.newTab(
                            HomePageInitializer(userPreferences, this, databaseScheduler, mainScheduler),
                            true
                        )
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
                if (drawer_layout.isDrawerOpen(getBookmarkDrawer())) {
                    drawer_layout.closeDrawer(getBookmarkDrawer())
                }
                return true
            }
            R.id.action_back -> {
                if (currentView?.canGoBack() == true) {
                    currentView.goBack()
                }
                return true
            }
            R.id.action_forward -> {
                if (currentView?.canGoForward() == true) {
                    currentView.goForward()
                }
                return true
            }
            R.id.action_add_to_homescreen -> {
                if (currentView != null) {
                    Utils.createShortcut(this, HistoryEntry(currentView.url, currentView.title), currentView.favicon)
                }
                return true
            }
            R.id.action_new_tab -> {
                presenter?.newTab(HomePageInitializer(userPreferences, this, databaseScheduler, mainScheduler), true)
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
                    clipboardManager.primaryClip = ClipData.newPlainText("label", currentUrl)
                    snackbar(R.string.message_link_copied)
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
        bookmarkManager.addBookmarkIfNotExists(Bookmark.Entry(url, title, 0, null))
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribe { boolean ->
                if (boolean) {
                    suggestionsAdapter?.refreshBookmarks()
                    bookmarksView?.handleUpdatedUrl(url)
                    toast(R.string.message_bookmark_added)
                }
            }
    }

    private fun deleteBookmark(title: String, url: String) {
        bookmarkManager.deleteBookmark(Bookmark.Entry(url, title, 0, null))
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribe { boolean ->
                if (boolean) {
                    suggestionsAdapter?.refreshBookmarks()
                    bookmarksView?.handleUpdatedUrl(url)
                }
            }
    }

    private fun putToolbarInRoot() {
        if (toolbar_layout.parent != ui_layout) {
            (toolbar_layout.parent as ViewGroup?)?.removeView(toolbar_layout)

            ui_layout.addView(toolbar_layout, 0)
            ui_layout.requestLayout()
        }
        setWebViewTranslation(0f)
    }

    private fun overlayToolbarOnWebView() {
        if (toolbar_layout.parent != content_frame) {
            (toolbar_layout.parent as ViewGroup?)?.removeView(toolbar_layout)

            content_frame.addView(toolbar_layout)
            content_frame.requestLayout()
        }
        setWebViewTranslation(toolbar_layout.height.toFloat())
    }

    private fun setWebViewTranslation(translation: Float) =
        if (isFullScreen) {
            currentTabView?.translationY = translation
        } else {
            currentTabView?.translationY = 0f
        }

    /**
     * method that shows a dialog asking what string the user wishes to search
     * for. It highlights the text entered.
     */
    private fun findInPage() = BrowserDialog.showEditText(
        this,
        R.string.action_find,
        R.string.search_hint,
        R.string.search_hint
    ) { text ->
        if (text.isNotEmpty()) {
            presenter?.findInPage(text)
            showFindInPageControls(text)
        }
    }

    private fun showFindInPageControls(text: String) {
        search_bar.visibility = View.VISIBLE

        findViewById<TextView>(R.id.search_query).apply { this.text = "'$text'" }
        findViewById<ImageButton>(R.id.button_next).apply { setOnClickListener(this@BrowserActivity) }
        findViewById<ImageButton>(R.id.button_back).apply { setOnClickListener(this@BrowserActivity) }
        findViewById<ImageButton>(R.id.button_quit).apply { setOnClickListener(this@BrowserActivity) }
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
        Log.d(TAG, "Notify Tab Removed: $position")
        tabsView?.tabRemoved(position)
    }

    override fun notifyTabViewAdded() {
        Log.d(TAG, "Notify Tab Added")
        tabsView?.tabAdded()
    }

    override fun notifyTabViewChanged(position: Int) {
        Log.d(TAG, "Notify Tab Changed: $position")
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
        if (currentTabView == view) {
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

    override fun showSnackbar(@StringRes resource: Int) = snackbar(resource)

    override fun tabCloseClicked(position: Int) {
        presenter?.deleteTab(position)
    }

    override fun tabClicked(position: Int) = showTab(position)

    override fun newTabButtonClicked() {
        presenter?.newTab(
            HomePageInitializer(userPreferences, this, databaseScheduler, mainScheduler),
            true
        )
    }

    override fun newTabButtonLongClicked() {
        val savedUrl = userPreferences.savedUrl

        if (savedUrl != "") {
            presenter?.newTab(UrlInitializer(savedUrl), true)

            snackbar(R.string.deleted_tab)
        }

        userPreferences.savedUrl = ""
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
                .observeOn(mainScheduler)
                .subscribe { boolean ->
                    if (boolean) {
                        deleteBookmark(title, url)
                    } else {
                        addBookmark(title, url)
                    }
                }
        }
    }

    override fun bookmarkItemClicked(entry: Bookmark.Entry) {
        presenter?.loadUrlInCurrentView(entry.url)
        // keep any jank from happening when the drawer is closed after the URL starts to load
        Handlers.MAIN.postDelayed({ closeDrawers(null) }, 150)
    }

    override fun handleHistoryChange() {
        HistoryPage().createHistoryPage()
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribeBy(onSuccess = { tabsManager.currentTab?.reload() })
    }

    /**
     * displays the WebView contained in the LightningView Also handles the
     * removal of previous views
     *
     * @param position the position of the tab to display
     */
    // TODO move to presenter
    private fun showTab(position: Int) {
        presenter?.tabChanged(position)
    }

    protected fun handleNewIntent(intent: Intent) {
        presenter?.onNewIntent(intent)
    }

    protected fun performExitCleanUp() {
        val currentTab = tabsManager.currentTab
        if (userPreferences.clearCacheExit && currentTab != null && !isIncognito()) {
            WebUtils.clearCache(currentTab.webView)
            Log.d(TAG, "Cache Cleared")
        }
        if (userPreferences.clearHistoryExitEnabled && !isIncognito()) {
            WebUtils.clearHistory(this, historyModel, databaseScheduler)
            Log.d(TAG, "History Cleared")
        }
        if (userPreferences.clearCookiesExitEnabled && !isIncognito()) {
            WebUtils.clearCookies(this)
            Log.d(TAG, "Cookies Cleared")
        }
        if (userPreferences.clearWebStorageExitEnabled && !isIncognito()) {
            WebUtils.clearWebStorage()
            Log.d(TAG, "WebStorage Cleared")
        } else if (isIncognito()) {
            WebUtils.clearWebStorage()     // We want to make sure incognito mode is secure
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        Log.d(TAG, "onConfigurationChanged")

        if (isFullScreen) {
            showActionBar()
            toolbar_layout.translationY = 0f
            setWebViewTranslation(toolbar_layout.height.toFloat())
        }

        invalidateOptionsMenu()
        initializeToolbarHeight(newConfig)
    }

    private fun initializeToolbarHeight(configuration: Configuration) =
        ui_layout.doOnLayout {
            // TODO externalize the dimensions
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

    override fun onBackPressed() {
        val currentTab = tabsManager.currentTab
        if (drawer_layout.isDrawerOpen(getTabDrawer())) {
            drawer_layout.closeDrawer(getTabDrawer())
        } else if (drawer_layout.isDrawerOpen(getBookmarkDrawer())) {
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
                        presenter?.deleteTab(tabsManager.positionOf(currentTab))
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

        networkDisposable?.dispose()

        if (isIncognito() && isFinishing) {
            overridePendingTransition(R.anim.fade_in_scale, R.anim.slide_down_out)
        }
    }

    protected fun saveOpenTabs() {
        if (userPreferences.restoreLostTabsEnabled) {
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
        if (swapBookmarksAndTabs != userPreferences.bookmarksAndTabsSwapped) {
            restart()
        }

        suggestionsAdapter?.let {
            it.refreshPreferences()
            it.refreshBookmarks()
        }
        tabsManager.resumeAll(this)
        initializePreferences()

        networkDisposable = networkConnectivityModel
            .connectivity()
            .subscribeOn(mainScheduler)
            .subscribe { connected ->
                Log.d(TAG, "Network connected: $connected")
                tabsManager.notifyConnectionStatus(connected)
            }

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
        val searchUrl = "$searchText${UrlUtils.QUERY_PLACE_HOLDER}"
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
     * @param favicon the Bitmap to extract the color from
     * @param tabBackground the optional LinearLayout to color
     */
    override fun changeToolbarBackground(favicon: Bitmap, tabBackground: Drawable?) {
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
                    } else {
                        tabBackground?.setColorFilter(animatedColor, PorterDuff.Mode.SRC_IN)
                    }
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
            if (isIncognito()) {
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

        historyModel.visitHistoryEntry(url, title)
            .subscribeOn(databaseScheduler)
            .subscribe()
    }

    /**
     * method to generate search suggestions for the AutoCompleteTextView from
     * previously searched URLs
     */
    private fun initializeSearchSuggestions(getUrl: AutoCompleteTextView) {

        suggestionsAdapter = SuggestionsAdapter(this, isDarkTheme, isIncognito())

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
            inputMethodManager.hideSoftInputFromWindow(getUrl.windowToken, 0)
            presenter?.onAutoCompleteItemPressed()
        }

        getUrl.setSelectAllOnFocus(true)
        getUrl.setAdapter<SuggestionsAdapter>(suggestionsAdapter)
    }

    /**
     * function that opens the HTML history page in the browser
     */
    private fun openHistory() {
        presenter?.newTab(
            AsyncUrlInitializer(HistoryPage().createHistoryPage(), databaseScheduler, mainScheduler),
            true
        )
    }

    private fun openDownloads() {
        presenter?.newTab(
            AsyncUrlInitializer(DownloadsPage().getDownloadsPage(), databaseScheduler, mainScheduler),
            true
        )
    }

    /**
     * helper function that opens the bookmark drawer
     */
    private fun openBookmarks() {
        if (drawer_layout.isDrawerOpen(getTabDrawer())) {
            drawer_layout.closeDrawers()
        }
        drawer_layout.openDrawer(getBookmarkDrawer())
    }

    /**
     * This method closes any open drawer and executes the runnable after the drawers are closed.
     *
     * @param runnable an optional runnable to run after the drawers are closed.
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
        backMenuItem = menu.findItem(R.id.action_back)?.apply {
            icon?.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
        }
        forwardMenuItem = menu.findItem(R.id.action_forward)?.apply {
            icon?.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
        }
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * opens a file chooser
     * param ValueCallback is the message from the WebView indicating a file chooser
     * should be opened
     */
    override fun openFileChooser(uploadMsg: ValueCallback<Uri>) {
        uploadMessageCallback = uploadMsg
        startActivityForResult(Intent.createChooser(Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }, getString(R.string.title_file_chooser)), FILE_CHOOSER_REQUEST_CODE)
    }

    /**
     * used to allow uploading into the browser
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == 1) {
                val result = if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
                uploadMessageCallback?.onReceiveValue(result)
                uploadMessageCallback = null

            }
        }

        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            val results: Array<Uri>? = if (resultCode == Activity.RESULT_OK) {
                if (intent == null) {
                    // If there is not data, then we may have taken a photo
                    cameraPhotoPath?.let { arrayOf(it.toUri()) }
                } else {
                    intent.dataString?.let { arrayOf(it.toUri()) }
                }
            } else {
                null
            }

            filePathCallback?.onReceiveValue(results)
            filePathCallback = null
        } else {
            super.onActivityResult(requestCode, resultCode, intent)
        }
    }

    override fun showFileChooser(filePathCallback: ValueCallback<Array<Uri>>) {
        this.filePathCallback?.onReceiveValue(null)
        this.filePathCallback = filePathCallback

        // Create the File where the photo should go
        val intentArray: Array<Intent> = try {
            arrayOf(Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra("PhotoPath", cameraPhotoPath)
                putExtra(
                    MediaStore.EXTRA_OUTPUT,
                    Uri.fromFile(Utils.createImageFile().also { file ->
                        cameraPhotoPath = "file:${file.absolutePath}"
                    })
                )
            })
        } catch (ex: IOException) {
            // Error occurred while creating the File
            Log.e(TAG, "Unable to create Image File", ex)
            emptyArray()
        }

        startActivityForResult(Intent(Intent.ACTION_CHOOSER).apply {
            putExtra(Intent.EXTRA_INTENT, Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            })
            putExtra(Intent.EXTRA_TITLE, "Image Chooser")
            putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
        }, FILE_CHOOSER_REQUEST_CODE)
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        originalOrientation = requestedOrientation
        val requestedOrientation = originalOrientation
        onShowCustomView(view, callback, requestedOrientation)
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback, requestedOrientation: Int) {
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

        setFullscreen(userPreferences.hideStatusBarEnabled, false)
        if (fullscreenContainerView != null) {
            val parent = fullscreenContainerView?.parent as ViewGroup
            parent.removeView(fullscreenContainerView)
            fullscreenContainerView?.removeAllViews()
        }

        fullscreenContainerView = null
        customView = null

        Log.d(TAG, "VideoView is being stopped")
        videoView?.stopPlayback()
        videoView?.setOnErrorListener(null)
        videoView?.setOnCompletionListener(null)
        videoView = null

        try {
            customViewCallback?.onCustomViewHidden()
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding custom view", e)
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
        if (drawer_layout.closeDrawerIfOpen(getTabDrawer())) {
            val currentTab = tabsManager.currentTab
            if (currentTab?.canGoBack() == true) {
                currentTab.goBack()
            } else if (currentTab != null) {
                tabsManager.let { presenter?.deleteTab(it.positionOf(currentTab)) }
            }
        } else if (drawer_layout.closeDrawerIfOpen(getBookmarkDrawer())) {
            // Don't do anything other than close the bookmarks drawer when the activity is being
            // delegated to.
        }
    }

    override fun onForwardButtonPressed() {
        val currentTab = tabsManager.currentTab
        if (currentTab?.canGoForward() == true) {
            currentTab.goForward()
            closeDrawers(null)
        }
    }

    override fun onHomeButtonPressed() {
        tabsManager.currentTab?.loadHomePage()
        closeDrawers(null)
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
    override fun onCreateWindow(resultMsg: Message) {
        presenter?.newTab(ResultMessageInitializer(resultMsg), true)
    }

    /**
     * Closes the specified [LightningView]. This implements
     * the JavaScript callback that asks the tab to close itself and
     * is especially helpful when a page creates a redirect and does
     * not need the tab to stay open any longer.
     *
     * @param tab the LightningView to close, delete it.
     */
    override fun onCloseWindow(tab: LightningView) {
        presenter?.deleteTab(tabsManager.positionOf(tab))
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
            currentTab.loadBookmarkPage()
        }
        if (currentTab != null) {
            bookmarksView?.handleUpdatedUrl(currentTab.url)
        }
    }

    override fun handleDownloadDeleted() {
        val currentTab = tabsManager.currentTab
        if (currentTab != null && UrlUtils.isDownloadsUrl(currentTab.url)) {
            currentTab.loadDownloadsPage()
        }
        if (currentTab != null) {
            bookmarksView?.handleUpdatedUrl(currentTab.url)
        }
    }

    override fun handleBookmarkDeleted(bookmark: Bookmark) {
        bookmarksView?.handleBookmarkDeleted(bookmark)
        handleBookmarksChange()
    }

    override fun handleNewTab(newTabType: LightningDialogBuilder.NewTab, url: String) {
        val urlInitializer = UrlInitializer(url)
        when (newTabType) {
            LightningDialogBuilder.NewTab.FOREGROUND -> presenter?.newTab(urlInitializer, true)
            LightningDialogBuilder.NewTab.BACKGROUND -> presenter?.newTab(urlInitializer, false)
            LightningDialogBuilder.NewTab.INCOGNITO -> {
                drawer_layout.closeDrawers()
                val intent = IncognitoActivity.createIntent(this, url.toUri())
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
                shouldShowTabsInDrawer -> drawer_layout.openDrawer(getTabDrawer())
                else -> currentTab.loadHomePage()
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

    /**
     * If the [drawer] is open, close it and return true. Return false otherwise.
     */
    private fun DrawerLayout.closeDrawerIfOpen(drawer: View): Boolean =
        if (isDrawerOpen(drawer)) {
            closeDrawer(drawer)
            true
        } else {
            false
        }

    companion object {

        private const val TAG = "BrowserActivity"

        const val INTENT_PANIC_TRIGGER = "info.guardianproject.panic.action.TRIGGER"

        private const val TAG_BOOKMARK_FRAGMENT = "TAG_BOOKMARK_FRAGMENT"
        private const val TAG_TABS_FRAGMENT = "TAG_TABS_FRAGMENT"

        private const val FILE_CHOOSER_REQUEST_CODE = 1111

        // Constant
        private val MATCH_PARENT = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        private val COVER_SCREEN_PARAMS = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

    }

}
