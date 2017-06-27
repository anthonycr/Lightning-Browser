package acr.browser.lightning.browser;

import android.app.Activity;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.URLUtil;

import com.anthonycr.bonsai.CompletableOnSubscribe;
import com.anthonycr.bonsai.Schedulers;

import javax.inject.Inject;

import acr.browser.lightning.BuildConfig;
import acr.browser.lightning.R;
import acr.browser.lightning.BrowserApp;
import acr.browser.lightning.constant.BookmarkPage;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.constant.StartPage;
import acr.browser.lightning.controller.UIController;
import acr.browser.lightning.preference.PreferenceManager;

import acr.browser.lightning.utils.UrlUtils;
import acr.browser.lightning.view.LightningView;

/**
 * Presenter in charge of keeping track of
 * the current tab and setting the current tab
 * of the
 */
public class BrowserPresenter {

    private static final String TAG = "BrowserPresenter";

    @NonNull private final TabsManager mTabsModel;
    @Inject Application mApplication;
    @Inject PreferenceManager mPreferences;

    @NonNull private final BrowserView mView;
    @Nullable private LightningView mCurrentTab;

    private final boolean mIsIncognito;
    private boolean mShouldClose;

    public BrowserPresenter(@NonNull BrowserView view, boolean isIncognito) {
        BrowserApp.getAppComponent().inject(this);
        mTabsModel = ((UIController) view).getTabModel();
        mView = view;
        mIsIncognito = isIncognito;
        mTabsModel.setTabNumberChangedListener(new TabsManager.TabNumberChangedListener() {
            @Override
            public void tabNumberChanged(int newNumber) {
                mView.updateTabNumber(newNumber);
            }
        });
    }

    /**
     * Initializes the tab manager with the new intent
     * that is handed in by the BrowserActivity.
     *
     * @param intent the intent to handle, may be null.
     */
    public void setupTabs(@Nullable Intent intent) {
        mTabsModel.initializeTabs((Activity) mView, intent, mIsIncognito)
            .subscribeOn(Schedulers.main())
            .subscribe(new CompletableOnSubscribe() {
                @Override
                public void onComplete() {
                    // At this point we always have at least a tab in the tab manager
                    mView.notifyTabViewInitialized();
                    mView.updateTabNumber(mTabsModel.size());
                    tabChanged(mTabsModel.last());
                }
            });
    }

    /**
     * Notify the presenter that a change occurred to
     * the current tab. Currently doesn't do anything
     * other than tell the view to notify the adapter
     * about the change.
     *
     * @param tab the tab that changed, may be null.
     */
    public void tabChangeOccurred(@Nullable LightningView tab) {
        mView.notifyTabViewChanged(mTabsModel.indexOfTab(tab));
    }

    private void onTabChanged(@Nullable LightningView newTab) {
        Log.d(TAG, "On tab changed");
        if (newTab == null) {
            mView.removeTabView();
            if (mCurrentTab != null) {
                mCurrentTab.pauseTimers();
                mCurrentTab.onDestroy();
            }
        } else {
            if (newTab.getWebView() == null) {
                mView.removeTabView();
                if (mCurrentTab != null) {
                    mCurrentTab.pauseTimers();
                    mCurrentTab.onDestroy();
                }
            } else {
                if (mCurrentTab != null) {
                    // TODO: Restore this when Google fixes the bug where the WebView is
                    // blank after calling onPause followed by onResume.
                    // mCurrentTab.onPause();
                    mCurrentTab.setIsForegroundTab(false);
                }

                newTab.resumeTimers();
                newTab.onResume();
                newTab.setIsForegroundTab(true);

                mView.updateProgress(newTab.getProgress());
                mView.setBackButtonEnabled(newTab.canGoBack());
                mView.setForwardButtonEnabled(newTab.canGoForward());
                mView.updateUrl(newTab.getUrl(), false);
                mView.setTabView(newTab.getWebView());
                int index = mTabsModel.indexOfTab(newTab);
                if (index >= 0) {
                    mView.notifyTabViewChanged(mTabsModel.indexOfTab(newTab));
                }
            }
        }

        mCurrentTab = newTab;
    }

    /**
     * Closes all tabs but the current tab.
     */
    public void closeAllOtherTabs() {

        while (mTabsModel.last() != mTabsModel.indexOfCurrentTab()) {
            deleteTab(mTabsModel.last());
        }

        while (0 != mTabsModel.indexOfCurrentTab()) {
            deleteTab(0);
        }

    }

    @NonNull
    private String mapHomepageToCurrentUrl() {
        String homepage = mPreferences.getHomepage();
        switch (homepage) {
            case Constants.SCHEME_HOMEPAGE:
                return Constants.FILE + StartPage.getStartPageFile(mApplication);
            case Constants.SCHEME_BOOKMARKS:
                return Constants.FILE + BookmarkPage.getBookmarkPage(mApplication, null);
            default:
                return homepage;
        }
    }

