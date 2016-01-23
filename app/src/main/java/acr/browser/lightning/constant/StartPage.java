/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.constant;

import android.app.Application;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;

import acr.browser.lightning.R;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.utils.Utils;
import acr.browser.lightning.view.LightningView;

public class StartPage extends AsyncTask<Void, Void, Void> {

    public static final String FILENAME = "homepage.html";

    private static final String HEAD_1 = "<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\">"
            + "<head>"
            + "<meta content=\"en-us\" http-equiv=\"Content-Language\" />"
            + "<meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\" />"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">"
            + "<title>";

    private static final String HEAD_2 = "</title>"
            + "</head>"
            + "<style>body{background:#f2f2f2;text-align:center;margin:0px;}#search_input{height:35px; "
            + "width:100%;outline:none;border:none;font-size: 16px;background-color:transparent;}"
            + "span { display: block; overflow: hidden; padding-left:5px;vertical-align:middle;}"
            + ".search_bar{display:table;vertical-align:middle;width:90%;height:35px;max-width:500px;margin:0 auto;background-color:#fff;box-shadow: 0px 2px 3px rgba( 0, 0, 0, 0.25 );"
            + "font-family: Arial;color: #444;-moz-border-radius: 2px;-webkit-border-radius: 2px;border-radius: 2px;}"
            + "#search_submit{outline:none;height:37px;float:right;color:#404040;font-size:16px;font-weight:bold;border:none;"
            + "background-color:transparent;}.outer { display: table; position: absolute; height: 100%; width: 100%;}"
            + ".middle { display: table-cell; vertical-align: middle;}.inner { margin-left: auto; margin-right: auto; "
            + "margin-bottom:10%; width: 100%;}img.smaller{width:50%;max-width:300px;}"
            + ".box { vertical-align:middle;position:relative; display: block; margin: 10px;padding-left:10px;padding-right:10px;padding-top:5px;padding-bottom:5px;"
            + " background-color:#fff;box-shadow: 0px 3px rgba( 0, 0, 0, 0.1 );font-family: Arial;color: #444;"
            + "font-size: 12px;-moz-border-radius: 2px;-webkit-border-radius: 2px;"
            + "border-radius: 2px;}</style><body> <div class=\"outer\"><div class=\"middle\"><div class=\"inner\"><img class=\"smaller\" src=\"";

    private static final String MIDDLE = "\" ></br></br><form onsubmit=\"return search()\" class=\"search_bar\">"
            + "<input type=\"submit\" id=\"search_submit\" value=\"Search\" ><span><input class=\"search\" type=\"text\" value=\"\" id=\"search_input\" >"
            + "</span></form></br></br></div></div></div><script type=\"text/javascript\">function search(){if(document.getElementById(\"search_input\").value != \"\"){window.location.href = \"";

    private static final String END = "\" + document.getElementById(\"search_input\").value;document.getElementById(\"search_input\").value = \"\";}return false;}</script></body></html>";

    private final String mTitle;
    private final File mFilesDir;
    private final WeakReference<LightningView> mTabReference;

    private String mStartpageUrl;

    public StartPage(LightningView tab, Application app) {
        mTitle = app.getString(R.string.home);
        mFilesDir = app.getFilesDir();
        mTabReference = new WeakReference<>(tab);
    }

    @Override
    protected Void doInBackground(Void... params) {
        mStartpageUrl = getHomepage();
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        LightningView tab = mTabReference.get();
        if (tab != null) {
            tab.loadUrl(mStartpageUrl);
        }
    }

    /**
     * This method builds the homepage and returns the local URL to be loaded
     * when it finishes building.
     *
     * @return the URL to load
     */
    private String getHomepage() {
        StringBuilder homepageBuilder = new StringBuilder(HEAD_1 + mTitle + HEAD_2);
        String icon;
        String searchUrl;
        final PreferenceManager preferenceManager = BrowserApp.getPreferenceManager();
        switch (preferenceManager.getSearchChoice()) {
            case 0:
                // CUSTOM SEARCH
                icon = "file:///android_asset/lightning.png";
                searchUrl = preferenceManager.getSearchUrl();
                break;
            case 1:
                // GOOGLE_SEARCH;
                icon = "file:///android_asset/google.png";
                // "https://www.google.com/images/srpr/logo11w.png";
                searchUrl = Constants.GOOGLE_SEARCH;
                break;
            case 2:
                // ANDROID SEARCH;
                icon = "file:///android_asset/ask.png";
                searchUrl = Constants.ASK_SEARCH;
                break;
            case 3:
                // BING_SEARCH;
                icon = "file:///android_asset/bing.png";
                // "http://upload.wikimedia.org/wikipedia/commons/thumb/b/b1/Bing_logo_%282013%29.svg/500px-Bing_logo_%282013%29.svg.png";
                searchUrl = Constants.BING_SEARCH;
                break;
            case 4:
                // YAHOO_SEARCH;
                icon = "file:///android_asset/yahoo.png";
                // "http://upload.wikimedia.org/wikipedia/commons/thumb/2/24/Yahoo%21_logo.svg/799px-Yahoo%21_logo.svg.png";
                searchUrl = Constants.YAHOO_SEARCH;
                break;
            case 5:
                // STARTPAGE_SEARCH;
                icon = "file:///android_asset/png";
                // "https://com/graphics/startp_logo.gif";
                searchUrl = Constants.STARTPAGE_SEARCH;
                break;
            case 6:
                // STARTPAGE_MOBILE
                icon = "file:///android_asset/png";
                // "https://com/graphics/startp_logo.gif";
                searchUrl = Constants.STARTPAGE_MOBILE_SEARCH;
                break;
            case 7:
                // DUCK_SEARCH;
                icon = "file:///android_asset/duckduckgo.png";
                // "https://duckduckgo.com/assets/logo_homepage.normal.v101.png";
                searchUrl = Constants.DUCK_SEARCH;
                break;
            case 8:
                // DUCK_LITE_SEARCH;
                icon = "file:///android_asset/duckduckgo.png";
                // "https://duckduckgo.com/assets/logo_homepage.normal.v101.png";
                searchUrl = Constants.DUCK_LITE_SEARCH;
                break;
            case 9:
                // BAIDU_SEARCH;
                icon = "file:///android_asset/baidu.png";
                // "http://www.baidu.com/img/bdlogo.gif";
                searchUrl = Constants.BAIDU_SEARCH;
                break;
            case 10:
                // YANDEX_SEARCH;
                icon = "file:///android_asset/yandex.png";
                // "http://upload.wikimedia.org/wikipedia/commons/thumb/9/91/Yandex.svg/600px-Yandex.svg.png";
                searchUrl = Constants.YANDEX_SEARCH;
                break;
            default:
                // DEFAULT GOOGLE_SEARCH;
                icon = "file:///android_asset/google.png";
                searchUrl = Constants.GOOGLE_SEARCH;
                break;

        }

        homepageBuilder.append(icon);
        homepageBuilder.append(MIDDLE);
        homepageBuilder.append(searchUrl);
        homepageBuilder.append(END);

        File homepage = new File(mFilesDir, FILENAME);
        FileWriter hWriter = null;
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            hWriter = new FileWriter(homepage, false);
            hWriter.write(homepageBuilder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Utils.close(hWriter);
        }

        return Constants.FILE + homepage;
    }

    public void load() {
        executeOnExecutor(BrowserApp.getIOThread());
    }

}
