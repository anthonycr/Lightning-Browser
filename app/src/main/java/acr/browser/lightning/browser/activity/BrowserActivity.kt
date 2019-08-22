/*
 * Copyright 2015 Anthony Restaino
 */

package acr.browser.lightning.browser.activity

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
import acr.browser.lightning.device.ScreenSize
import acr.browser.lightning.di.DatabaseScheduler
import acr.browser.lightning.di.MainHandler
import acr.browser.lightning.di.MainScheduler
import acr.browser.lightning.di.injector
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.dialog.DialogItem
import acr.browser.lightning.dialog.LightningDialogBuilder
import acr.browser.lightning.extensions.*
import acr.browser.lightning.html.bookmark.BookmarkPageFactory
import acr.browser.lightning.html.history.HistoryPageFactory
import acr.browser.lightning.html.homepage.HomePageFactory
import acr.browser.lightning.interpolator.BezierDecelerateInterpolator
import acr.browser.lightning.log.Logger
import acr.browser.lightning.notifications.IncognitoNotification
import acr.browser.lightning.reading.activity.ReadingActivity
import acr.browser.lightning.search.SearchEngineProvider
import acr.browser.lightning.search.SuggestionsAdapter
import acr.browser.lightning.settings.activity.SettingsActivity
import acr.browser.lightning.ssl.SSLState
import acr.browser.lightning.utils.*
import acr.browser.lightning.view.*
import acr.browser.lightning.view.SearchView
import acr.browser.lightning.view.find.FindResults
import android.app.Activity
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.ActivityInfo
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
import android.os.Handler
import android.os.Message
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.style.CharacterStyle
import android.text.style.ParagraphStyle
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
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.palette.graphics.Palette
import butterknife.ButterKnife
import com.anthonycr.grant.PermissionsManager
import io.reactivex.Completable
import io.reactivex.Scheduler
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.browser_content.*
import kotlinx.android.synthetic.main.search.*
import kotlinx.android.synthetic.main.search_interface.*
import kotlinx.android.synthetic.main.toolbar.*
import java.io.IOException
import javax.inject.Inject
import kotlin.system.exitProcess

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
    private var currentUiColor = Color.BLACK
    private var keyDownStartTime: Long = 0
    private var searchText: String? = null
    private var cameraPhotoPath: String? = null

    private var findResult: FindResults? = null

    // The singleton BookmarkManager
    @Inject lateinit var bookmarkManager: BookmarkRepository
    @Inject lateinit var historyModel: HistoryRepository
    @Inject lateinit var searchBoxModel: SearchBoxModel
    @Inject lateinit var searchEngineProvider: SearchEngineProvider
    @Inject lateinit var inputMethodManager: InputMethodManager
    @Inject lateinit var clipboardManager: ClipboardManager
    @Inject lateinit var notificationManager: NotificationManager
    @Inject @field:DatabaseScheduler lateinit var databaseScheduler: Scheduler
    @Inject @field:MainScheduler lateinit var mainScheduler: Scheduler
    @Inject lateinit var tabsManager: TabsManager
    @Inject lateinit var homePageFactory: HomePageFactory
    @Inject lateinit var bookmarkPageFactory: BookmarkPageFactory
    @Inject lateinit var historyPageFactory: HistoryPageFactory
    @Inject lateinit var historyPageInitializer: HistoryPageInitializer
    @Inject lateinit var downloadPageInitializer: DownloadPageInitializer
    @Inject lateinit var homePageInitializer: HomePageInitializer
    @Inject @field:MainHandler lateinit var mainHandler: Handler
    @Inject lateinit var proxyUtils: ProxyUtils
    @Inject lateinit var logger: Logger
    @Inject lateinit var bookmarksDialogBuilder: LightningDialogBuilder
    @Inject lateinit var screenSize: ScreenSize

    // Image
    private var webPageBitmap: Bitmap? = null
    private val backgroundDrawable = ColorDrawable()
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
        injector.inject(this)
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

        presenter = BrowserPresenter(
            this,
            isIncognito(),
            userPreferences,
            tabsManager,
            mainScheduler,
            homePageFactory,
            bookmarkPageFactory,
            RecentTabModel(),
            logger
        )

        initialize(savedInstanceState)
    }

    private fun initialize(savedInstanceState: Bundle?) {
        initializeToolbarHeight(resources.configuration)
        setSupportActionBar(toolbar)
        val actionBar = requireNotNull(supportActionBar)

        //TODO make sure dark theme flag gets set correctly
        isDarkTheme = userPreferences.useTheme != 0 || isIncognito()
        shouldShowTabsInDrawer = userPreferences.showTabsInDrawer
        swapBookmarksAndTabs = userPreferences.bookmarksAndTabsSwapped

        // initialize background ColorDrawable
        val primaryColor = ThemeUtils.getPrimaryColor(this)
        backgroundDrawable.color = primaryColor

        // Drawer stutters otherwise
        left_drawer.setLayerType(LAYER_TYPE_NONE, null)
        right_drawer.setLayerType(LAYER_TYPE_NONE, null)

        drawer_layout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit

            override fun onDrawerOpened(drawerView: View) = Unit

            override fun onDrawerClosed(drawerView: View) = Unit

            override fun onDrawerStateChanged(newState: Int) {
                if (newState == DrawerLayout.STATE_DRAGGING) {
                    left_drawer.setLayerType(LAYER_TYPE_HARDWARE, null)
                    right_drawer.setLayerType(LAYER_TYPE_HARDWARE, null)
                } else if (newState == DrawerLayout.STATE_IDLE) {
                    left_drawer.setLayerType(LAYER_TYPE_NONE, null)
                    right_drawer.setLayerType(LAYER_TYPE_NONE, null)
                }
            }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !shouldShowTabsInDrawer) {
            window.statusBarColor = Color.BLACK
        }

        setNavigationDrawerWidth()
        drawer_layout.addDrawerListener(DrawerLocker())

        webPageBitmap = drawable(R.drawable.ic_webpage).toBitmap()

        val fragmentManager = supportFragmentManager

        val tabsFragment: TabsFragment? = fragmentManager.findFragmentByTag(TAG_TABS_FRAGMENT) as? TabsFragment
        val bookmarksFragment: BookmarksFragment? = fragmentManager.findFragmentByTag(TAG_BOOKMARK_FRAGMENT) as? BookmarksFragment

        if (tabsFragment != null) {
            fragmentManager.beginTransaction().remove(tabsFragment).commit()
        }

        tabsView = tabsFragment
            ?: TabsFragment.createTabsFragment(isIncognito(), shouldShowTabsInDrawer)

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
            tabs_toolbar_container.visibility = GONE
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
                    it.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
                }
                updateTabNumber(0)

                // Post drawer locking in case the activity is being recreated
                mainHandler.post { drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, getTabDrawer()) }
            } else {

                // Post drawer locking in case the activity is being recreated
                mainHandler.post { drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, getTabDrawer()) }
                it.setImageResource(R.drawable.ic_action_home)
            }
        }

        // Post drawer locking in case the activity is being recreated
        mainHandler.post { drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, getBookmarkDrawer()) }

        customView.findViewById<FrameLayout>(R.id.arrow_button).setOnClickListener(this)

        // create the search EditText in the ToolBar
        searchView = customView.findViewById<SearchView>(R.id.search).apply {
            setCompoundDrawablesWithIntrinsicBounds(sslDrawable, null, null, null)
            search_refresh.setImageResource(R.drawable.ic_action_refresh)

            val searchListener = SearchListenerClass()
            setOnKeyListener(searchListener)
            onFocusChangeListener = searchListener
            setOnEditorActionListener(searchListener)
            onPreFocusListener = searchListener
            addTextChangedListener(searchListener)

            initializeSearchSuggestions(this)
        }

        search_refresh.setOnClickListener {
            if (searchView?.hasFocus() == true) {
                searchView?.setText("")
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
        logger.log(TAG, "Closing browser")
        tabsManager.newTab(this, NoOpInitializer(), false)
        tabsManager.switchToTab(0)
        tabsManager.clearSavedState()

        historyPageFactory.deleteHistoryPage().subscribe()
        closeBrowser()
        // System exit needed in the case of receiving
        // the panic intent since finish() isn't completely
        // closing the browser
        exitProcess(1)
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
                searchView?.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
                search_refresh.setImageResource(R.drawable.ic_action_delete)
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
            if (!url.isSpecialUrl()) {
                if (searchView?.hasFocus() == false) {
                    searchView?.setText(url)
                }
            }
        }
    }

    private inner class DrawerLocker : DrawerLayout.DrawerListener {

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
        val maxWidth = if (screenSize.isTablet()) {
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
                changeToolbarBackground(currentView.favicon ?: webBitmap, null)
            } else if (!isIncognito() && !isDarkTheme) {
                changeToolbarBackground(webBitmap, null)
            }
        }

        val manager = supportFragmentManager
        val tabsFragment = manager.findFragmentByTag(TAG_TABS_FRAGMENT) as? TabsFragment
        tabsFragment?.reinitializePreferences()

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
            mainHandler.postDelayed(longPressBackRunnable, ViewConfiguration.getLongPressTimeout().toLong())
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            mainHandler.removeCallbacks(longPressBackRunnable)
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
                            homePageInitializer,
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
                if (currentView != null
                    && currentView.url.isNotBlank()
                    && !currentView.url.isSpecialUrl()) {
                    HistoryEntry(currentView.url, currentView.title).also {
                        Utils.createShortcut(this, it, currentView.favicon ?: webPageBitmap!!)
                        logger.log(TAG, "Creating shortcut: ${it.title} ${it.url}")
                    }
                }
                return true
            }
            R.id.action_new_tab -> {
                presenter?.newTab(homePageInitializer, true)
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
                if (currentUrl != null && !currentUrl.isSpecialUrl()) {
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
                if (currentUrl != null && !currentUrl.isSpecialUrl()) {
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
        val bookmark = Bookmark.Entry(url, title, 0, Bookmark.Folder.Root)
        bookmarksDialogBuilder.showAddBookmarkDialog(this, this, bookmark)
    }

    private fun deleteBookmark(title: String, url: String) {
        bookmarkManager.deleteBookmark(Bookmark.Entry(url, title, 0, Bookmark.Folder.Root))
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribe { boolean ->
                if (boolean) {
                    handleBookmarksChange()
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
            findResult = presenter?.findInPage(text)
            showFindInPageControls(text)
        }
    }

    private fun showFindInPageControls(text: String) {
        search_bar.visibility = VISIBLE

        findViewById<TextView>(R.id.search_query).text = "'$text'"
        findViewById<ImageButton>(R.id.button_next).setOnClickListener(this)
        findViewById<ImageButton>(R.id.button_back).setOnClickListener(this)
        findViewById<ImageButton>(R.id.button_quit).setOnClickListener(this)
    }

    override fun getTabModel(): TabsManager = tabsManager

    override fun showCloseDialog(position: Int) {
        if (position < 0) {
            return
        }
        BrowserDialog.show(this, R.string.dialog_title_close_browser,
            DialogItem(title = R.string.close_tab) {
                presenter?.deleteTab(position)
            },
            DialogItem(title = R.string.close_other_tabs) {
                presenter?.closeAllOtherTabs()
            },
            DialogItem(title = R.string.close_all_tabs, onClick = this::closeBrowser))
    }

    override fun notifyTabViewRemoved(position: Int) {
        logger.log(TAG, "Notify Tab Removed: $position")
        tabsView?.tabRemoved(position)
    }

    override fun notifyTabViewAdded() {
        logger.log(TAG, "Notify Tab Added")
        tabsView?.tabAdded()
    }

    override fun notifyTabViewChanged(position: Int) {
        logger.log(TAG, "Notify Tab Changed: $position")
        tabsView?.tabChanged(position)
    }

    override fun notifyTabViewInitialized() {
        logger.log(TAG, "Notify Tabs Initialized")
        tabsView?.tabsInitialized()
    }

    override fun updateSslState(sslState: SSLState) {
        sslDrawable = when (sslState) {
            is SSLState.None -> null
            is SSLState.Valid -> {
                val bitmap = DrawableUtils.createImageInsetInRoundedSquare(this, R.drawable.ic_secured)
                val securedDrawable = BitmapDrawable(resources, bitmap)
                securedDrawable
            }
            is SSLState.Invalid -> {
                val bitmap = DrawableUtils.createImageInsetInRoundedSquare(this, R.drawable.ic_unsecured)
                val unsecuredDrawable = BitmapDrawable(resources, bitmap)
                unsecuredDrawable
            }
        }

        searchView?.setCompoundDrawablesWithIntrinsicBounds(sslDrawable, null, null, null)
    }

    override fun tabChanged(tab: LightningView) {
        presenter?.tabChangeOccurred(tab)
    }

    override fun removeTabView() {

        logger.log(TAG, "Remove the tab view")

        currentTabView.removeFromParent()

        currentTabView = null

        // Use a delayed handler to make the transition smooth
        // otherwise it will get caught up with the showTab code
        // and cause a janky motion
        mainHandler.postDelayed(drawer_layout::closeDrawers, 200)

    }

    override fun setTabView(view: View) {
        if (currentTabView == view) {
            return
        }

        logger.log(TAG, "Setting the tab view")

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
        mainHandler.postDelayed(drawer_layout::closeDrawers, 200)
    }

    override fun showBlockedLocalFileDialog(onPositiveClick: Function0<Unit>) =
        AlertDialog.Builder(this)
            .setCancelable(true)
            .setTitle(R.string.title_warning)
            .setMessage(R.string.message_blocked_local)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.action_open) { _, _ -> onPositiveClick.invoke() }
            .resizeAndShow()

    override fun showSnackbar(@StringRes resource: Int) = snackbar(resource)

    override fun tabCloseClicked(position: Int) {
        presenter?.deleteTab(position)
    }

    override fun tabClicked(position: Int) = showTab(position)

    override fun newTabButtonClicked() {
        presenter?.newTab(
            homePageInitializer,
            true
        )
    }

    override fun newTabButtonLongClicked() {
        presenter?.onNewTabLongClicked()
    }

    override fun bookmarkButtonClicked() {
        val currentTab = tabsManager.currentTab
        val url = currentTab?.url
        val title = currentTab?.title
        if (url == null || title == null) {
            return
        }

        if (!url.isSpecialUrl()) {
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
        mainHandler.postDelayed({ closeDrawers(null) }, 150)
    }

    override fun handleHistoryChange() {
        historyPageFactory
            .buildPage()
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
            logger.log(TAG, "Cache Cleared")
        }
        if (userPreferences.clearHistoryExitEnabled && !isIncognito()) {
            WebUtils.clearHistory(this, historyModel, databaseScheduler)
            logger.log(TAG, "History Cleared")
        }
        if (userPreferences.clearCookiesExitEnabled && !isIncognito()) {
            WebUtils.clearCookies(this)
            logger.log(TAG, "Cookies Cleared")
        }
        if (userPreferences.clearWebStorageExitEnabled && !isIncognito()) {
            WebUtils.clearWebStorage()
            logger.log(TAG, "WebStorage Cleared")
        } else if (isIncognito()) {
            WebUtils.clearWebStorage()     // We want to make sure incognito mode is secure
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        logger.log(TAG, "onConfigurationChanged")

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
            toolbar.layoutParams = ConstraintLayout.LayoutParams(LayoutParams.MATCH_PARENT, toolbarSize)
            toolbar.minimumHeight = toolbarSize
            toolbar.doOnLayout { setWebViewTranslation(toolbar_layout.height.toFloat()) }
            toolbar.requestLayout()
        }

    override fun closeBrowser() {
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
                logger.log(TAG, "onBackPressed")
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
                logger.log(TAG, "This shouldn't happen ever")
                super.onBackPressed()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        logger.log(TAG, "onPause")
        tabsManager.pauseAll()

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
        logger.log(TAG, "onDestroy")

        mainHandler.removeCallbacksAndMessages(null)

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
        logger.log(TAG, "onResume")
        if (swapBookmarksAndTabs != userPreferences.bookmarksAndTabsSwapped) {
            restart()
        }

        suggestionsAdapter?.let {
            it.refreshPreferences()
            it.refreshBookmarks()
        }
        tabsManager.resumeAll()
        initializePreferences()

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
        val searchUrl = "$searchText$QUERY_PLACE_HOLDER"
        if (currentTab != null) {
            currentTab.stopLoading()
            presenter?.loadUrlInCurrentView(smartUrlFilter(query.trim(), true, searchUrl))
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
    override fun changeToolbarBackground(favicon: Bitmap?, tabBackground: Drawable?) {
        val defaultColor = ContextCompat.getColor(this, R.color.primary_color)
        if (currentUiColor == Color.BLACK) {
            currentUiColor = defaultColor
        }
        Palette.from(favicon ?: webPageBitmap!!).generate { palette ->
            // OR with opaque black to remove transparency glitches
            val color = Color.BLACK or (palette?.getVibrantColor(defaultColor) ?: defaultColor)

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
                        mainHandler.post { window.setBackgroundDrawable(backgroundDrawable) }
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
                arrowImageView?.setImageResource(R.drawable.incognito_mode)
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
        if (url.isSpecialUrl()) {
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

        getUrl.setAdapter(SuggestionsAdapter(this, isIncognito()))
    }

    /**
     * function that opens the HTML history page in the browser
     */
    private fun openHistory() {
        presenter?.newTab(
            historyPageInitializer,
            true
        )
    }

    private fun openDownloads() {
        presenter?.newTab(
            downloadPageInitializer,
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

        drawer_layout.addDrawerListener(object : DrawerLayout.DrawerListener {
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
        forwardMenuItem?.isEnabled = enabled
        tabsView?.setGoForwardEnabled(enabled)
    }

    override fun setBackButtonEnabled(enabled: Boolean) {
        backMenuItem?.isEnabled = enabled
        tabsView?.setGoBackEnabled(enabled)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        backMenuItem = menu.findItem(R.id.action_back)
        forwardMenuItem = menu.findItem(R.id.action_forward)
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
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                val result = if (intent == null || resultCode != Activity.RESULT_OK) {
                    null
                } else {
                    intent.data
                }

                uploadMessageCallback?.onReceiveValue(result)
                uploadMessageCallback = null
            } else {
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
            }
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
            logger.log(TAG, "Unable to create Image File", ex)
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
        onShowCustomView(view, callback, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback, requestedOrientation: Int) {
        val currentTab = tabsManager.currentTab
        if (customView != null) {
            try {
                callback.onCustomViewHidden()
            } catch (e: Exception) {
                logger.log(TAG, "Error hiding custom view", e)
            }

            return
        }

        try {
            view.keepScreenOn = true
        } catch (e: SecurityException) {
            logger.log(TAG, "WebView is not allowed to keep the screen on")
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
        setFullscreen(enabled = true, immersive = true)
        currentTab?.setVisibility(INVISIBLE)
    }

    override fun onHideCustomView() {
        val currentTab = tabsManager.currentTab
        if (customView == null || customViewCallback == null || currentTab == null) {
            if (customViewCallback != null) {
                try {
                    customViewCallback?.onCustomViewHidden()
                } catch (e: Exception) {
                    logger.log(TAG, "Error hiding custom view", e)
                }

                customViewCallback = null
            }
            return
        }
        logger.log(TAG, "onHideCustomView")
        currentTab.setVisibility(VISIBLE)
        try {
            customView?.keepScreenOn = false
        } catch (e: SecurityException) {
            logger.log(TAG, "WebView is not allowed to keep the screen on")
        }

        setFullscreen(userPreferences.hideStatusBarEnabled, false)
        if (fullscreenContainerView != null) {
            val parent = fullscreenContainerView?.parent as ViewGroup
            parent.removeView(fullscreenContainerView)
            fullscreenContainerView?.removeAllViews()
        }

        fullscreenContainerView = null
        customView = null

        logger.log(TAG, "VideoView is being stopped")
        videoView?.stopPlayback()
        videoView?.setOnErrorListener(null)
        videoView?.setOnCompletionListener(null)
        videoView = null

        try {
            customViewCallback?.onCustomViewHidden()
        } catch (e: Exception) {
            logger.log(TAG, "Error hiding custom view", e)
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
        logger.log(TAG, "onWindowFocusChanged")
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
                decor.systemUiVisibility = (SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or SYSTEM_UI_FLAG_FULLSCREEN
                    or SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            } else {
                decor.systemUiVisibility = SYSTEM_UI_FLAG_VISIBLE
            }
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            decor.systemUiVisibility = SYSTEM_UI_FLAG_VISIBLE
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
            logger.log(TAG, "showActionBar")
            if (toolbar_layout == null)
                return

            var height = toolbar_layout.height
            if (height == 0) {
                toolbar_layout.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
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
        if (currentTab != null && currentTab.url.isBookmarkUrl()) {
            currentTab.loadBookmarkPage()
        }
        if (currentTab != null) {
            bookmarksView?.handleUpdatedUrl(currentTab.url)
        }
        suggestionsAdapter?.refreshBookmarks()
    }

    override fun handleDownloadDeleted() {
        val currentTab = tabsManager.currentTab
        if (currentTab != null && currentTab.url.isDownloadsUrl()) {
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
            searchView?.setCompoundDrawablesWithIntrinsicBounds(sslDrawable, null, null, null)
            search_refresh.setImageResource(if (isLoading) R.drawable.ic_action_delete else R.drawable.ic_action_refresh)
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
            R.id.button_next -> findResult?.nextResult()
            R.id.button_back -> findResult?.previousResult()
            R.id.button_quit -> {
                findResult?.clearResults()
                findResult = null
                search_bar.visibility = GONE
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
