package acr.browser.lightning;

import android.content.SharedPreferences;
import android.os.Environment;

public class Preferences {

	private static Preferences mInstance;
	private static SharedPreferences mPrefs;
	private static final String PREFERENCES = "settings";
	
	private Preferences(){
		mPrefs = BrowserApp.getAppContext().getSharedPreferences(PREFERENCES, 0);
	}
	
	public static Preferences getInstance(){
		if(mInstance == null){
			mInstance = new Preferences();
		}
		return mInstance;
	}
	
	public int getFlashSupport(){
		return mPrefs.getInt(Name.ADOBE_FLASH_SUPPORT, 0);
	}
	
	public void setFlashSupport(int n){
		mPrefs.edit().putInt(Name.ADOBE_FLASH_SUPPORT, n).apply();
	}
	
	public boolean getAdBlockEnabled(){
		return mPrefs.getBoolean(Name.BLOCK_ADS, false);
	}
	
	public void setAdBlockEnabled(boolean enable){
		mPrefs.edit().putBoolean(Name.BLOCK_ADS, enable).apply();
	}
	
	public boolean getBlockImagesEnabled(){
		return mPrefs.getBoolean(Name.BLOCK_IMAGES, false);
	}
	
	public void setBlockImagesEnabled(boolean enable){
		mPrefs.edit().putBoolean(Name.BLOCK_IMAGES, enable).apply();
	}
	
	public boolean getClearCacheExit(){
		return mPrefs.getBoolean(Name.CLEAR_CACHE_EXIT, false);
	}
	
	public void setClearCacheExit(boolean enable){
		mPrefs.edit().putBoolean(Name.CLEAR_CACHE_EXIT, enable).apply();
	}
	
	public boolean getCookiesEnabled(){
		return mPrefs.getBoolean(Name.COOKIES, true);
	}
	
	public void setCookiesEnabled(boolean enable){
		mPrefs.edit().putBoolean(Name.COOKIES, enable).apply();
	}
	
	public String getDownloadDirectory(){
		return mPrefs.getString(Name.DOWNLOAD_DIRECTORY, Environment.DIRECTORY_DOWNLOADS);
	}
	
	public void setDownloadDirectory(String directory){
		mPrefs.edit().putString(Name.DOWNLOAD_DIRECTORY, directory).apply();
	}
	
	public boolean getFullScreenEnabled(){
		return mPrefs.getBoolean(Name.FULL_SCREEN, false);
	}
	
	public void setFullScreenEnabled(boolean enable){
		mPrefs.edit().putBoolean(Name.FULL_SCREEN, enable).apply();
	}
	
	public boolean getHideStatusBarEnabled(){
		return mPrefs.getBoolean(Name.HIDE_STATUS_BAR, false);
	}
	
	public void setHideStatusBarEnabled(boolean enable){
		mPrefs.edit().putBoolean(Name.HIDE_STATUS_BAR, enable).apply();
	}
	
	public String getHomepage(){
		return mPrefs.getString(Name.HOMEPAGE, Constants.HOMEPAGE);
	}
	
	public void setHomepage(String homepage){
		mPrefs.edit().putString(Name.HOMEPAGE, homepage).apply();
	}
	
	public boolean getIncognitoCookiesEnabled(){
		return mPrefs.getBoolean(Name.INCOGNITO_COOKIES, false);
	}
	
	public void setIncognitoCookiesEnabled(boolean enable){
		mPrefs.edit().putBoolean(Name.INCOGNITO_COOKIES, enable).apply();
	}
	
	public boolean getJavaScriptEnabled(){
		return mPrefs.getBoolean(PreferenceConstants.JAVASCRIPT, true);
	}
	
	public void setJavaScriptEnabled(boolean enable){
		mPrefs.edit().putBoolean(Name.JAVASCRIPT, enable).apply();
	}
	
	public boolean getLocationEnabled(){
		return mPrefs.getBoolean(Name.LOCATION, false);
	}
	
	public void setLocationEnabled(boolean enable){
		mPrefs.edit().putBoolean(Name.LOCATION, enable).apply();
	}
	
	public boolean getOverviewModeEnabled(){
		return mPrefs.getBoolean(Name.OVERVIEW_MODE, true);
	}
	
	public void setOverviewModeEnabled(boolean enable){
		mPrefs.edit().putBoolean(Name.OVERVIEW_MODE, enable).apply();
	}
	
