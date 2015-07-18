package acr.browser.lightning.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.R;

@SuppressWarnings("deprecation")
public class IncognitoActivity extends BrowserActivity {

    @Override
    public void updateCookiePreference() {
        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(this);
        }
        cookieManager.setAcceptCookie(PreferenceManager.getInstance().getIncognitoCookiesEnabled());
        super.updateCookiePreference();
    }

    @Override
    public synchronized void initializeTabs() {
        newTab(null, true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.incognito, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // handleNewIntent(intent);
        super.onNewIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // saveOpenTabs();
    }

    @Override
    public void updateHistory(String title, String url) {
        // addItemToHistory(title, url);
    }

    @Override
    public boolean isIncognito() {
        return true;
    }

    @Override
    public void closeActivity() {
        closeDrawers();
        finish();
    }

    @Override
    public int getMenu() {
        return R.menu.incognito;
    }
}
