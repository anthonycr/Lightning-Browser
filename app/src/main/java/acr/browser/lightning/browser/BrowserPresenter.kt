package acr.browser.lightning.browser

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.BuildConfig
import acr.browser.lightning.R
import acr.browser.lightning.constant.FILE
import acr.browser.lightning.constant.INTENT_ORIGIN
import acr.browser.lightning.constant.SCHEME_BOOKMARKS
import acr.browser.lightning.constant.SCHEME_HOMEPAGE
import acr.browser.lightning.controller.UIController
import acr.browser.lightning.html.bookmark.BookmarkPage
import acr.browser.lightning.html.homepage.StartPage
import acr.browser.lightning.preference.PreferenceManager
import acr.browser.lightning.utils.UrlUtils
import acr.browser.lightning.view.LightningView
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.util.Log
import android.webkit.URLUtil
import com.anthonycr.bonsai.CompletableOnSubscribe
import com.anthonycr.bonsai.Schedulers
import javax.inject.Inject

/**
 * Presenter in charge of keeping track of
 * the current tab and setting the current tab
 * of the
 */
class BrowserPresenter(private val view: BrowserView, private val isIncognito: Boolean) {

    @Inject internal lateinit var application: Application
    @Inject internal lateinit var preferences: PreferenceManager
    private val tabsModel: TabsManager
    private var currentTab: LightningView? = null
    private var shouldClose: Boolean = false

    init {
        BrowserApp.getAppComponent().inject(this)
        tabsModel = (view as UIController).getTabModel()
        tabsModel.setTabNumberChangedListener { newNumber -> view.updateTabNumber(newNumber) }
    }

    /**
     * Initializes the tab manager with the new intent
     * that is handed in by the BrowserActivity.
     *
     * @param intent the intent to handle, may be null.
     */
    fun setupTabs(intent: Intent?) {
        tabsModel.initializeTabs(view as Activity, intent, isIncognito)
                .subscribeOn(Schedulers.main())
                .subscribe(object : CompletableOnSubscribe() {
                    override fun onComplete() {
                        // At this point we always have at least a tab in the tab manager
                        view.notifyTabViewInitialized()
                        view.updateTabNumber(tabsModel.size())
                        tabChanged(tabsModel.last())
                    }
                })
    }

    /**
     * Notify the presenter that a change occurred to
     * the current tab. Currently doesn't do anything
     * other than tell the view to notify the adapter
     * about the change.
     *
     * @param tab the tab that changed, may be null.
     */
    fun tabChangeOccurred(tab: LightningView?) {
        view.notifyTabViewChanged(tabsModel.indexOfTab(tab))
    }

    private fun onTabChanged(newTab: LightningView?) {
        Log.d(TAG, "On tab changed")

        val webView = newTab?.webView

        if (newTab == null) {
            view.removeTabView()
            currentTab?.let {
                it.pauseTimers()
                it.onDestroy()
            }
        } else {
            if (webView == null) {
                view.removeTabView()
                currentTab?.let {
                    it.pauseTimers()
                    it.onDestroy()
                }
            } else {
                currentTab.let {
                    // TODO: Restore this when Google fixes the bug where the WebView is
                    // blank after calling onPause followed by onResume.
                    // currentTab.onPause();
                    it?.setIsForegroundTab(false)
                }

                newTab.resumeTimers()
                newTab.onResume()
                newTab.setIsForegroundTab(true)

                view.updateProgress(newTab.progress)
                view.setBackButtonEnabled(newTab.canGoBack())
                view.setForwardButtonEnabled(newTab.canGoForward())
                view.updateUrl(newTab.url, false)
                view.setTabView(webView)
                val index = tabsModel.indexOfTab(newTab)
                if (index >= 0) {
                    view.notifyTabViewChanged(tabsModel.indexOfTab(newTab))
                }
            }
        }

        currentTab = newTab
    }

    /**
     * Closes all tabs but the current tab.
     */
    fun closeAllOtherTabs() {

        while (tabsModel.last() != tabsModel.indexOfCurrentTab()) {
            deleteTab(tabsModel.last())
        }

        while (0 != tabsModel.indexOfCurrentTab()) {
            deleteTab(0)
        }

    }

    private fun mapHomepageToCurrentUrl(): String {
        val homepage = preferences.homepage

        return when (homepage) {
            SCHEME_HOMEPAGE -> "$FILE${StartPage.getStartPageFile(application)}"
            SCHEME_BOOKMARKS -> "$FILE${BookmarkPage.getBookmarkPage(application, null)}"
            else -> homepage
        }
    }

