package acr.browser.lightning;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public abstract class ThemableSettingsActivity extends ActionBarActivity {

	private SharedPreferences mPreferences;
	private boolean mDark;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mPreferences = getSharedPreferences(PreferenceConstants.PREFERENCES, 0);
		mDark = mPreferences.getBoolean(PreferenceConstants.DARK_THEME, false);

		// set the theme
		if (mDark) {
			setTheme(R.style.Theme_SettingsTheme_Dark);
		}
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mPreferences != null
				&& mPreferences.getBoolean(PreferenceConstants.DARK_THEME,
						false) != mDark) {
			restart();
		}
	}

	protected void restart() {
		final Bundle outState = new Bundle();
		onSaveInstanceState(outState);
		final Intent intent = new Intent(this, getClass());
		finish();
		overridePendingTransition(0, 0);
		startActivity(intent);
	}
}
