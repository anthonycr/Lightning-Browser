/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import acr.browser.lightning.R;
import acr.browser.lightning.dialog.BrowserDialog;

public class DisplaySettingsFragment extends LightningPreferenceFragment implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final String SETTINGS_HIDESTATUSBAR = "fullScreenOption";
    private static final String SETTINGS_FULLSCREEN = "fullscreen";
    private static final String SETTINGS_VIEWPORT = "wideViewPort";
    private static final String SETTINGS_OVERVIEWMODE = "overViewMode";
    private static final String SETTINGS_REFLOW = "text_reflow";
    private static final String SETTINGS_THEME = "app_theme";
    private static final String SETTINGS_TEXTSIZE = "text_size";
    private static final String SETTINGS_DRAWERTABS = "cb_drawertabs";
    private static final String SETTINGS_SWAPTABS = "cb_swapdrawers";

    private static final float XXLARGE = 30.0f;
    private static final float XLARGE = 26.0f;
    private static final float LARGE = 22.0f;
    private static final float MEDIUM = 18.0f;
    private static final float SMALL = 14.0f;
    private static final float XSMALL = 10.0f;

    private Activity mActivity;
    private CheckBoxPreference cbstatus, cbfullscreen, cbviewport, cboverview, cbreflow;
    private Preference theme;
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
        mThemeOptions = this.getResources().getStringArray(R.array.themes);
        mCurrentTheme = mPreferenceManager.getUseTheme();

        theme = findPreference(SETTINGS_THEME);
        Preference textsize = findPreference(SETTINGS_TEXTSIZE);
        cbstatus = (CheckBoxPreference) findPreference(SETTINGS_HIDESTATUSBAR);
        cbfullscreen = (CheckBoxPreference) findPreference(SETTINGS_FULLSCREEN);
        cbviewport = (CheckBoxPreference) findPreference(SETTINGS_VIEWPORT);
        cboverview = (CheckBoxPreference) findPreference(SETTINGS_OVERVIEWMODE);
        cbreflow = (CheckBoxPreference) findPreference(SETTINGS_REFLOW);
        CheckBoxPreference cbDrawerTabs = (CheckBoxPreference) findPreference(SETTINGS_DRAWERTABS);
        CheckBoxPreference cbSwapTabs = (CheckBoxPreference) findPreference(SETTINGS_SWAPTABS);

        theme.setOnPreferenceClickListener(this);
        textsize.setOnPreferenceClickListener(this);
        cbstatus.setOnPreferenceChangeListener(this);
        cbfullscreen.setOnPreferenceChangeListener(this);
        cbviewport.setOnPreferenceChangeListener(this);
        cboverview.setOnPreferenceChangeListener(this);
        cbreflow.setOnPreferenceChangeListener(this);
        cbDrawerTabs.setOnPreferenceChangeListener(this);
        cbSwapTabs.setOnPreferenceChangeListener(this);

        cbstatus.setChecked(mPreferenceManager.getHideStatusBarEnabled());
        cbfullscreen.setChecked(mPreferenceManager.getFullScreenEnabled());
        cbviewport.setChecked(mPreferenceManager.getUseWideViewportEnabled());
        cboverview.setChecked(mPreferenceManager.getOverviewModeEnabled());
        cbreflow.setChecked(mPreferenceManager.getTextReflowEnabled());
        cbDrawerTabs.setChecked(mPreferenceManager.getShowTabsInDrawer(true));
        cbSwapTabs.setChecked(mPreferenceManager.getBookmarksAndTabsSwapped());

        theme.setSummary(mThemeOptions[mPreferenceManager.getUseTheme()]);
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
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
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        boolean checked = false;
        if (newValue instanceof Boolean) {
            checked = Boolean.TRUE.equals(newValue);
        }
        // switch preferences
        switch (preference.getKey()) {
            case SETTINGS_HIDESTATUSBAR:
                mPreferenceManager.setHideStatusBarEnabled(checked);
                cbstatus.setChecked(checked);
                return true;
            case SETTINGS_FULLSCREEN:
                mPreferenceManager.setFullScreenEnabled(checked);
                cbfullscreen.setChecked(checked);
                return true;
            case SETTINGS_VIEWPORT:
                mPreferenceManager.setUseWideViewportEnabled(checked);
                cbviewport.setChecked(checked);
                return true;
            case SETTINGS_OVERVIEWMODE:
                mPreferenceManager.setOverviewModeEnabled(checked);
                cboverview.setChecked(checked);
                return true;
            case SETTINGS_REFLOW:
                mPreferenceManager.setTextReflowEnabled(checked);
                cbreflow.setChecked(checked);
                return true;
            case SETTINGS_DRAWERTABS:
                mPreferenceManager.setShowTabsInDrawer(checked);
                return true;
            case SETTINGS_SWAPTABS:
                mPreferenceManager.setBookmarkAndTabsSwapped(checked);
                return true;
            default:
                return false;
        }
    }

    private void textSizePicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        LinearLayout view = (LinearLayout) inflater.inflate(R.layout.seek_layout, null);
        final SeekBar bar = (SeekBar) view.findViewById(R.id.text_size_seekbar);
        final TextView sample = new TextView(getActivity());
        sample.setText(R.string.untitled);
        sample.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT));
        sample.setGravity(Gravity.CENTER_HORIZONTAL);
        view.addView(sample);
        bar.setOnSeekBarChangeListener(new TextSeekBarListener(sample));
        final int MAX = 5;
        bar.setMax(MAX);
        bar.setProgress(MAX - mPreferenceManager.getTextSize());
        builder.setView(view);
        builder.setTitle(R.string.title_text_size);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                mPreferenceManager.setTextSize(MAX - bar.getProgress());
            }

        });
        Dialog dialog = builder.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }

    private static float getTextSize(int size) {
        switch (size) {
            case 0:
                return XSMALL;
            case 1:
                return SMALL;
            case 2:
                return MEDIUM;
            case 3:
                return LARGE;
            case 4:
                return XLARGE;
            case 5:
                return XXLARGE;
            default:
                return MEDIUM;
        }
    }

    private void themePicker() {
        AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
        picker.setTitle(getResources().getString(R.string.theme));

        int n = mPreferenceManager.getUseTheme();
        picker.setSingleChoiceItems(mThemeOptions, n, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPreferenceManager.setUseTheme(which);
                if (which < mThemeOptions.length) {
                    theme.setSummary(mThemeOptions[which]);
                }
            }
        });
        picker.setPositiveButton(getResources().getString(R.string.action_ok),
            new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mCurrentTheme != mPreferenceManager.getUseTheme()) {
                        getActivity().onBackPressed();
                    }
                }
            });
        picker.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (mCurrentTheme != mPreferenceManager.getUseTheme()) {
                    getActivity().onBackPressed();
                }
            }
        });
        Dialog dialog = picker.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }

    private static class TextSeekBarListener implements SeekBar.OnSeekBarChangeListener {

        private final TextView sample;

        public TextSeekBarListener(TextView sample) {this.sample = sample;}

        @Override
        public void onProgressChanged(SeekBar view, int size, boolean user) {
            this.sample.setTextSize(getTextSize(size));
        }

        @Override
        public void onStartTrackingTouch(SeekBar arg0) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar arg0) {
        }

    }
}
