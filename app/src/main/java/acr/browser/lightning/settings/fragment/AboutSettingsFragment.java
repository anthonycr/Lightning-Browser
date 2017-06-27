/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.settings.fragment;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import acr.browser.lightning.R;

public class AboutSettingsFragment extends PreferenceFragment {

    private static final String TAG = "AboutSettingsFragment";
    private static final String SETTINGS_VERSION = "pref_version";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference_about);

        Preference version = findPreference(SETTINGS_VERSION);
        version.setSummary(getVersion());
    }

    private String getVersion() {
        try {
            Activity activity = getActivity();
            String packageName = activity.getPackageName();
            PackageInfo packageInfo = activity.getPackageManager().getPackageInfo(packageName, 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "getVersion: error", e);
            return "1.0";
        }
    }
}
