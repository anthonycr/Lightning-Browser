/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.activity;

import android.os.Bundle;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import acr.browser.lightning.R;

public class SettingsActivity extends ThemableSettingsActivity {

    private static List<String> fragments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: set up ToolBar
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preferences_headers, target);
        fragments.clear();
        for (Header header : target) {
            fragments.add(header.fragment);
        }
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return fragments.contains(fragmentName);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return true;
    }
}
