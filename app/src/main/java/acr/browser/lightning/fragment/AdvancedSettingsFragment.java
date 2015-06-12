/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import acr.browser.lightning.R;

public class AdvancedSettingsFragment extends PreferenceFragment {

    private Activity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference_advanced);

        mActivity = getActivity();
    }
}
