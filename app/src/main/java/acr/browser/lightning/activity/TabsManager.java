package acr.browser.lightning.activity;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.webkit.WebView;

import com.squareup.otto.Bus;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import acr.browser.lightning.R;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.utils.FileUtils;
import acr.browser.lightning.view.LightningView;

/**
 * @author Stefano Pacifici
 * @date 2015/09/14
 */
@Singleton
public class TabsManager {

    private static final String TAG = TabsManager.class.getSimpleName();
    private final List<LightningView> mWebViewList = new ArrayList<>();
    private LightningView mCurrentTab;
    private static final String BUNDLE_KEY = "WEBVIEW_";
    private static final String BUNDLE_STORAGE = "SAVED_TABS.parcel";

    @Inject PreferenceManager mPreferenceManager;
    @Inject Bus mEventBus;
    @Inject Application mApp;

    @Inject
    public TabsManager() {}

    /**
     * Restores old tabs that were open before the browser
     * was closed. Handles the intent used to open the browser.
     *
     * @param activity  the activity needed to create tabs.
     * @param intent    the intent that started the browser activity.
     * @param incognito whether or not we are in incognito mode.
     */
    public synchronized void restoreTabsAndHandleIntent(final Activity activity,
                                                        final Intent intent,
                                                        final boolean incognito) {
        // If incognito, only create one tab, do not handle intent
        // in order to protect user privacy
        if (incognito && mWebViewList.isEmpty()) {
            newTab(activity, null, true);
            return;
        }

        String url = null;
        if (intent != null) {
            url = intent.getDataString();
        }
        mWebViewList.clear();
        mCurrentTab = null;
        if (mPreferenceManager.getRestoreLostTabsEnabled()) {
            restoreState(activity);
        }
        if (url != null) {
            if (url.startsWith(Constants.FILE)) {
                final String urlToLoad = url;
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setCancelable(true)
                        .setTitle(R.string.title_warning)
                        .setMessage(R.string.message_blocked_local)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.action_open, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                newTab(activity, urlToLoad, false);
                            }
                        })
                        .show();
            } else {
                newTab(activity, url, false);
            }
        }
        if (mWebViewList.size() == 0) {
            newTab(activity, null, false);
        }
    }

    /**
     * Return the tab at the given position in tabs list, or null if position is not in tabs list
     * range.
     *
     * @param position the index in tabs list
     * @return the corespondent {@link LightningView}, or null if the index is invalid
     */
    @Nullable
    public synchronized LightningView getTabAtPosition(final int position) {
        if (position < 0 || position >= mWebViewList.size()) {
            return null;
        }

        return mWebViewList.get(position);
    }

    /**
     * Try to low memory pressure
     */
    public synchronized void freeMemory() {
        for (LightningView tab : mWebViewList) {
            tab.freeMemory();
        }
    }

    /**
     * Shutdown the manager
     */
    public synchronized void shutdown() {
        for (LightningView tab : mWebViewList) {
            tab.onDestroy();
        }
        mWebViewList.clear();
        mCurrentTab = null;
    }

    /**
     * Resume the tabs
     *
     * @param context
     */
    public synchronized void resume(final Context context) {
        for (LightningView tab : mWebViewList) {
            tab.initializePreferences(context);
        }
    }

    /**
     * Forward network connection status to the webviews.
     *
     * @param isConnected
     */
    public synchronized void notifyConnectionStatus(final boolean isConnected) {
        for (LightningView tab : mWebViewList) {
            final WebView webView = tab.getWebView();
            if (webView != null) {
                webView.setNetworkAvailable(isConnected);
            }
        }
    }

    /**
     * @return The number of currently opened tabs
     */
    public synchronized int size() {
        return mWebViewList.size();
    }

    /**
     * Create and return a new tab. The tab is automatically added to the tabs list.
     *
     * @param activity
     * @param url
     * @param isIncognito
     * @return
     */
    public synchronized LightningView newTab(final Activity activity,
                                             final String url,
                                             final boolean isIncognito) {
        final LightningView tab = new LightningView(activity, url, isIncognito);
        mWebViewList.add(tab);
        return tab;
    }

    /**
     * Remove a tab and return its reference or null if the position is not in tabs range
     *
     * @param position The position of the tab to remove
     */
    private synchronized void removeTab(final int position) {
        if (position >= mWebViewList.size()) {
            return;
        }
        final LightningView tab = mWebViewList.remove(position);
        if (mCurrentTab == tab) {
            mCurrentTab = null;
        }
        tab.onDestroy();
        Log.d(Constants.TAG, tab.toString());
    }

    public synchronized void deleteTab(int position) {
        final LightningView currentTab = getCurrentTab();
        int current = positionOf(currentTab);

        if (current == position) {
            if (size() == 1) {
                mCurrentTab = null;
            } else if (current < size() - 1) {
                // There is another tab after this one
                mCurrentTab = getTabAtPosition(current + 1);
            } else {
                mCurrentTab = getTabAtPosition(current - 1);
            }
            removeTab(current);
        } else {
            removeTab(position);
        }
    }

    /**
     * Return the position of the given tab.
     *
     * @param tab the tab to look for
     * @return the position of the tab or -1 if the tab is not in the list
     */
    public synchronized int positionOf(final LightningView tab) {
        return mWebViewList.indexOf(tab);
    }

    public void saveState() {
        Bundle outState = new Bundle(ClassLoader.getSystemClassLoader());
        Log.d(Constants.TAG, "Saving tab state");
        for (int n = 0; n < mWebViewList.size(); n++) {
            LightningView tab = mWebViewList.get(n);
            Bundle state = new Bundle(ClassLoader.getSystemClassLoader());
            if (tab.getWebView() != null) {
                tab.getWebView().saveState(state);
                outState.putBundle(BUNDLE_KEY + n, state);
            }
        }
        FileUtils.writeBundleToStorage(mApp, outState, BUNDLE_STORAGE);
    }

    private void restoreState(Activity activity) {
        Bundle savedState = FileUtils.readBundleFromStorage(mApp, BUNDLE_STORAGE);
        if (savedState != null) {
            Log.d(Constants.TAG, "Restoring previous WebView state now");
            for (String key : savedState.keySet()) {
                if (key.startsWith(BUNDLE_KEY)) {
                    LightningView tab = newTab(activity, "", false);
                    if (tab.getWebView() != null) {
                        tab.getWebView().restoreState(savedState.getBundle(key));
                    }
                }
            }
        }
    }

    /**
     * Return the {@link WebView} associated to the current tab, or null if there is no current tab
     *
     * @return a {@link WebView} or null
     */
    @Nullable
    public synchronized WebView getCurrentWebView() {
        return mCurrentTab != null ? mCurrentTab.getWebView() : null;
    }

    /**
     * TODO We should remove also this, but probably not
     *
     * @return
     */
    @Nullable
    public synchronized LightningView getCurrentTab() {
        return mCurrentTab;
    }

    /**
     * Switch the current tab to the one at the given position. It returns the selected. After this
     * call {@link TabsManager#getCurrentTab()} return the same reference returned by this method if
     * position is valid.
     *
     * @return the selected tab or null if position is out of tabs range
     */
    @Nullable
    public synchronized LightningView switchToTab(final int position) {
        if (position < 0 || position >= mWebViewList.size()) {
            Log.e(TAG, "Returning a null LightningView requested for position: " + position);
            return null;
        } else {
            final LightningView tab = mWebViewList.get(position);
            if (tab != null) {
                mCurrentTab = tab;
            }
            return tab;
        }
    }

}
