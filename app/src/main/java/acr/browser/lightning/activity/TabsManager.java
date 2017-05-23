package acr.browser.lightning.activity;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.webkit.WebView;

import com.squareup.otto.Bus;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.constant.BookmarkPage;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.constant.HistoryPage;
import acr.browser.lightning.constant.StartPage;
import acr.browser.lightning.database.BookmarkManager;
import acr.browser.lightning.database.HistoryDatabase;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.react.Action;
import acr.browser.lightning.react.Observable;
import acr.browser.lightning.react.OnSubscribe;
import acr.browser.lightning.react.Schedulers;
import acr.browser.lightning.react.Subscriber;
import acr.browser.lightning.utils.FileUtils;
import acr.browser.lightning.utils.UrlUtils;
import acr.browser.lightning.view.LightningView;

/**
 * A manager singleton that holds all the {@link LightningView}
 * and tracks the current tab. It handles creation, deletion,
 * restoration, state saving, and switching of tabs.
 */
public class TabsManager {

    private static final String TAG = TabsManager.class.getSimpleName();
    private static final String BUNDLE_KEY = "WEBVIEW_";
    private static final String URL_KEY = "URL_KEY";
    private static final String BUNDLE_STORAGE = "SAVED_TABS.parcel";

    private final List<LightningView> mTabList = new ArrayList<>(1);
    @Nullable private LightningView mCurrentTab;
    @Nullable private TabNumberChangedListener mTabNumberListener;

    private boolean mIsInitialized = false;
    private final List<Runnable> mPostInitializationWorkList = new ArrayList<>();

    @Inject PreferenceManager mPreferenceManager;
    @Inject BookmarkManager mBookmarkManager;
    @Inject HistoryDatabase mHistoryManager;
    @Inject Bus mEventBus;
    @Inject Application mApp;

    public TabsManager() {
        BrowserApp.getAppComponent().inject(this);
    }

    // TODO remove and make presenter call new tab methods so it always knows
    @Deprecated
    public interface TabNumberChangedListener {
        void tabNumberChanged(int newNumber);
    }

    public void setTabNumberChangedListener(@Nullable TabNumberChangedListener listener) {
        mTabNumberListener = listener;
    }

    public void cancelPendingWork() {
        mPostInitializationWorkList.clear();
    }

    public void doAfterInitialization(@NonNull Runnable runnable) {
        if (mIsInitialized) {
            runnable.run();
        } else {
            mPostInitializationWorkList.add(runnable);
        }
    }

    private void finishInitialization() {
        mIsInitialized = true;
        for (Runnable runnable : mPostInitializationWorkList) {
            runnable.run();
        }
    }

    /**
     * Restores old tabs that were open before the browser
     * was closed. Handles the intent used to open the browser.
     *
     * @param activity  the activity needed to create tabs.
     * @param intent    the intent that started the browser activity.
     * @param incognito whether or not we are in incognito mode.
     */
    public synchronized Observable<Void> initializeTabs(@NonNull final Activity activity,
                                                        @Nullable final Intent intent,
                                                        final boolean incognito) {
        return Observable.create(new Action<Void>() {
            @Override
            public void onSubscribe(@NonNull final Subscriber<Void> subscriber) {

                // Make sure we start with a clean tab list
                shutdown();

                // If incognito, only create one tab, do not handle intent
                // in order to protect user privacy
                if (incognito) {
                    newTab(activity, null, true);
                    subscriber.onComplete();
                    return;
                }

                String url = null;
                if (intent != null) {
                    url = intent.getDataString();
                }
                Log.d(TAG, "URL from intent: " + url);
                mCurrentTab = null;
                if (mPreferenceManager.getRestoreLostTabsEnabled()) {
                    restoreLostTabs(url, activity, subscriber);
                } else {
                    newTab(activity, null, false);
                    finishInitialization();
                    subscriber.onComplete();
                }

            }
        });

    }

