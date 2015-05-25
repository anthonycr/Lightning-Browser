/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.constant;

import android.os.Environment;

import acr.browser.lightning.BuildConfig;

public final class Constants {

	private Constants() {
	}

	public static final boolean FULL_VERSION = BuildConfig.FULL_VERSION;
	
	public static final String DESKTOP_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2049.0 Safari/537.36";
	public static final String MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; U; Android 4.4; en-us; Nexus 4 Build/JOP24G) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30";
	public static final String YAHOO_SEARCH = "https://search.yahoo.com/search?p=";
	public static final String GOOGLE_SEARCH = "https://www.google.com/search?client=lightning&ie=UTF-8&oe=UTF-8&q=";
	public static final String BING_SEARCH = "https://www.bing.com/search?q=";
	public static final String DUCK_SEARCH = "https://duckduckgo.com/?t=lightning&q=";
	public static final String DUCK_LITE_SEARCH = "https://duckduckgo.com/lite/?t=lightning&q=";
	public static final String STARTPAGE_MOBILE_SEARCH = "https://startpage.com/do/m/mobilesearch?language=english&query=";
	public static final String STARTPAGE_SEARCH = "https://startpage.com/do/search?language=english&query=";
	public static final String ASK_SEARCH = "http://www.ask.com/web?qsrc=0&o=0&l=dir&qo=lightningBrowser&q=";
	public static final String HOMEPAGE = "about:home";
	public static final String BAIDU_SEARCH = "https://www.baidu.com/s?wd=";
	public static final String YANDEX_SEARCH = "https://yandex.ru/yandsearch?lr=21411&text=";
	public static final String EXTERNAL_STORAGE = Environment.getExternalStorageDirectory()
			.toString();
	public static final String JAVASCRIPT_INVERT_PAGE = "javascript:(function(){var e='img {-webkit-filter: invert(100%);'+'-moz-filter: invert(100%);'+'-o-filter: invert(100%);'+'-ms-filter: invert(100%); }',t=document.getElementsByTagName('head')[0],n=document.createElement('style');if(!window.counter){window.counter=1}else{window.counter++;if(window.counter%2==0){var e='html {-webkit-filter: invert(0%); -moz-filter: invert(0%); -o-filter: invert(0%); -ms-filter: invert(0%); }'}}n.type='text/css';if(n.styleSheet){n.styleSheet.cssText=e}else{n.appendChild(document.createTextNode(e))}t.appendChild(n)})();";
	public static final String JAVASCRIPT_TEXT_REFLOW = "javascript:document.getElementsByTagName('body')[0].style.width=window.innerWidth+'px';";

	public static final String LOAD_READING_URL = "ReadingUrl";

	public static final String SEPARATOR = "\\|\\$\\|SEPARATOR\\|\\$\\|";
	public static final String HTTP = "http://";
	public static final String HTTPS = "https://";
	public static final String FILE = "file://";
	public static final String FOLDER = "folder://";
	public static final String TAG = "Lightning";
}
