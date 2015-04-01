package acr.browser.lightning;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.WindowManager;

public abstract class ThemableActivity extends ActionBarActivity {

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

		// set window params
		if (mPreferences.getBoolean(PreferenceConstants.HIDE_STATUS_BAR, false)) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mPreferences != null
				&& mPreferences.getBoolean(PreferenceConstants.DARK_THEME,
						false) != mDark) {
			this.recreate();
		}
	}
}
