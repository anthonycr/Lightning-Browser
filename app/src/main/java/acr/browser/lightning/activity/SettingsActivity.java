/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.activity;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.anthonycr.grant.PermissionsManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import acr.browser.lightning.R;
import acr.browser.lightning.app.BrowserApp;

public class SettingsActivity extends ThemableSettingsActivity {

    private static final List<String> mFragments = new ArrayList<>(7);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // this is a workaround for the Toolbar in PreferenceActitivty
        ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        LinearLayout content = (LinearLayout) root.getChildAt(0);
        LinearLayout toolbarContainer = (LinearLayout) View.inflate(this, R.layout.toolbar_settings, null);

        root.removeAllViews();
        toolbarContainer.addView(content);
        root.addView(toolbarContainer);

        // now we can set the Toolbar using AppCompatPreferenceActivity
        Toolbar toolbar = (Toolbar) toolbarContainer.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preferences_headers, target);
        mFragments.clear();
        Iterator<Header> headerIterator = target.iterator();
        while (headerIterator.hasNext()) {
            Header header = headerIterator.next();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                // Workaround for bug in the AppCompat support library
                header.iconRes = R.drawable.empty;
            }

            if (header.titleRes == R.string.debug_title) {
                if (BrowserApp.isRelease()) {
                    headerIterator.remove();
                } else {
                    mFragments.add(header.fragment);
                }
            } else {
                mFragments.add(header.fragment);
            }
        }
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return mFragments.contains(fragmentName);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
