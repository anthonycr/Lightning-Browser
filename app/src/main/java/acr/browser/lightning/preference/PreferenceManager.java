package acr.browser.lightning.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.constant.Proxy;

@Singleton
public class PreferenceManager {

     static final class Name {
























        static final String SHOW_TABS_IN_DRAWER = "showTabsInDrawer";
        static final String DO_NOT_TRACK = "doNotTrack";
        static final String IDENTIFYING_HEADERS = "removeIdentifyingHeaders";
        static final String SWAP_BOOKMARKS_AND_TABS = "swapBookmarksAndTabs";
        static final String SEARCH_SUGGESTIONS = "searchSuggestions";
        static final String BLACK_STATUS_BAR = "blackStatusBar";

        static final String USE_PROXY = "useProxy";
        static final String PROXY_CHOICE = "proxyChoice";
        static final String USE_PROXY_HOST = "useProxyHost";
        static final String USE_PROXY_PORT = "useProxyPort";
        static final String INITIAL_CHECK_FOR_TOR = "checkForTor";
        static final String INITIAL_CHECK_FOR_I2P = "checkForI2P";

        static final String LEAK_CANARY = "leakCanary";
    }

    public enum Suggestion {
        SUGGESTION_GOOGLE,
        SUGGESTION_DUCK,
        SUGGESTION_BAIDU,
        SUGGESTION_NONE
    }

    @NonNull private final SharedPreferences mPrefs;

    private static final String PREFERENCES = "settings";

    @Inject
    PreferenceManager(@NonNull final Context context) {
        mPrefs = context.getSharedPreferences(PREFERENCES, 0);
    }

    @NonNull
    public Suggestion getSearchSuggestionChoice() {
        try {
            return Suggestion.valueOf(mPrefs.getString(Name.SEARCH_SUGGESTIONS, Suggestion.SUGGESTION_GOOGLE.name()));
        } catch (IllegalArgumentException ignored) {
            return Suggestion.SUGGESTION_NONE;
        }
    }

    public void setSearchSuggestionChoice(@NonNull Suggestion suggestion) {
        putString(Name.SEARCH_SUGGESTIONS, suggestion.name());
    }

    public boolean getBookmarksAndTabsSwapped() {
        return mPrefs.getBoolean(Name.SWAP_BOOKMARKS_AND_TABS, false);
    }

    public void setBookmarkAndTabsSwapped(boolean swap) {
        putBoolean(Name.SWAP_BOOKMARKS_AND_TABS, swap);
    }

    public boolean getCheckedForTor() {
        return mPrefs.getBoolean(Name.INITIAL_CHECK_FOR_TOR, false);
    }

    public boolean getCheckedForI2P() {
        return mPrefs.getBoolean(Name.INITIAL_CHECK_FOR_I2P, false);
    }

    @NonNull
    public String getProxyHost() {
        return mPrefs.getString(Name.USE_PROXY_HOST, "localhost");
    }

    public int getProxyPort() {
        return mPrefs.getInt(Name.USE_PROXY_PORT, 8118);
    }

    public boolean getUseProxy() {
        return mPrefs.getBoolean(Name.USE_PROXY, false);
    }

    @Proxy
    public int getProxyChoice() {
        @Proxy int proxy = mPrefs.getInt(Name.PROXY_CHOICE, Constants.NO_PROXY);
        switch (proxy) {
            case Constants.NO_PROXY:
            case Constants.PROXY_ORBOT:
            case Constants.PROXY_I2P:
            case Constants.PROXY_MANUAL:
                return proxy;
            default:
                return Constants.NO_PROXY;
        }
    }

    public boolean getShowTabsInDrawer(boolean defaultValue) {
        return mPrefs.getBoolean(Name.SHOW_TABS_IN_DRAWER, defaultValue);
    }

    public boolean getDoNotTrackEnabled() {
        return mPrefs.getBoolean(Name.DO_NOT_TRACK, false);
    }

    public boolean getRemoveIdentifyingHeadersEnabled() {
        return mPrefs.getBoolean(Name.IDENTIFYING_HEADERS, false);
    }

    public boolean getUseBlackStatusBar() {
        return mPrefs.getBoolean(Name.BLACK_STATUS_BAR, false);
    }

    private void putBoolean(@NonNull String name, boolean value) {
        mPrefs.edit().putBoolean(name, value).apply();
    }

    private void putInt(@NonNull String name, int value) {
        mPrefs.edit().putInt(name, value).apply();
    }

    private void putString(@NonNull String name, @Nullable String value) {
        mPrefs.edit().putString(name, value).apply();
    }

    public void setUseBlackStatusBar(boolean enabled) {
        putBoolean(Name.BLACK_STATUS_BAR, enabled);
    }

    public void setRemoveIdentifyingHeadersEnabled(boolean enabled) {
        putBoolean(Name.IDENTIFYING_HEADERS, enabled);
    }

    public void setDoNotTrackEnabled(boolean doNotTrack) {
        putBoolean(Name.DO_NOT_TRACK, doNotTrack);
    }

    public void setShowTabsInDrawer(boolean show) {
        putBoolean(Name.SHOW_TABS_IN_DRAWER, show);
    }

    public void setCheckedForTor(boolean check) {
        putBoolean(Name.INITIAL_CHECK_FOR_TOR, check);
    }

    public void setCheckedForI2P(boolean check) {
        putBoolean(Name.INITIAL_CHECK_FOR_I2P, check);
    }

    public void setUseLeakCanary(boolean useLeakCanary) {
        putBoolean(Name.LEAK_CANARY, useLeakCanary);
    }

    public boolean getUseLeakCanary() {
        return mPrefs.getBoolean(Name.LEAK_CANARY, false);
    }

    /**
     * Valid choices:
     * <ul>
     * <li>{@link Constants#NO_PROXY}</li>
     * <li>{@link Constants#PROXY_ORBOT}</li>
     * <li>{@link Constants#PROXY_I2P}</li>
     * </ul>
     *
     * @param choice the proxy to use.
     */
    public void setProxyChoice(@Proxy int choice) {
        putBoolean(Name.USE_PROXY, choice != Constants.NO_PROXY);
        putInt(Name.PROXY_CHOICE, choice);
    }

    public void setProxyHost(@NonNull String proxyHost) {
        putString(Name.USE_PROXY_HOST, proxyHost);
    }

    public void setProxyPort(int proxyPort) {
        putInt(Name.USE_PROXY_PORT, proxyPort);
    }

}
