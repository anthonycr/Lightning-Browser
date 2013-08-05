package acr.browser.barebones.utilities;

import android.os.Environment;


public class FinalVariables {
	
	public static final int MAX_TABS = 5;
	public static final int MAX_BOOKMARKS = 100;
	public static final boolean PAID_VERSION = false;
	public static final String DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/20 Safari/537.17";
	public static final String MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; U; Android 4.2; en-us; Nexus 4 Build/JOP24G) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30";
	public static final int API = android.os.Build.VERSION.SDK_INT;
	public static final String YAHOO_SEARCH = "http://search.yahoo.com/search?p=";
	public static final String GOOGLE_SEARCH = "https://www.google.com/search?client=lightning&q=";
	public static final String BING_SEARCH = "http://www.bing.com/search?q=";
	public static final String DUCK_SEARCH = "https://duckduckgo.com/?t=lightning&q=";
	public static final String STARTPAGE_SEARCH = "https://startpage.com/do/metasearch.pl?language=english&cat=web&query=";
	public static final String HOMEPAGE = "https://www.google.com/";
	public static final String SETTINGS_INTENT = "android.intent.action.SETTINGS";
	public static final String INCOGNITO_INTENT = "android.intent.action.BAREBONESINCOGNITO";
	public static final String DOWNLOAD_LOCATION = Environment.getExternalStorageDirectory().toString()+"/"+Environment.DIRECTORY_DOWNLOADS;
	
	
	
}
