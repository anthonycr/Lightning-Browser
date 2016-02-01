package acr.browser.lightning.browser;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.squareup.otto.Bus;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.activity.TabsManager;
import acr.browser.lightning.activity.TabsManager.TabChangeListener;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.bus.BrowserEvents;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.utils.UrlUtils;
import acr.browser.lightning.view.LightningView;

/**
 * Presenter in charge of keeping track of
 * the current tab and setting the current tab
 * of the
 */
public class BrowserPresenter {

    private static final String TAG = BrowserPresenter.class.getSimpleName();

    @Inject TabsManager mTabsModel;
    @Inject PreferenceManager mPreferences;
    @Inject Bus mEventBus;

    @NonNull private BrowserView mView;
    @Nullable private LightningView mCurrentTab;

    private boolean mIsIncognito;
    private boolean mIsNewIntent;

    private static class TabListener implements TabChangeListener {

        private BrowserPresenter mPresenter;

        public TabListener(@NonNull BrowserPresenter presenter) {
            mPresenter = presenter;
        }

        @Override
        public void tabChanged(@Nullable LightningView newTab) {
            mPresenter.onTabChanged(newTab);
        }
    }

    public BrowserPresenter(@NonNull BrowserView view, boolean isIncognito) {
        BrowserApp.getAppComponent().inject(this);
        mView = view;
        mIsIncognito = isIncognito;
        mTabsModel.setTabChangeListener(new TabListener(this));
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
                    mCurrentTab.onPause();
                    mCurrentTab.setForegroundTab(false);
                }
                newTab.onResume();
                newTab.resumeTimers();
                newTab.setForegroundTab(true);
                mView.updateProgress(newTab.getProgress());
                mView.updateUrl(newTab.getUrl(), true);
                mView.setTabView(newTab.getWebView());
            }
        }

        mCurrentTab = newTab;
    }

    public void deleteTab(int position) {
        final LightningView tabToDelete = mTabsModel.getTabAtPosition(position);

        if (tabToDelete == null) {
            return;
        }

        if (!UrlUtils.isSpecialUrl(tabToDelete.getUrl()) && !mIsIncognito) {
            mPreferences.setSavedUrl(tabToDelete.getUrl());
        }
        final boolean isShown = tabToDelete.isShown();
        boolean shouldClose = mIsNewIntent && isShown;
        final LightningView currentTab = mTabsModel.getCurrentTab();
        if (mTabsModel.size() == 1 && currentTab != null &&
                (UrlUtils.isSpecialUrl(currentTab.getUrl()) ||
                        currentTab.getUrl().equals(mPreferences.getHomepage()))) {
            mView.closeActivity();
            return;
        } else {
            if (isShown) {
                mView.removeTabView();
            }
            mTabsModel.deleteTab(position);
        }
        final LightningView afterTab = mTabsModel.getCurrentTab();
        if (afterTab == null) {
            mView.closeBrowser();
            return;
        } else if (afterTab != currentTab) {
            //TODO remove this?
//            switchTabs(currentTab, afterTab);
//            if (currentTab != null) {
//                currentTab.pauseTimers();
//            }
        }

        mEventBus.post(new BrowserEvents.TabsChanged());

        if (shouldClose) {
            mIsNewIntent = false;
            mView.closeActivity();
        }

        Log.d(Constants.TAG, "deleted tab");
    }

    public void onNewIntent(Intent intent) {
        final String url;
        if (intent != null) {
            url = intent.getDataString();
        } else {
            url = null;
        }
        int num = 0;
        if (intent != null && intent.getExtras() != null) {
            num = intent.getExtras().getInt(Constants.INTENT_ORIGIN);
        }

        if (num == 1) {
            loadUrlInCurrentView(url);
        } else if (url != null) {
            if (url.startsWith(Constants.FILE)) {
                mView.showBlockedLocalFileDialog(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        newTab(url, true);
                    }
                });
            } else {
                newTab(url, true);
            }
            mIsNewIntent = true;
        }
    }

    public void loadUrlInCurrentView(final String url) {
        final LightningView currentTab = mTabsModel.getCurrentTab();
        if (currentTab == null) {
            // This is a problem, probably an assert will be better than a return
            return;
        }

        currentTab.loadUrl(url);
    }

    public synchronized boolean newTab(String url, boolean show) {
        // Limit number of tabs for limited version of app
        if (!Constants.FULL_VERSION && mTabsModel.size() >= 10) {
            mView.showSnackbar(R.string.max_tabs);
            return false;
        }
        mIsNewIntent = false;
        LightningView startingTab = mTabsModel.newTab((Activity) mView, url, mIsIncognito);
        if (mTabsModel.size() == 1) {
            startingTab.resumeTimers();
        }

        if (show) {
            mTabsModel.switchToTab(mTabsModel.size() - 1);
        }

        // TODO Restore this
        // new Handler().postDelayed(new Runnable() {
        //    @Override
        //    public void run() {
        //        mDrawerListLeft.smoothScrollToPosition(mTabsManager.size() - 1);
        //    }
        // }, 300);

        return true;
    }

    public void destroy() {
        mTabsModel.setTabChangeListener(null);
    }

}
