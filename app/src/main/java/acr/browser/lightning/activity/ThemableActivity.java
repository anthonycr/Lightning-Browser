package acr.browser.lightning.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import acr.browser.lightning.R;
import acr.browser.lightning.preference.PreferenceManager;

public abstract class ThemableActivity extends AppCompatActivity {

    private int mTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mTheme = PreferenceManager.getInstance().getUseTheme();

        // set the theme
        if (mTheme == 1) {
            setTheme(R.style.Theme_DarkTheme);
        } else if (mTheme == 2) {
            setTheme(R.style.Theme_BlackTheme);
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

    private void restart() {
        recreate();
    }
}
