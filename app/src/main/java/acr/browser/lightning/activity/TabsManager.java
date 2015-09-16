package acr.browser.lightning.activity;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;
import android.webkit.WebView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import acr.browser.lightning.controller.BrowserController;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.view.LightningView;

/**
 * @author Stefano Pacifici
 * @date 2015/09/14
 */
@Singleton
public class TabsManager {

    private final List<LightningView> mWebViewList = new ArrayList<>();
    private LightningView mCurrentTab;

    @Inject
    public TabsManager(final Context context, final PreferenceManager preferenceManager) {
    }

    /**
     * Return a clone of the current tabs list. The list will not be updated, the user has to fetch
     * a new copy when notified.
     *
     * @return  a copy of the current tabs list
     */
    public List<LightningView> getTabsList() {
        return new ArrayList<>(mWebViewList);
    }

    /**
     * Return the tab at the given position in tabs list, or null if position is not in tabs list
     * range.
     *
     * @param position  the index in tabs list
     * @return  the corespondent {@link LightningView}, or null if the index is invalid
     */
    @Nullable
    public LightningView getTabAtPosition(final int position) {
        if (position < 0 || position >= mWebViewList.size()) {
            return null;
        }

        return mWebViewList.get(position);
    }

    /**
     * Try to low memory pressure
     */
    public void freeMemory() {
        for (LightningView tab: mWebViewList) {
            tab.freeMemory();
        }
    }

    /**
     * Shutdown the manager
     */
    public synchronized void shutdown() {
        for (LightningView tab: mWebViewList) {
            tab.onDestroy();
        }
        mWebViewList.clear();
    }

    /**
     * Resume the tabs
     *
     * @param context
     */
    public synchronized void resume(final Context context) {
        for (LightningView tab: mWebViewList) {
            tab.initializePreferences(null, context);
        }
    }

    /**
     * Forward network connection status to the webviews.
     *
     * @param isConnected
     */
    public synchronized void notifyConnectioneStatus(final boolean isConnected) {
        for (LightningView tab: mWebViewList) {
            final WebView webView = tab.getWebView();
            if (webView != null) {
                webView.setNetworkAvailable(isConnected);
            }
        }
    }
    /**
     * @return  The number of currently opened tabs
     */
    public int size() {
        return mWebViewList.size();
    }

    /**
     * Create and return a new tab. The tab is automatically added to the tabs list.
     *
     * @param activity
     * @param url
     * @param darkTheme
     * @param isIncognito
     * @param controller
     * @return
     */
    public synchronized LightningView newTab(final Activity activity,
                                             final String url, final boolean darkTheme,
                                             final boolean isIncognito,
                                             final BrowserController controller) {
        final LightningView tab = new LightningView(activity, url, darkTheme, isIncognito, controller);
        mWebViewList.add(tab);
        return tab;
    }

    /**
     * Remove a tab and return its reference or null if the position is not in tabs range
     *
     * @param position  The position of the tab to remove
     * @return  The removed tab reference or null
     */
    @Nullable
    public synchronized LightningView deleteTab(final int position) {
        if (position >= mWebViewList.size()) {
            return null;
        }
        final LightningView tab = mWebViewList.remove(position);
        // TODO This should not be done outside this call
        // tab.onDestroy();
        return tab;
    }

    /**
     * TODO I think it should be removed
     * Return the position of the given tab
     * @param tab
     * @return
     */
    public int positionOf(final LightningView tab) {
        return mWebViewList.indexOf(tab);
    }

    /**
     * @return  A string representation of the currently opened tabs
     */
    public String tabsString() {
        final StringBuilder builder = new StringBuilder();
        for (LightningView tab: mWebViewList) {
            final String url = tab.getUrl();
            if (url != null && !url.isEmpty()) {
                builder.append(url).append("|$|SEPARATOR|$|");
            }
        }
        return builder.toString();
    }

    /**
     * TODO Remove this, temporary workaround
     * @param tab
     * @return
     */
    public int getPositionForTab(final LightningView tab) {
        return mWebViewList.indexOf(tab);
    }

    /**
     * Return the {@link WebView} associated to the current tab, or null if there is no current tab
     * @return a {@link WebView} or null
     */
    @Nullable
    public WebView getCurrentWebView() {
        return mCurrentTab != null ? mCurrentTab.getWebView() : null;
    }

    /**
     * TODO We should remove also this, but probably not
     * @return
     */
    public LightningView getCurrentTab() {
        return mCurrentTab;
    }

    /**
     * Switch the current tab to the one at the given position. It returns the selected. After this
     * call {@link TabsManager#getCurrentTab()} return the same reference returned by this method if
     * position is valid.
     *
     * @return  the selected tab or null if position is out of tabs range
     */
    @Nullable
    public LightningView switchToTab(final int position) {
        if (position < 0 || position >= mWebViewList.size()) {
            return null;
        } else {
            final LightningView tab = mWebViewList.get(position);
            mCurrentTab = tab;
            return tab;
        }
    }
}
