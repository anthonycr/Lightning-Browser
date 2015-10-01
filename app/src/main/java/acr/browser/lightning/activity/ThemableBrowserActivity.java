package acr.browser.lightning.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import acr.browser.lightning.R;
import acr.browser.lightning.preference.PreferenceManager;

public abstract class ThemableBrowserActivity extends AppCompatActivity {

    private int mTheme;
    private boolean mShowTabsInDrawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mTheme = PreferenceManager.getInstance().getUseTheme();
        mShowTabsInDrawer = PreferenceManager.getInstance().getShowTabsInDrawer(!isTablet());

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
        int theme = PreferenceManager.getInstance().getUseTheme();
        boolean drawerTabs = PreferenceManager.getInstance().getShowTabsInDrawer(!isTablet());
        if (theme != mTheme || mShowTabsInDrawer != drawerTabs) {
            restart();
        }
    }

    boolean isTablet() {
        return (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    private void restart() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }
}
