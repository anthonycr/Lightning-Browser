package acr.browser.lightning.activity;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import acr.browser.lightning.R;
import acr.browser.lightning.preference.PreferenceManager;

public abstract class ThemableSettingsActivity extends AppCompatPreferenceActivity  {

    private int mTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mTheme = PreferenceManager.getInstance().getUseTheme();

        // set the theme
        if (mTheme == 1) {
            setTheme(R.style.Theme_SettingsTheme_Dark);
        } else if (mTheme == 2) {
            setTheme(R.style.Theme_SettingsTheme_Black);
            this.getWindow().setBackgroundDrawable(new ColorDrawable(this.getResources().getColor(R.color.black)));
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (PreferenceManager.getInstance().getUseTheme() != mTheme) {
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