    /**
     * Deletes the tab at the specified position.
     *
     * @param position the position at which to
     * delete the tab.
     */
    fun deleteTab(position: Int) {
        Log.d(TAG, "delete Tab")
        val tabToDelete = tabsModel.getTabAtPosition(position) ?: return

        if (!UrlUtils.isSpecialUrl(tabToDelete.url) && !isIncognito) {
            preferences.savedUrl = tabToDelete.url
        }

        val isShown = tabToDelete.isShown
        val shouldClose = shouldClose && isShown && tabToDelete.isNewTab
        val currentTab = tabsModel.currentTab
        if (tabsModel.size() == 1 && currentTab != null &&
                URLUtil.isFileUrl(currentTab.url) &&
                currentTab.url == mapHomepageToCurrentUrl()) {
            view.closeActivity()
            return
        } else {
            if (isShown) {
                view.removeTabView()
            }
            val currentDeleted = tabsModel.deleteTab(position)
            if (currentDeleted) {
                tabChanged(tabsModel.indexOfCurrentTab())
            }
        }

        val afterTab = tabsModel.currentTab
        view.notifyTabViewRemoved(position)

        if (afterTab == null) {
            view.closeBrowser()
            return
        } else if (afterTab !== currentTab) {
            view.notifyTabViewChanged(tabsModel.indexOfCurrentTab())
        }

        if (shouldClose) {
            this.shouldClose = false
            view.closeActivity()
        }

        view.updateTabNumber(tabsModel.size())

        Log.d(TAG, "deleted tab")
    }

    /**
     * Handle a new intent from the the main
     * BrowserActivity.
     *
     * @param intent the intent to handle,
     * may be null.
     */
    fun onNewIntent(intent: Intent?) {
        tabsModel.doAfterInitialization {
            val url = intent?.dataString
            var tabHashCode = 0
            if (intent != null && intent.extras != null) {
                tabHashCode = intent.extras.getInt(INTENT_ORIGIN)
            }

            if (tabHashCode != 0 && url != null) {
                val tab = tabsModel.getTabForHashCode(tabHashCode)
                tab?.loadUrl(url)
            } else if (url != null) {
                if (URLUtil.isFileUrl(url)) {
                    view.showBlockedLocalFileDialog {
                        newTab(url, true)
                        shouldClose = true
                        val tab = tabsModel.lastTab()
                        tab?.setIsNewTab(true)
                    }
                } else {
                    newTab(url, true)
                    shouldClose = true
                    val tab = tabsModel.lastTab()
                    tab?.setIsNewTab(true)
                }
            }
        }
    }

    /**
     * Loads a URL in the current tab.
     *
     * @param url the URL to load, must
     * not be null.
     */
    fun loadUrlInCurrentView(url: String) {
        val currentTab = tabsModel.currentTab ?: // This is a problem, probably an assert will be better than a return
                return

        currentTab.loadUrl(url)
    }

    /**
     * Notifies the presenter that it should
     * shut down. This should be called when
     * the BrowserActivity is destroyed so that
     * we don't leak any memory.
     */
    fun shutdown() {
        onTabChanged(null)
        tabsModel.setTabNumberChangedListener(null)
        tabsModel.cancelPendingWork()
    }

    /**
     * Notifies the presenter that we wish
     * to switch to a different tab at the
     * specified position. If the position
     * is not in the model, this method will
     * do nothing.
     *
     * @param position the position of the
     * tab to switch to.
     */
    @Synchronized
    fun tabChanged(position: Int) {
        Log.d(TAG, "tabChanged: " + position)
        if (position < 0 || position >= tabsModel.size()) {
            return
        }
        val tab = tabsModel.switchToTab(position)
        onTabChanged(tab)
    }

    /**
     * Open a new tab with the specified URL. You
     * can choose to show the tab or load it in the
     * background.
     *
     * @param url  the URL to load, may be null if you
     * don't wish to load anything.
     * @param show whether or not to switch to this
     * tab after opening it.
     * @return true if we successfully created the tab,
     * false if we have hit max tabs.
     */
    @Synchronized
    fun newTab(url: String?, show: Boolean): Boolean {
        // Limit number of tabs for limited version of app
        if (!BuildConfig.FULL_VERSION && tabsModel.size() >= 10) {
            view.showSnackbar(R.string.max_tabs)
            return false
        }

        Log.d(TAG, "New tab, show: " + show)

        val startingTab = tabsModel.newTab(view as Activity, url, isIncognito)
        if (tabsModel.size() == 1) {
            startingTab.resumeTimers()
        }

        view.notifyTabViewAdded()
        view.updateTabNumber(tabsModel.size())

        if (show) {
            val tab = tabsModel.switchToTab(tabsModel.last())
            onTabChanged(tab)
        }

        return true
    }

    fun onAutoCompleteItemPressed() {
        tabsModel.currentTab?.requestFocus()
    }

    fun findInPage(query: String) {
        tabsModel.currentTab?.find(query)
    }

    fun onAppLowMemory() {
        tabsModel.freeMemory()
    }

    companion object {
        private const val TAG = "BrowserPresenter"
    }

}
