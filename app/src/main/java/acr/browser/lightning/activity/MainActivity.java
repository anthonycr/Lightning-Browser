package acr.browser.lightning.activity;

import android.content.Intent;
import android.os.Build;
import android.view.Menu;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import acr.browser.lightning.R;
import acr.browser.lightning.preference.PreferenceManager;

@SuppressWarnings("deprecation")
public class MainActivity extends BrowserActivity {

    @Override
    public void updateCookiePreference() {
        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(this);
        }
        cookieManager.setAcceptCookie(PreferenceManager.getInstance().getCookiesEnabled());
    }

    @Override
    public synchronized void initializeTabs() {
        restoreOrNewTab();
        // if incognito mode use newTab(null, true); instead
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleNewIntent(intent);
        super.onNewIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveOpenTabs();
    }

    @Override
    public void updateHistory(String title, String url) {
        addItemToHistory(title, url);
    }

    @Override
    public boolean isIncognito() {
        return false;
    }

    @Override
    public int getMenu() {
        return R.menu.main;
    }

    @Override
    public void closeActivity() {
        closeDrawers();
        moveTaskToBack(true);
    }
}
