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
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.controller.UIController;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.react.OnSubscribe;
import acr.browser.lightning.utils.UrlUtils;
import acr.browser.lightning.view.LightningView;

/**
 * Presenter in charge of keeping track of
 * the current tab and setting the current tab
 * of the
 */
public class BrowserPresenter {

    private static final String TAG = BrowserPresenter.class.getSimpleName();

    private TabsManager mTabsModel;
    @Inject PreferenceManager mPreferences;
    @Inject Bus mEventBus;

    @NonNull private final BrowserView mView;
    @Nullable private LightningView mCurrentTab;

    private final boolean mIsIncognito;
    private boolean mIsNewIntent;

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

    public void setupTabs(@Nullable Intent intent, boolean isIncognito) {
        mTabsModel.initializeTabs((Activity) mView, intent, isIncognito)
                .subscribe(new OnSubscribe<Void>() {
                    @Override
                    public void onComplete() {
                        // At this point we always have at least a tab in the tab manager
                        tabChanged(mTabsModel.last());
                        mView.updateTabNumber(mTabsModel.size());
                    }
                });
    }

    public void tabChangeOccurred(LightningView tab) {
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
                    mCurrentTab.onPause();
                    mCurrentTab.setForegroundTab(false);
                }
                newTab.onResume();
                newTab.resumeTimers();
                newTab.setForegroundTab(true);
                mView.updateProgress(newTab.getProgress());
                mView.setBackButtonEnabled(newTab.canGoBack());
                mView.setForwardButtonEnabled(newTab.canGoForward());
                mView.updateUrl(newTab.getUrl(), true);
                mView.setTabView(newTab.getWebView());
                int index = mTabsModel.indexOfTab(newTab);
                if (index >= 0) {
                    mView.notifyTabViewChanged(mTabsModel.indexOfTab(newTab));
                }
            }
        }

        mCurrentTab = newTab;
    }

    public void closeAllOtherTabs() {

        while (mTabsModel.last() != mTabsModel.indexOfCurrentTab()) {
            deleteTab(mTabsModel.last());
        }

        while (0 != mTabsModel.indexOfCurrentTab()) {
            deleteTab(0);
        }

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
            mIsNewIntent = false;
            mView.closeActivity();
        }

        mView.updateTabNumber(mTabsModel.size());

        Log.d(Constants.TAG, "deleted tab");
    }

    public void onNewIntent(final Intent intent) {
        mTabsModel.doAfterInitialization(new Runnable() {
            @Override
            public void run() {
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
        });
    }

    public void loadUrlInCurrentView(final String url) {
        final LightningView currentTab = mTabsModel.getCurrentTab();
        if (currentTab == null) {
            // This is a problem, probably an assert will be better than a return
            return;
        }

        currentTab.loadUrl(url);
    }

    public void shutdown() {
        onTabChanged(null);
        mTabsModel.setTabNumberChangedListener(null);
        mTabsModel.cancelPendingWork();
    }

    public void tabChanged(int position) {
        if (position < 0) {
            return;
        }
        LightningView tab = mTabsModel.switchToTab(position);
        onTabChanged(tab);
    }

    public synchronized boolean newTab(String url, boolean show) {
        // Limit number of tabs for limited version of app
        if (!Constants.FULL_VERSION && mTabsModel.size() >= 10) {
            mView.showSnackbar(R.string.max_tabs);
            return false;
        }

        Log.d(TAG, "New tab, show: " + show);

        mIsNewIntent = false;
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

}