    private void restoreLostTabs(@Nullable final String url, @NonNull final Activity activity,
                                 @NonNull final Subscriber subscriber) {

        restoreState().subscribeOn(Schedulers.io())
                .observeOn(Schedulers.main()).subscribe(new OnSubscribe<Bundle>() {
            @Override
            public void onNext(Bundle item) {
                LightningView tab = newTab(activity, "", false);
                String url = item.getString(URL_KEY);
                if (url != null && tab.getWebView() != null) {
                    if (UrlUtils.isBookmarkUrl(url)) {
                        new BookmarkPage(tab, activity, mBookmarkManager).load();
                    } else if (UrlUtils.isStartPageUrl(url)) {
                        new StartPage(tab, mApp).load();
                    } else if (UrlUtils.isHistoryUrl(url)) {
                        new HistoryPage(tab, mApp, mHistoryManager).load();
                    }
                } else if (tab.getWebView() != null) {
                    tab.getWebView().restoreState(item);
                }
            }

            @Override
            public void onComplete() {
                if (url != null) {
                    if (url.startsWith(Constants.FILE)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setCancelable(true)
                                .setTitle(R.string.title_warning)
                                .setMessage(R.string.message_blocked_local)
                                .setNegativeButton(android.R.string.cancel, null)
                                .setPositiveButton(R.string.action_open, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        newTab(activity, url, false);
                                    }
                                }).show();
                    } else {
                        newTab(activity, url, false);
                    }
                }
                if (mTabList.size() == 0) {
                    newTab(activity, null, false);
                }
                finishInitialization();
                subscriber.onComplete();
            }
        });
    }

    /**
     * Method used to resume all the tabs in the browser.
     * This is necessary because we cannot pause the
     * WebView when the app is open currently due to a
     * bug in the WebView, where calling onResume doesn't
     * consistently resume it.
     */
    public void resumeAll() {
        LightningView current = getCurrentTab();
        if (current != null) {
            current.resumeTimers();
        }
        for (LightningView tab : mTabList) {
            if (tab != null) {
                tab.onResume();
            }
        }
    }

    /**
     * Method used to pause all the tabs in the browser.
     * This is necessary because we cannot pause the
     * WebView when the app is open currently due to a
     * bug in the WebView, where calling onResume doesn't
     * consistently resume it.
     */
    public void pauseAll() {
        LightningView current = getCurrentTab();
        if (current != null) {
            current.pauseTimers();
        }
        for (LightningView tab : mTabList) {
            if (tab != null) {
                tab.onPause();
            }
        }
    }

    /**
     * Return the tab at the given position in tabs list, or
     * null if position is not in tabs list range.
     *
     * @param position the index in tabs list
     * @return the corespondent {@link LightningView},
     * or null if the index is invalid
     */
    @Nullable
    public synchronized LightningView getTabAtPosition(final int position) {
        if (position < 0 || position >= mTabList.size()) {
            return null;
        }

        return mTabList.get(position);
    }

    /**
     * Frees memory for each tab in the
     * manager. Note: this will only work
     * on API < KITKAT as on KITKAT onward
     * the WebViews manage their own
     * memory correctly.
     */
    public synchronized void freeMemory() {
        for (LightningView tab : mTabList) {
            //noinspection deprecation
            tab.freeMemory();
        }
    }

    /**
     * Shutdown the manager. This destroys
     * all tabs and clears the references
     * to those tabs. Current tab is also
     * released for garbage collection.
     */
    public synchronized void shutdown() {
        for (LightningView tab : mTabList) {
            tab.onDestroy();
        }
        mTabList.clear();
        mIsInitialized = false;
        mCurrentTab = null;
    }

    /**
     * Reinitializes the preferences for
     * all the tabs in the list.
     *
     * @param context the context needed
     *                to initialize the preferences.
     */
    public synchronized void resume(@NonNull final Context context) {
        for (LightningView tab : mTabList) {
            tab.initializePreferences(context);
        }
    }

    /**
     * Forwards network connection status to the WebViews.
     *
     * @param isConnected whether there is a network
     *                    connection or not.
     */
    public synchronized void notifyConnectionStatus(final boolean isConnected) {
        for (LightningView tab : mTabList) {
            final WebView webView = tab.getWebView();
            if (webView != null) {
                webView.setNetworkAvailable(isConnected);
            }
        }
    }

    /**
     * The current number of tabs in the manager.
     *
     * @return the number of tabs in the list.
     */
    public synchronized int size() {
        return mTabList.size();
    }

    /**
     * The index of the last tab in the manager.
     *
     * @return the last tab in the list or -1 if there are no tabs.
     */
    public synchronized int last() {
        return mTabList.size() - 1;
    }


    /**
     * The last tab in the tab manager.
     *
     * @return the last tab, or null if
     * there are no tabs.
     */
    @Nullable
    public synchronized LightningView lastTab() {
        if (last() < 0) {
            return null;
        }
        return mTabList.get(last());
    }

    /**
     * Create and return a new tab. The tab is
     * automatically added to the tabs list.
     *
     * @param activity    the activity needed to create the tab.
     * @param url         the URL to initialize the tab with.
     * @param isIncognito whether the tab is an incognito
     *                    tab or not.
     * @return a valid initialized tab.
     */
    @NonNull
    public synchronized LightningView newTab(@NonNull final Activity activity,
                                             @Nullable final String url,
                                             final boolean isIncognito) {
        Log.d(TAG, "New tab");
        final LightningView tab = new LightningView(activity, url, isIncognito);
        mTabList.add(tab);
        if (mTabNumberListener != null) {
            mTabNumberListener.tabNumberChanged(size());
        }
        return tab;
    }

    /**
     * Removes a tab from the list and destroys the tab.
     * If the tab removed is the current tab, the reference
     * to the current tab will be nullified.
     *
     * @param position The position of the tab to remove.
     */
    private synchronized void removeTab(final int position) {
        if (position >= mTabList.size()) {
            return;
        }
        final LightningView tab = mTabList.remove(position);
        if (mCurrentTab == tab) {
            mCurrentTab = null;
        }
        tab.onDestroy();
    }

    /**
     * Deletes a tab from the manager. If the tab
     * being deleted is the current tab, this method
     * will switch the current tab to a new valid tab.
     *
     * @param position the position of the tab to delete.
     * @return returns true if the current tab
     * was deleted, false otherwise.
     */
    public synchronized boolean deleteTab(int position) {
        Log.d(TAG, "Delete tab: " + position);
        final LightningView currentTab = getCurrentTab();
        int current = positionOf(currentTab);

        if (current == position) {
            if (size() == 1) {
                mCurrentTab = null;
            } else if (current < size() - 1) {
                // There is another tab after this one
                switchToTab(current + 1);
            } else {
                switchToTab(current - 1);
            }
        }

        removeTab(position);
        if (mTabNumberListener != null) {
            mTabNumberListener.tabNumberChanged(size());
        }
        return current == position;
    }

    /**
     * Return the position of the given tab.
     *
     * @param tab the tab to look for.
     * @return the position of the tab or -1
     * if the tab is not in the list.
     */
    public synchronized int positionOf(final LightningView tab) {
        return mTabList.indexOf(tab);
    }

    /**
     * Saves the state of the current WebViews,
     * to a bundle which is then stored in persistent
     * storage and can be unparceled.
     */
    public void saveState() {
        Bundle outState = new Bundle(ClassLoader.getSystemClassLoader());
        Log.d(Constants.TAG, "Saving tab state");
        for (int n = 0; n < mTabList.size(); n++) {
            LightningView tab = mTabList.get(n);
            Bundle state = new Bundle(ClassLoader.getSystemClassLoader());
            if (tab.getWebView() != null && !UrlUtils.isSpecialUrl(tab.getUrl())) {
                tab.getWebView().saveState(state);
                outState.putBundle(BUNDLE_KEY + n, state);
            } else if (tab.getWebView() != null) {
                state.putString(URL_KEY, tab.getUrl());
                outState.putBundle(BUNDLE_KEY + n, state);
            }
        }
        FileUtils.writeBundleToStorage(mApp, outState, BUNDLE_STORAGE);
    }

    /**
     * Use this method to clear the saved
     * state if you do not wish it to be
     * restored when the browser next starts.
     */
    public void clearSavedState() {
        FileUtils.deleteBundleInStorage(mApp, BUNDLE_STORAGE);
    }

    /**
     * Restores the previously saved tabs from the
     * bundle stored in peristent file storage.
     * It will create new tabs for each tab saved
     * and will delete the saved instance file when
     * restoration is complete.
     */
    private Observable<Bundle> restoreState() {
        return Observable.create(new Action<Bundle>() {
            @Override
            public void onSubscribe(@NonNull Subscriber<Bundle> subscriber) {
                Bundle savedState = FileUtils.readBundleFromStorage(mApp, BUNDLE_STORAGE);
                if (savedState != null) {
                    Log.d(Constants.TAG, "Restoring previous WebView state now");
                    for (String key : savedState.keySet()) {
                        if (key.startsWith(BUNDLE_KEY)) {
                            subscriber.onNext(savedState.getBundle(key));
                        }
                    }
                }
                FileUtils.deleteBundleInStorage(mApp, BUNDLE_STORAGE);
                subscriber.onComplete();
            }
        });
    }

    /**
     * Return the {@link WebView} associated to the current tab,
     * or null if there is no current tab.
     *
     * @return a {@link WebView} or null if there is no current tab.
     */
    @Nullable
    public synchronized WebView getCurrentWebView() {
        return mCurrentTab != null ? mCurrentTab.getWebView() : null;
    }

    /**
     * Returns the index of the current tab.
     *
     * @return Return the index of the current tab, or -1 if the
     * current tab is null.
     */
    public int indexOfCurrentTab() {
        return mTabList.indexOf(mCurrentTab);
    }

    /**
     * Returns the index of the tab.
     *
     * @return Return the index of the tab, or -1 if the tab isn't in the list.
     */
    public int indexOfTab(LightningView tab) {
        return mTabList.indexOf(tab);
    }

    /**
     * Return the current {@link LightningView} or null if
     * no current tab has been set.
     *
     * @return a {@link LightningView} or null if there
     * is no current tab.
     */
    @Nullable
    public synchronized LightningView getCurrentTab() {
        return mCurrentTab;
    }

    /**
     * Switch the current tab to the one at the given position.
     * It returns the selected tab that has been switced to.
     *
     * @return the selected tab or null if position is out of tabs range.
     */
    @Nullable
    public synchronized LightningView switchToTab(final int position) {
        Log.d(TAG, "switch to tab: " + position);
        if (position < 0 || position >= mTabList.size()) {
            Log.e(TAG, "Returning a null LightningView requested for position: " + position);
            return null;
        } else {
            final LightningView tab = mTabList.get(position);
            if (tab != null) {
                mCurrentTab = tab;
            }
            return tab;
        }
    }

}
