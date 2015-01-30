package acr.browser.lightning;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

public class MainActivity extends BrowserActivity {

	SharedPreferences mPreferences;

	CookieManager mCookieManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPreferences = getSharedPreferences(PreferenceConstants.PREFERENCES, 0);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void updateCookiePreference() {
		if (mPreferences == null) {
			mPreferences = getSharedPreferences(PreferenceConstants.PREFERENCES, 0);
		}
		mCookieManager = CookieManager.getInstance();
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			CookieSyncManager.createInstance(this);
		}
		mCookieManager.setAcceptCookie(mPreferences.getBoolean(PreferenceConstants.COOKIES, true));
		super.updateCookiePreference();
	}

	@Override
	public synchronized void initializeTabs() {
		super.initializeTabs();
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
		super.updateHistory(title, url);
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
