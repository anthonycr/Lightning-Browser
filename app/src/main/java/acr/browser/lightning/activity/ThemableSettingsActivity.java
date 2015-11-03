package acr.browser.lightning.activity;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import acr.browser.lightning.R;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.utils.ThemeUtils;

public abstract class ThemableSettingsActivity extends AppCompatPreferenceActivity {

    private int mTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mTheme = BrowserApp.getAppComponent().getPreferenceManager().getUseTheme();

        // set the theme
        if (mTheme == 0) {
            setTheme(R.style.Theme_SettingsTheme);
            this.getWindow().setBackgroundDrawable(new ColorDrawable(ThemeUtils.getPrimaryColor(this)));
        } else if (mTheme == 1) {
            setTheme(R.style.Theme_SettingsTheme_Dark);
            this.getWindow().setBackgroundDrawable(new ColorDrawable(ThemeUtils.getPrimaryColorDark(this)));
        } else if (mTheme == 2) {
            setTheme(R.style.Theme_SettingsTheme_Black);
            this.getWindow().setBackgroundDrawable(new ColorDrawable(ThemeUtils.getPrimaryColorDark(this)));
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (BrowserApp.getAppComponent().getPreferenceManager().getUseTheme() != mTheme) {
            restart();
        }
    }

    private void restart() {
        recreate();
    }
}
