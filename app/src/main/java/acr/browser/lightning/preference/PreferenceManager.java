package acr.browser.lightning.preference;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.download.DownloadHandler;

@Singleton
public class PreferenceManager {

    private static class Name {
        public static final String ADOBE_FLASH_SUPPORT = "enableflash";
        public static final String BLOCK_ADS = "AdBlock";
        public static final String BLOCK_IMAGES = "blockimages";
        public static final String CLEAR_CACHE_EXIT = "cache";
        public static final String COOKIES = "cookies";
        public static final String DOWNLOAD_DIRECTORY = "downloadLocation";
        public static final String FULL_SCREEN = "fullscreen";
        public static final String HIDE_STATUS_BAR = "hidestatus";
        public static final String HOMEPAGE = "home";
        public static final String INCOGNITO_COOKIES = "incognitocookies";
        public static final String JAVASCRIPT = "java";
        public static final String LOCATION = "location";
        public static final String OVERVIEW_MODE = "overviewmode";
        public static final String POPUPS = "newwindows";
        public static final String RESTORE_LOST_TABS = "restoreclosed";
        public static final String SAVE_PASSWORDS = "passwords";
        public static final String SEARCH = "search";
        public static final String SEARCH_URL = "searchurl";
        public static final String TEXT_REFLOW = "textreflow";
        public static final String TEXT_SIZE = "textsize";
        public static final String USE_WIDE_VIEWPORT = "wideviewport";
        public static final String USER_AGENT = "agentchoose";
        public static final String USER_AGENT_STRING = "userAgentString";
        public static final String GOOGLE_SEARCH_SUGGESTIONS = "GoogleSearchSuggestions";
        public static final String CLEAR_HISTORY_EXIT = "clearHistoryExit";
        public static final String CLEAR_COOKIES_EXIT = "clearCookiesExit";
        public static final String SAVE_URL = "saveUrl";
        public static final String RENDERING_MODE = "renderMode";
        public static final String BLOCK_THIRD_PARTY = "thirdParty";
        public static final String ENABLE_COLOR_MODE = "colorMode";
        public static final String URL_BOX_CONTENTS = "urlContent";
        public static final String INVERT_COLORS = "invertColors";
        public static final String READING_TEXT_SIZE = "readingTextSize";
        public static final String THEME = "Theme";
        public static final String TEXT_ENCODING = "textEncoding";
        public static final String CLEAR_WEBSTORAGE_EXIT = "clearWebStorageExit";
        public static final String SHOW_TABS_IN_DRAWER = "showTabsInDrawer";
        public static final String DO_NOT_TRACK = "doNotTrack";
        public static final String IDENTIFYING_HEADERS = "removeIdentifyingHeaders";

        public static final String USE_PROXY = "useProxy";
        public static final String PROXY_CHOICE = "proxyChoice";
        public static final String USE_PROXY_HOST = "useProxyHost";
        public static final String USE_PROXY_PORT = "useProxyPort";
        public static final String INITIAL_CHECK_FOR_TOR = "checkForTor";
        public static final String INITIAL_CHECK_FOR_I2P = "checkForI2P";
    }

    private final SharedPreferences mPrefs;

    private static final String PREFERENCES = "settings";

    @Inject
    PreferenceManager(final Context context) {
        mPrefs = context.getSharedPreferences(PREFERENCES, 0);
    }

    public boolean getAdBlockEnabled() {
        return mPrefs.getBoolean(Name.BLOCK_ADS, false);
    }

    public boolean getBlockImagesEnabled() {
        return mPrefs.getBoolean(Name.BLOCK_IMAGES, false);
    }

    public boolean getBlockThirdPartyCookiesEnabled() {
        return mPrefs.getBoolean(Name.BLOCK_THIRD_PARTY, false);
    }

    public boolean getCheckedForTor() {
        return mPrefs.getBoolean(Name.INITIAL_CHECK_FOR_TOR, false);
    }

    public boolean getCheckedForI2P() {
        return mPrefs.getBoolean(Name.INITIAL_CHECK_FOR_I2P, false);
    }

    public boolean getClearCacheExit() {
        return mPrefs.getBoolean(Name.CLEAR_CACHE_EXIT, false);
    }

    public boolean getClearCookiesExitEnabled() {
        return mPrefs.getBoolean(Name.CLEAR_COOKIES_EXIT, false);
    }

    public boolean getClearWebStorageExitEnabled() {
        return mPrefs.getBoolean(Name.CLEAR_WEBSTORAGE_EXIT, false);
    }

    public boolean getClearHistoryExitEnabled() {
        return mPrefs.getBoolean(Name.CLEAR_HISTORY_EXIT, false);
    }

    public boolean getColorModeEnabled() {
        return mPrefs.getBoolean(Name.ENABLE_COLOR_MODE, false);
    }

    public boolean getCookiesEnabled() {
        return mPrefs.getBoolean(Name.COOKIES, true);
    }

    public String getDownloadDirectory() {
        return mPrefs.getString(Name.DOWNLOAD_DIRECTORY, DownloadHandler.DEFAULT_DOWNLOAD_PATH);
    }