	public boolean getPopupsEnabled(){
		return mPrefs.getBoolean(Name.POPUPS, true);
	}

	public void setPopupsEnabled(boolean enable){
		mPrefs.edit().putBoolean(Name.POPUPS, enable).apply();
	}
	
	public boolean getRestoreLostTabsEnabled(){
		return mPrefs.getBoolean(Name.RESTORE_LOST_TABS, true);
	}
	
	public void setRestoreLostTabsEnabled(boolean enable){
		mPrefs.edit().putBoolean(Name.RESTORE_LOST_TABS, enable).apply();
	}
	
	public boolean getSavePasswordsEnabled(){
		return mPrefs.getBoolean(Name.SAVE_PASSWORDS, true);
	}
	
	public void setSavePasswordsEnabled(boolean enable){
		mPrefs.edit().putBoolean(Name.SAVE_PASSWORDS, enable).apply();
	}
	
	public int getSearchChoice(){
		return mPrefs.getInt(Name.SEARCH, 1);
	}
	
	public void setSearchChoice(int choice){
		mPrefs.edit().putInt(Name.SEARCH, choice).apply();
	}
	
	public String getSearchUrl(){
		return mPrefs.getString(Name.SEARCH_URL, Constants.GOOGLE_SEARCH);
	}
	
	public void setSearchUrl(String url){
		mPrefs.edit().putString(Name.SEARCH_URL, url).apply();
	}
	
	public boolean getSystemBrowserPresent(){
		return mPrefs.getBoolean(Name.SYSTEM_BROWSER_PRESENT, false);
	}
	
	public void setSystemBrowserPresent(boolean available){
		mPrefs.edit().putBoolean(Name.SYSTEM_BROWSER_PRESENT, available).apply();
	}
	
	public boolean getTextReflowEnabled(){
		return mPrefs.getBoolean(Name.TEXT_REFLOW, false);
	}
	
	public void setTextReflowEnabled(boolean enable){
		mPrefs.edit().putBoolean(Name.TEXT_REFLOW, enable).apply();
	}
	
	public int getTextSize(){
		return mPrefs.getInt(Name.TEXT_SIZE, 3);
	}
	
	public void setTextSize(int size){
		mPrefs.edit().putInt(Name.TEXT_SIZE, size).apply();
	}
	
	public String getMemoryUrl(){
		return mPrefs.getString(Name.URL_MEMORY, "");
	}
	
	public void setMemoryUrl(String url){
		mPrefs.edit().putString(Name.URL_MEMORY, url).apply();
	}
	
	public boolean getUseWideViewportEnabled(){
		return mPrefs.getBoolean(Name.USE_WIDE_VIEWPORT, true);
	}
	
	public void setUseWideViewportEnabled(boolean enable){
		mPrefs.edit().putBoolean(Name.USE_WIDE_VIEWPORT, enable).apply();
	}
	
	public int getUserAgentChoice(){
		return mPrefs.getInt(Name.USER_AGENT, 1);
	}
	
	public void setUserAgentChoice(int choice){
		mPrefs.edit().putInt(Name.USER_AGENT, choice).apply();
	}
	
	public String getUserAgentString(String def){
		return mPrefs.getString(Name.USER_AGENT_STRING, def);
	}
	
	public void setUserAgentString(String agent){
		mPrefs.edit().putString(Name.USER_AGENT_STRING, agent).apply();
	}
	
	public boolean getGoogleSearchSuggestionsEnabled(){
		return mPrefs.getBoolean(Name.GOOGLE_SEARCH_SUGGESTIONS, true);
	}
	
	public void setGoogleSearchSuggestionsEnabled(boolean enabled){
		mPrefs.edit().putBoolean(Name.GOOGLE_SEARCH_SUGGESTIONS, enabled).apply();
	}
	
	public boolean getClearHistoryExitEnabled(){
		return mPrefs.getBoolean(Name.CLEAR_HISTORY_EXIT, false);
	}
	
	public void setClearHistoryExitEnabled(boolean enable){
		mPrefs.edit().putBoolean(Name.CLEAR_HISTORY_EXIT, enable).apply();
	}
	
	public boolean getClearCookiesExitEnabled(){
		return mPrefs.getBoolean(Name.CLEAR_COOKIES_EXIT, false);
	}
	
	public void setClearCookiesExitEnabled(boolean enable){
		mPrefs.edit().putBoolean(Name.CLEAR_COOKIES_EXIT, enable).apply();
	}
	
