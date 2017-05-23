/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.fragment;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import acr.browser.lightning.R;

public class AboutSettingsFragment extends PreferenceFragment {

    private Activity mActivity;

    private static final String SETTINGS_VERSION = "pref_version";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference_about);

        mActivity = getActivity();

        Preference version = findPreference(SETTINGS_VERSION);
        version.setSummary(getVersion());
    }

    private String getVersion() {
        try {
            PackageInfo p = mActivity.getPackageManager().getPackageInfo(mActivity.getPackageName(), 0);
            return p.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "1.0";
        }
    }
}
