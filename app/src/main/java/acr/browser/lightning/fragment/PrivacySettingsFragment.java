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

public class PrivacySettingsFragment extends PreferenceFragment {

    private Activity mActivity;
    private PreferenceManager mPreferences;
    private CheckBoxPreference cblocation, cb3cookies, cbsavepasswords, cbcacheexit, cbhistoryexit,
            cbcookiesexit, synchistory;
    private Preference clearcache, clearhistory, clearcookies;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference_privacy);

        mActivity = getActivity();

        initPrefs();
    }

    private void initPrefs() {
        // mPreferences storage
        mPreferences = PreferenceManager.getInstance();
    }
}
