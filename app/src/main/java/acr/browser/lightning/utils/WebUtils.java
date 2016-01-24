package acr.browser.lightning.utils;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebIconDatabase;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewDatabase;

import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.database.HistoryDatabase;

/**
 * Copyright 8/4/2015 Anthony Restaino
 */
public class WebUtils {

    public static void clearCookies(@NonNull Context context) {
        CookieManager c = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            c.removeAllCookies(null);
        } else {
            //noinspection deprecation
            CookieSyncManager.createInstance(context);
            //noinspection deprecation
            c.removeAllCookie();
        }
    }

    public static void clearWebStorage() {
        WebStorage.getInstance().deleteAllData();
    }

    public static void clearHistory(@NonNull Context context, HistoryDatabase historyDatabase) {
        historyDatabase.deleteHistory();
        WebViewDatabase m = WebViewDatabase.getInstance(context);
        m.clearFormData();
        m.clearHttpAuthUsernamePassword();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            //noinspection deprecation
            m.clearUsernamePassword();
            //noinspection deprecation
            WebIconDatabase.getInstance().removeAllIcons();
        }
        Utils.trimCache(context);
    }

    public static void clearCache(WebView view) {
        if (view == null) return;
        view.clearCache(true);
    }

}
