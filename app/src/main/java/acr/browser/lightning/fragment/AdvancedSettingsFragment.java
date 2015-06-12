/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import acr.browser.lightning.R;
import acr.browser.lightning.preference.PreferenceManager;

public class AdvancedSettingsFragment extends PreferenceFragment {

    private Activity mActivity;
    private PreferenceManager mPreferences;
    private CheckBoxPreference newwindow, enablecookies, cookieInkognito, restoreTabs;
    private Preference renderingmode, urlcontent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference_advanced);

        mActivity = getActivity();

        initPrefs();
    }

    private void initPrefs() {
        // mPreferences storage
        mPreferences = PreferenceManager.getInstance();
    }
}
