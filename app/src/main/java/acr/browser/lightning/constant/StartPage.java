/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.constant;

import android.app.Application;
import android.support.annotation.NonNull;

import com.anthonycr.bonsai.Single;
import com.anthonycr.bonsai.SingleAction;
import com.anthonycr.bonsai.SingleSubscriber;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.BrowserApp;
import acr.browser.lightning.search.SearchEngineProvider;
import acr.browser.lightning.search.engine.BaseSearchEngine;
import acr.browser.lightning.utils.Utils;

public class StartPage {

    public static final String FILENAME = "homepage.html";

    private static final String HEAD_1 = "<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\">"
        + "<head>"
        + "<meta content=\"en-us\" http-equiv=\"Content-Language\" />"
        + "<meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\" />"
        + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">"
        + "<title>";

    private static final String HEAD_2 = "</title>"
        + "</head>"
        + "<style>body{background:#f5f5f5;text-align:center;margin:0px;}#search_input{height:35px; "
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

    private static final String MIDDLE = "\" ></br></br><form onsubmit=\"return search()\" class=\"search_bar\" autocomplete=\"off\">"
        + "<input type=\"submit\" id=\"search_submit\" value=\"Search\" ><span><input class=\"search\" type=\"text\" value=\"\" id=\"search_input\" >"
        + "</span></form></br></br></div></div></div><script type=\"text/javascript\">function search(){if(document.getElementById(\"search_input\").value != \"\"){window.location.href = \"";

    private static final String END = "\" + document.getElementById(\"search_input\").value;document.getElementById(\"search_input\").value = \"\";}return false;}</script></body></html>";

    @NonNull
    public static File getStartPageFile(@NonNull Application application) {
        return new File(application.getFilesDir(), FILENAME);
    }

    @NonNull private final String mTitle;

    @Inject Application mApp;
    @Inject SearchEngineProvider mSearchEngineProvider;

    public StartPage() {
        BrowserApp.getAppComponent().inject(this);
        mTitle = mApp.getString(R.string.home);
    }

    @NonNull
    public Single<String> getHomepage() {
        return Single.create(new SingleAction<String>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {

                StringBuilder homepageBuilder = new StringBuilder(HEAD_1 + mTitle + HEAD_2);

                BaseSearchEngine currentSearchEngine = mSearchEngineProvider.getCurrentSearchEngine();

                String icon = currentSearchEngine.getIconUrl();
                String searchUrl = currentSearchEngine.getQueryUrl();

                homepageBuilder.append(icon);
                homepageBuilder.append(MIDDLE);
                homepageBuilder.append(searchUrl);
                homepageBuilder.append(END);

                File homepage = getStartPageFile(mApp);
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

                subscriber.onItem(Constants.FILE + homepage);

            }
        });
    }

}