    public int getFlashSupport() {
        return mPrefs.getInt(Name.ADOBE_FLASH_SUPPORT, 0);
    }

    public boolean getFullScreenEnabled() {
        return mPrefs.getBoolean(Name.FULL_SCREEN, false);
    }

    public boolean getGoogleSearchSuggestionsEnabled() {
        return mPrefs.getBoolean(Name.GOOGLE_SEARCH_SUGGESTIONS, true);
    }

    public boolean getHideStatusBarEnabled() {
        return mPrefs.getBoolean(Name.HIDE_STATUS_BAR, false);
    }

    public String getHomepage() {
        return mPrefs.getString(Name.HOMEPAGE, Constants.HOMEPAGE);
    }

    public boolean getIncognitoCookiesEnabled() {
        return mPrefs.getBoolean(Name.INCOGNITO_COOKIES, false);
    }

    public boolean getInvertColors() {
        return mPrefs.getBoolean(Name.INVERT_COLORS, false);
    }

    public boolean getJavaScriptEnabled() {
        return mPrefs.getBoolean(Name.JAVASCRIPT, true);
    }

    public boolean getLocationEnabled() {
        return mPrefs.getBoolean(Name.LOCATION, false);
    }

    public boolean getOverviewModeEnabled() {
        return mPrefs.getBoolean(Name.OVERVIEW_MODE, true);
    }

    public boolean getPopupsEnabled() {
        return mPrefs.getBoolean(Name.POPUPS, true);
    }

    public String getProxyHost() {
        return mPrefs.getString(Name.USE_PROXY_HOST, "localhost");
    }

    public int getProxyPort() {
        return mPrefs.getInt(Name.USE_PROXY_PORT, 8118);
    }

    public int getReadingTextSize() {
        return mPrefs.getInt(Name.READING_TEXT_SIZE, 2);
    }

    public int getRenderingMode() {
        return mPrefs.getInt(Name.RENDERING_MODE, 0);
    }

    public boolean getRestoreLostTabsEnabled() {
        return mPrefs.getBoolean(Name.RESTORE_LOST_TABS, true);
    }

    public String getSavedUrl() {
        return mPrefs.getString(Name.SAVE_URL, null);
    }

    public boolean getSavePasswordsEnabled() {
        return mPrefs.getBoolean(Name.SAVE_PASSWORDS, true);
    }

    public int getSearchChoice() {
        return mPrefs.getInt(Name.SEARCH, 1);
    }

    public String getSearchUrl() {
        return mPrefs.getString(Name.SEARCH_URL, Constants.GOOGLE_SEARCH);
    }

    public boolean getTextReflowEnabled() {
        return mPrefs.getBoolean(Name.TEXT_REFLOW, false);
    }

    public int getTextSize() {
        return mPrefs.getInt(Name.TEXT_SIZE, 3);
    }

    public int getUrlBoxContentChoice() {
        return mPrefs.getInt(Name.URL_BOX_CONTENTS, 0);
    }

    public int getUseTheme() {
        return mPrefs.getInt(Name.THEME, 0);
    }

    public boolean getUseProxy() {
        return mPrefs.getBoolean(Name.USE_PROXY, false);
    }

    public int getProxyChoice() {
        return mPrefs.getInt(Name.PROXY_CHOICE, Constants.NO_PROXY);
    }

    public int getUserAgentChoice() {
        return mPrefs.getInt(Name.USER_AGENT, 1);
    }

    public String getUserAgentString(String def) {
        return mPrefs.getString(Name.USER_AGENT_STRING, def);
    }

    public boolean getUseWideViewportEnabled() {
        return mPrefs.getBoolean(Name.USE_WIDE_VIEWPORT, true);
    }

    public String getTextEncoding() {
        return mPrefs.getString(Name.TEXT_ENCODING, Constants.DEFAULT_ENCODING);
    }

    public boolean getShowTabsInDrawer(boolean defaultValue) {
        return mPrefs.getBoolean(Name.SHOW_TABS_IN_DRAWER, defaultValue);
    }

    public boolean getDoNotTrackEnabled() {
        return mPrefs.getBoolean(Name.DO_NOT_TRACK, false);
    }

    public boolean getRemoveIdentifyingHeadersEnabled(){
        return mPrefs.getBoolean(Name.IDENTIFYING_HEADERS, false);
    }

    private void putBoolean(String name, boolean value) {
        mPrefs.edit().putBoolean(name, value).apply();
    }

    private void putInt(String name, int value) {
        mPrefs.edit().putInt(name, value).apply();
    }

    private void putString(String name, String value) {
        mPrefs.edit().putString(name, value).apply();
    }

    public void setRemoveIdentifyingHeadersEnabled(boolean enabled){
        putBoolean(Name.IDENTIFYING_HEADERS, enabled);
    }

    public void setDoNotTrackEnabled(boolean doNotTrack) {
        putBoolean(Name.DO_NOT_TRACK, doNotTrack);
    }

    public void setShowTabsInDrawer(boolean show) {
        putBoolean(Name.SHOW_TABS_IN_DRAWER, show);
    }

