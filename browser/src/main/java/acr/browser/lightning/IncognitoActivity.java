package acr.browser.lightning;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

public class IncognitoActivity extends BrowserActivity {

    SharedPreferences mPreferences;

    CookieManager mCookieManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = getSharedPreferences(PreferenceConstants.PREFERENCES, 0);
    }

    @Override
    public void updateCookiePreference() {
        if (mPreferences == null) {
            mPreferences = getSharedPreferences(PreferenceConstants.PREFERENCES, 0);
        }
        mCookieManager = CookieManager.getInstance();
        CookieSyncManager.createInstance(this);
        mCookieManager.setAcceptCookie(mPreferences.getBoolean(
                PreferenceConstants.INCOGNITO_COOKIES, false));
        super.updateCookiePreference();
    }

    @Override
    public synchronized void initializeTabs() {
        super.initializeTabs();
        // restoreOrNewTab();
        newTab(null, true);
        // if incognito mode use newTab(null, true); instead
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.incognito, menu);
        return super.onCreateOptionsMenu(menu);
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
}