    /**
     * Deletes the tab at the specified position.
     *
     * @param position the position at which to
     *                 delete the tab.
     */
    public void deleteTab(int position) {
        Log.d(TAG, "delete Tab");
        final LightningView tabToDelete = mTabsModel.getTabAtPosition(position);

        if (tabToDelete == null) {
            return;
        }

        if (!UrlUtils.isSpecialUrl(tabToDelete.getUrl()) && !mIsIncognito) {
            mPreferences.setSavedUrl(tabToDelete.getUrl());
        }

        final boolean isShown = tabToDelete.isShown();
        boolean shouldClose = mShouldClose && isShown && tabToDelete.isNewTab();
        final LightningView currentTab = mTabsModel.getCurrentTab();
        if (mTabsModel.size() == 1 && currentTab != null &&
            URLUtil.isFileUrl(currentTab.getUrl()) &&
            currentTab.getUrl().equals(mapHomepageToCurrentUrl())) {
            mView.closeActivity();
            return;
        } else {
            if (isShown) {
                mView.removeTabView();
            }
            boolean currentDeleted = mTabsModel.deleteTab(position);
            if (currentDeleted) {
                tabChanged(mTabsModel.indexOfCurrentTab());
            }
        }

        final LightningView afterTab = mTabsModel.getCurrentTab();
        mView.notifyTabViewRemoved(position);

        if (afterTab == null) {
            mView.closeBrowser();
            return;
        } else if (afterTab != currentTab) {
            //TODO remove this?
//            switchTabs(currentTab, afterTab);
//            if (currentTab != null) {
//                currentTab.pauseTimers();
//            }
            mView.notifyTabViewChanged(mTabsModel.indexOfCurrentTab());
        }

        if (shouldClose) {
            mShouldClose = false;
            mView.closeActivity();
        }

        mView.updateTabNumber(mTabsModel.size());

        Log.d(TAG, "deleted tab");
    }

    /**
     * Handle a new intent from the the main
     * BrowserActivity.
     *
     * @param intent the intent to handle,
     *               may be null.
     */
    public void onNewIntent(@Nullable final Intent intent) {
        mTabsModel.doAfterInitialization(new Runnable() {
            @Override
            public void run() {
                final String url;
                if (intent != null) {
                    url = intent.getDataString();
                } else {
                    url = null;
                }
                int tabHashCode = 0;
                if (intent != null && intent.getExtras() != null) {
                    tabHashCode = intent.getExtras().getInt(Constants.INTENT_ORIGIN);
                }

                if (tabHashCode != 0) {
                    LightningView tab = mTabsModel.getTabForHashCode(tabHashCode);
                    if (tab != null) {
                        tab.loadUrl(url);
                    }
                } else if (url != null) {
                    if (URLUtil.isFileUrl(url)) {
                        mView.showBlockedLocalFileDialog(new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                newTab(url, true);
                                mShouldClose = true;
                                LightningView tab = mTabsModel.lastTab();
                                if (tab != null) {
                                    tab.setIsNewTab(true);
                                }
                            }
                        });
                    } else {
                        newTab(url, true);
                        mShouldClose = true;
                        LightningView tab = mTabsModel.lastTab();
                        if (tab != null) {
                            tab.setIsNewTab(true);
                        }
                    }
                }
            }
        });
    }

    /**
     * Loads a URL in the current tab.
     *
     * @param url the URL to load, must
     *            not be null.
     */
    public void loadUrlInCurrentView(@NonNull final String url) {
        final LightningView currentTab = mTabsModel.getCurrentTab();
        if (currentTab == null) {
            // This is a problem, probably an assert will be better than a return
            return;
        }

        currentTab.loadUrl(url);
    }

    /**
     * Notifies the presenter that it should
     * shut down. This should be called when
     * the BrowserActivity is destroyed so that
     * we don't leak any memory.
     */
    public void shutdown() {
        onTabChanged(null);
        mTabsModel.setTabNumberChangedListener(null);
        mTabsModel.cancelPendingWork();
    }

    /**
     * Notifies the presenter that we wish
     * to switch to a different tab at the
     * specified position. If the position
     * is not in the model, this method will
     * do nothing.
     *
     * @param position the position of the
     *                 tab to switch to.
     */
    public synchronized void tabChanged(int position) {
        Log.d(TAG, "tabChanged: " + position);
        if (position < 0 || position >= mTabsModel.size()) {
            return;
        }
        LightningView tab = mTabsModel.switchToTab(position);
        onTabChanged(tab);
    }

    /**
     * Open a new tab with the specified URL. You
     * can choose to show the tab or load it in the
     * background.
     *
     * @param url  the URL to load, may be null if you
     *             don't wish to load anything.
     * @param show whether or not to switch to this
     *             tab after opening it.
     * @return true if we successfully created the tab,
     * false if we have hit max tabs.
     */
    public synchronized boolean newTab(@Nullable String url, boolean show) {
        // Limit number of tabs for limited version of app
        if (!BuildConfig.FULL_VERSION && mTabsModel.size() >= 10) {
            mView.showSnackbar(R.string.max_tabs);
            return false;
        }

        Log.d(TAG, "New tab, show: " + show);

        LightningView startingTab = mTabsModel.newTab((Activity) mView, url, mIsIncognito);
        if (mTabsModel.size() == 1) {
            startingTab.resumeTimers();
        }

        mView.notifyTabViewAdded();

        if (show) {
            LightningView tab = mTabsModel.switchToTab(mTabsModel.last());
            onTabChanged(tab);
        }

        mView.updateTabNumber(mTabsModel.size());

        return true;
    }

    public void onAutoCompleteItemPressed() {
        final LightningView currentTab = mTabsModel.getCurrentTab();
        if (currentTab != null) {
            currentTab.requestFocus();
        }
    }

    public void findInPage(@NonNull String query) {
        final LightningView currentView = mTabsModel.getCurrentTab();
        if (currentView != null) {
            currentView.find(query);
        }
    }

    public void onAppLowMemory() {
        mTabsModel.freeMemory();
    }

}