	public String getSavedUrl(){
		return mPrefs.getString(Name.SAVE_URL, null);
	}
	
	public void setSavedUrl(String url){
		mPrefs.edit().putString(Name.SAVE_URL, url).apply();
	}
	
	public int getRenderingMode(){
		return mPrefs.getInt(Name.RENDERING_MODE, 0);
	}
	
	public void setRenderingMode(int mode){
		mPrefs.edit().putInt(Name.RENDERING_MODE, mode).apply();
	}
	
	public boolean getSyncHistoryEnabled(){
		return mPrefs.getBoolean(Name.SYNC_HISTORY, true);
	}
	
	public void setSyncHistoryEnabled(boolean enable){
		mPrefs.edit().putBoolean(Name.SYNC_HISTORY, enable).apply();
	}

	public boolean getBlockThirdPartyCookiesEnabled(){
		return mPrefs.getBoolean(Name.BLOCK_THIRD_PARTY, false);
	}
	
	public void setBlockThirdPartyCookiesEnabled(boolean enable){
		mPrefs.edit().putBoolean(Name.BLOCK_THIRD_PARTY, enable).apply();
	}
	
	public boolean getColorModeEnabled(){
		return mPrefs.getBoolean(Name.ENABLE_COLOR_MODE, true);
	}
	
	public void setColorModeEnabled(boolean enable){
		mPrefs.edit().putBoolean(Name.ENABLE_COLOR_MODE, enable).apply();
	}
	
	public int getUrlBoxContentChoice(){
		return mPrefs.getInt(Name.URL_BOX_CONTENTS, 0);
	}
	
	public void setUrlBoxContentChoice(int choice){
		mPrefs.edit().putInt(Name.URL_BOX_CONTENTS, choice).apply();
	}
	
	public boolean getUseProxy(){
		return mPrefs.getBoolean(Name.USE_PROXY, false);
	}
	
	public void setUseProxy(boolean enable){
		mPrefs.edit().putBoolean(Name.USE_PROXY, enable).apply();
	}
	
	public String getProxyHost(){
		return mPrefs.getString(Name.USE_PROXY_HOST, "localhost");
	}
	
	public int getProxyPort(){
		return mPrefs.getInt(Name.USE_PROXY_PORT, 8118);
	}
	
	public boolean getCheckedForTor(){
		return mPrefs.getBoolean(Name.INITIAL_CHECK_FOR_TOR, false);
	}
	
	public void setCheckedForTor(boolean check){
		mPrefs.edit().putBoolean(Name.INITIAL_CHECK_FOR_TOR, check).apply();
	}
	
	private class Name {
		public static final String ADOBE_FLASH_SUPPORT = "enableflash";
		public static final String BLOCK_ADS = "AdBlock";
		public static final String BLOCK_IMAGES = "blockimages";
		public static final String CLEAR_CACHE_EXIT = "cache";
		public static final String COOKIES = "cookies";
		public static final String DOWNLOAD_DIRECTORY = "download";
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
		public static final String SYSTEM_BROWSER_PRESENT = "SystemBrowser";
		public static final String TEXT_REFLOW = "textreflow";
		public static final String TEXT_SIZE = "textsize";
		public static final String URL_MEMORY = "memory";
		public static final String USE_WIDE_VIEWPORT = "wideviewport";
		public static final String USER_AGENT = "agentchoose";
		public static final String USER_AGENT_STRING = "userAgentString";
		public static final String GOOGLE_SEARCH_SUGGESTIONS = "GoogleSearchSuggestions";
		public static final String CLEAR_HISTORY_EXIT = "clearHistoryExit";
		public static final String CLEAR_COOKIES_EXIT = "clearCookiesExit";
		public static final String SAVE_URL = "saveUrl";
		public static final String RENDERING_MODE = "renderMode";
		public static final String SYNC_HISTORY = "syncHistory";
		public static final String BLOCK_THIRD_PARTY = "thirdParty";
		public static final String ENABLE_COLOR_MODE = "colorMode";
		public static final String URL_BOX_CONTENTS = "urlContent";

		public static final String USE_PROXY = "useProxy";
		public static final String USE_PROXY_HOST = "useProxyHost";
		public static final String USE_PROXY_PORT = "useProxyPort";
		public static final String INITIAL_CHECK_FOR_TOR = "checkForTor";
	}
}
