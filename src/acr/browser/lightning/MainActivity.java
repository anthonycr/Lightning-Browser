package acr.browser.lightning;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

@SuppressWarnings("deprecation")
public class MainActivity extends BrowserActivity {

	SharedPreferences mPreferences;
	CookieManager mCookieManager;
	private boolean mDark;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mPreferences = getSharedPreferences(PreferenceConstants.PREFERENCES, 0);
		mDark = mPreferences.getBoolean(PreferenceConstants.DARK_THEME, false);
		if (mDark) {
			this.setTheme(R.style.Theme_DarkTheme);
		}
		super.onCreate(savedInstanceState);
	}

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
	protected void onResume() {
		super.onResume();
		if (mPreferences != null
				&& mPreferences.getBoolean(PreferenceConstants.DARK_THEME, false) != mDark) {
			this.recreate();
		}
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