    public void setTextEncoding(String encoding) {
        putString(Name.TEXT_ENCODING, encoding);
    }

    public void setAdBlockEnabled(boolean enable) {
        putBoolean(Name.BLOCK_ADS, enable);
    }

    public void setBlockImagesEnabled(boolean enable) {
        putBoolean(Name.BLOCK_IMAGES, enable);
    }

    public void setBlockThirdPartyCookiesEnabled(boolean enable) {
        putBoolean(Name.BLOCK_THIRD_PARTY, enable);
    }

    public void setCheckedForTor(boolean check) {
        putBoolean(Name.INITIAL_CHECK_FOR_TOR, check);
    }

    public void setCheckedForI2P(boolean check) {
        putBoolean(Name.INITIAL_CHECK_FOR_I2P, check);
    }

    public void setClearCacheExit(boolean enable) {
        putBoolean(Name.CLEAR_CACHE_EXIT, enable);
    }

    public void setClearCookiesExitEnabled(boolean enable) {
        putBoolean(Name.CLEAR_COOKIES_EXIT, enable);
    }

    public void setClearWebStorageExitEnabled(boolean enable) {
        putBoolean(Name.CLEAR_WEBSTORAGE_EXIT, enable);
    }

    public void setClearHistoryExitEnabled(boolean enable) {
        putBoolean(Name.CLEAR_HISTORY_EXIT, enable);
    }

    public void setColorModeEnabled(boolean enable) {
        putBoolean(Name.ENABLE_COLOR_MODE, enable);
    }

    public void setCookiesEnabled(boolean enable) {
        putBoolean(Name.COOKIES, enable);
    }

    public void setDownloadDirectory(String directory) {
        putString(Name.DOWNLOAD_DIRECTORY, directory);
    }

    public void setFlashSupport(int n) {
        putInt(Name.ADOBE_FLASH_SUPPORT, n);
    }

    public void setFullScreenEnabled(boolean enable) {
        putBoolean(Name.FULL_SCREEN, enable);
    }

    public void setGoogleSearchSuggestionsEnabled(boolean enabled) {
        putBoolean(Name.GOOGLE_SEARCH_SUGGESTIONS, enabled);
    }

    public void setHideStatusBarEnabled(boolean enable) {
        putBoolean(Name.HIDE_STATUS_BAR, enable);
    }

    public void setHomepage(String homepage) {
        putString(Name.HOMEPAGE, homepage);
    }

    public void setIncognitoCookiesEnabled(boolean enable) {
        putBoolean(Name.INCOGNITO_COOKIES, enable);
    }

    public void setInvertColors(boolean enable) {
        putBoolean(Name.INVERT_COLORS, enable);
    }

    public void setJavaScriptEnabled(boolean enable) {
        putBoolean(Name.JAVASCRIPT, enable);
    }

    public void setLocationEnabled(boolean enable) {
        putBoolean(Name.LOCATION, enable);
    }

    public void setOverviewModeEnabled(boolean enable) {
        putBoolean(Name.OVERVIEW_MODE, enable);
    }

    public void setPopupsEnabled(boolean enable) {
        putBoolean(Name.POPUPS, enable);
    }

    public void setReadingTextSize(int size) {
        putInt(Name.READING_TEXT_SIZE, size);
    }

    public void setRenderingMode(int mode) {
        putInt(Name.RENDERING_MODE, mode);
    }

    public void setRestoreLostTabsEnabled(boolean enable) {
        putBoolean(Name.RESTORE_LOST_TABS, enable);
    }

    public void setSavedUrl(String url) {
        putString(Name.SAVE_URL, url);
    }

    public void setSavePasswordsEnabled(boolean enable) {
        putBoolean(Name.SAVE_PASSWORDS, enable);
    }

    public void setSearchChoice(int choice) {
        putInt(Name.SEARCH, choice);
    }

    public void setSearchUrl(String url) {
        putString(Name.SEARCH_URL, url);
    }

    public void setTextReflowEnabled(boolean enable) {
        putBoolean(Name.TEXT_REFLOW, enable);
    }

    public void setTextSize(int size) {
        putInt(Name.TEXT_SIZE, size);
    }

    public void setUrlBoxContentChoice(int choice) {
        putInt(Name.URL_BOX_CONTENTS, choice);
    }

    public void setUseTheme(int theme) {
        putInt(Name.THEME, theme);
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
    public void setProxyChoice(int choice) {
        putBoolean(Name.USE_PROXY, choice != Constants.NO_PROXY);
        putInt(Name.PROXY_CHOICE, choice);
    }

    public void setProxyHost(String proxyHost) {
        putString(Name.USE_PROXY_HOST, proxyHost);
    }

    public void setProxyPort(int proxyPort) {
        putInt(Name.USE_PROXY_PORT, proxyPort);
    }

    public void setUserAgentChoice(int choice) {
        putInt(Name.USER_AGENT, choice);
    }

    public void setUserAgentString(String agent) {
        putString(Name.USER_AGENT_STRING, agent);
    }

    public void setUseWideViewportEnabled(boolean enable) {
        putBoolean(Name.USE_WIDE_VIEWPORT, enable);
    }
}
