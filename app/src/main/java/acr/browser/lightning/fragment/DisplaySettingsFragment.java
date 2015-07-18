/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import acr.browser.lightning.R;
import acr.browser.lightning.activity.SettingsActivity;
import acr.browser.lightning.preference.PreferenceManager;

public class DisplaySettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final String SETTINGS_HIDESTATUSBAR = "fullScreenOption";
    private static final String SETTINGS_FULLSCREEN = "fullscreen";
    private static final String SETTINGS_VIEWPORT = "wideViewPort";
    private static final String SETTINGS_OVERVIEWMODE = "overViewMode";
    private static final String SETTINGS_REFLOW = "text_reflow";
    private static final String SETTINGS_THEME = "app_theme";
    private static final String SETTINGS_TEXTSIZE = "text_size";

    private Activity mActivity;
    private PreferenceManager mPreferences;
    private CheckBoxPreference cbstatus, cbfullscreen, cbviewport, cboverview, cbreflow;
    private Preference theme, textsize;
    private String[] mThemeOptions;
    private int mCurrentTheme;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference_display);

        mActivity = getActivity();

        initPrefs();
    }

    private void initPrefs() {
        // mPreferences storage
        mPreferences = PreferenceManager.getInstance();
        mThemeOptions = this.getResources().getStringArray(R.array.themes);
        mCurrentTheme = mPreferences.getUseTheme();

        theme = findPreference(SETTINGS_THEME);
        textsize = findPreference(SETTINGS_TEXTSIZE);
        cbstatus = (CheckBoxPreference) findPreference(SETTINGS_HIDESTATUSBAR);
        cbfullscreen = (CheckBoxPreference) findPreference(SETTINGS_FULLSCREEN);
        cbviewport = (CheckBoxPreference) findPreference(SETTINGS_VIEWPORT);
        cboverview = (CheckBoxPreference) findPreference(SETTINGS_OVERVIEWMODE);
        cbreflow = (CheckBoxPreference) findPreference(SETTINGS_REFLOW);

        theme.setOnPreferenceClickListener(this);
        textsize.setOnPreferenceClickListener(this);
        cbstatus.setOnPreferenceChangeListener(this);
        cbfullscreen.setOnPreferenceChangeListener(this);
        cbviewport.setOnPreferenceChangeListener(this);
        cboverview.setOnPreferenceChangeListener(this);
        cbreflow.setOnPreferenceChangeListener(this);

        cbstatus.setChecked(mPreferences.getHideStatusBarEnabled());
        cbfullscreen.setChecked(mPreferences.getFullScreenEnabled());
        cbviewport.setChecked(mPreferences.getUseWideViewportEnabled());
        cboverview.setChecked(mPreferences.getOverviewModeEnabled());
        cbreflow.setChecked(mPreferences.getTextReflowEnabled());

        theme.setSummary(mThemeOptions[mPreferences.getUseTheme()]);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case SETTINGS_THEME:
                themePicker();
                return true;
            case SETTINGS_TEXTSIZE:
                textSizePicker();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // switch preferences
        switch (preference.getKey()) {
            case SETTINGS_HIDESTATUSBAR:
                mPreferences.setHideStatusBarEnabled((Boolean) newValue);
                cbstatus.setChecked((Boolean) newValue);
                return true;
            case SETTINGS_FULLSCREEN:
                mPreferences.setFullScreenEnabled((Boolean) newValue);
                cbfullscreen.setChecked((Boolean) newValue);
                return true;
            case SETTINGS_VIEWPORT:
                mPreferences.setUseWideViewportEnabled((Boolean) newValue);
                cbviewport.setChecked((Boolean) newValue);
                return true;
            case SETTINGS_OVERVIEWMODE:
                mPreferences.setOverviewModeEnabled((Boolean) newValue);
                cboverview.setChecked((Boolean) newValue);
                return true;
            case SETTINGS_REFLOW:
                mPreferences.setTextReflowEnabled((Boolean) newValue);
                cbreflow.setChecked((Boolean) newValue);
                return true;
            default:
                return false;
        }
    }

    private void textSizePicker() {
        AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
        picker.setTitle(getResources().getString(R.string.title_text_size));

        int n = mPreferences.getTextSize();

        picker.setSingleChoiceItems(R.array.text_size, n - 1,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPreferences.setTextSize(which + 1);
                    }
                });
        picker.setNeutralButton(getResources().getString(R.string.action_ok),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        picker.show();
    }

    private void themePicker() {
        AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
        picker.setTitle(getResources().getString(R.string.url_contents));

        int n = mPreferences.getUseTheme();
        picker.setSingleChoiceItems(mThemeOptions, n, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPreferences.setUseTheme(which);
                if (which < mThemeOptions.length) {
                    theme.setSummary(mThemeOptions[which]);
                }
            }
        });
        picker.setNeutralButton(getResources().getString(R.string.action_ok),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mCurrentTheme != mPreferences.getUseTheme()) {
                            ((SettingsActivity) getActivity()).restart();
                        }
                    }
                });
        picker.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (mCurrentTheme != mPreferences.getUseTheme()) {
                    ((SettingsActivity) getActivity()).restart();
                }
            }
        });
        picker.show();
    }
}
