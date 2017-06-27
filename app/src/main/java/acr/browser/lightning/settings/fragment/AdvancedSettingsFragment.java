/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.settings.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import java.util.Arrays;
import java.util.List;

import acr.browser.lightning.R;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.dialog.BrowserDialog;

public class AdvancedSettingsFragment extends LightningPreferenceFragment implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final String SETTINGS_NEWWINDOW = "allow_new_window";
    private static final String SETTINGS_ENABLECOOKIES = "allow_cookies";
    private static final String SETTINGS_COOKIESINKOGNITO = "incognito_cookies";
    private static final String SETTINGS_RESTORETABS = "restore_tabs";
    private static final String SETTINGS_RENDERINGMODE = "rendering_mode";
    private static final String SETTINGS_URLCONTENT = "url_contents";
    private static final String SETTINGS_TEXTENCODING = "text_encoding";

    private Activity mActivity;
    private CheckBoxPreference cbAllowPopups, cbenablecookies, cbcookiesInkognito, cbrestoreTabs;
    private Preference renderingmode, urlcontent, textEncoding;
    private CharSequence[] mUrlOptions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference_advanced);

        mActivity = getActivity();

        initPrefs();
    }

    private void initPrefs() {

        renderingmode = findPreference(SETTINGS_RENDERINGMODE);
        textEncoding = findPreference(SETTINGS_TEXTENCODING);
        urlcontent = findPreference(SETTINGS_URLCONTENT);
        cbAllowPopups = (CheckBoxPreference) findPreference(SETTINGS_NEWWINDOW);
        cbenablecookies = (CheckBoxPreference) findPreference(SETTINGS_ENABLECOOKIES);
        cbcookiesInkognito = (CheckBoxPreference) findPreference(SETTINGS_COOKIESINKOGNITO);
        cbrestoreTabs = (CheckBoxPreference) findPreference(SETTINGS_RESTORETABS);

        renderingmode.setOnPreferenceClickListener(this);
        textEncoding.setOnPreferenceClickListener(this);
        urlcontent.setOnPreferenceClickListener(this);
        cbAllowPopups.setOnPreferenceChangeListener(this);
        cbenablecookies.setOnPreferenceChangeListener(this);
        cbcookiesInkognito.setOnPreferenceChangeListener(this);
        cbrestoreTabs.setOnPreferenceChangeListener(this);

        switch (mPreferenceManager.getRenderingMode()) {
            case 0:
                renderingmode.setSummary(getString(R.string.name_normal));
                break;
            case 1:
                renderingmode.setSummary(getString(R.string.name_inverted));
                break;
            case 2:
                renderingmode.setSummary(getString(R.string.name_grayscale));
                break;
            case 3:
                renderingmode.setSummary(getString(R.string.name_inverted_grayscale));
                break;
            case 4:
                renderingmode.setSummary(getString(R.string.name_increase_contrast));
                break;
        }

        textEncoding.setSummary(mPreferenceManager.getTextEncoding());

        mUrlOptions = getResources().getStringArray(R.array.url_content_array);
        int option = mPreferenceManager.getUrlBoxContentChoice();
        urlcontent.setSummary(mUrlOptions[option]);

        cbAllowPopups.setChecked(mPreferenceManager.getPopupsEnabled());
        cbenablecookies.setChecked(mPreferenceManager.getCookiesEnabled());
        cbcookiesInkognito.setChecked(mPreferenceManager.getIncognitoCookiesEnabled());
        cbrestoreTabs.setChecked(mPreferenceManager.getRestoreLostTabsEnabled());
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        switch (preference.getKey()) {
            case SETTINGS_RENDERINGMODE:
                renderPicker();
                return true;
            case SETTINGS_URLCONTENT:
                urlBoxPicker();
                return true;
            case SETTINGS_TEXTENCODING:
                textEncodingPicker();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        // switch preferences
        switch (preference.getKey()) {
            case SETTINGS_NEWWINDOW:
                mPreferenceManager.setPopupsEnabled((Boolean) newValue);
                cbAllowPopups.setChecked((Boolean) newValue);
                return true;
            case SETTINGS_ENABLECOOKIES:
                mPreferenceManager.setCookiesEnabled((Boolean) newValue);
                cbenablecookies.setChecked((Boolean) newValue);
                return true;
            case SETTINGS_COOKIESINKOGNITO:
                mPreferenceManager.setIncognitoCookiesEnabled((Boolean) newValue);
                cbcookiesInkognito.setChecked((Boolean) newValue);
                return true;
            case SETTINGS_RESTORETABS:
                mPreferenceManager.setRestoreLostTabsEnabled((Boolean) newValue);
                cbrestoreTabs.setChecked((Boolean) newValue);
                return true;
            default:
                return false;
        }
    }

    private void renderPicker() {
        AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
        picker.setTitle(getResources().getString(R.string.rendering_mode));
        CharSequence[] chars = {mActivity.getString(R.string.name_normal),
                mActivity.getString(R.string.name_inverted),
                mActivity.getString(R.string.name_grayscale),
                mActivity.getString(R.string.name_inverted_grayscale),
                mActivity.getString(R.string.name_increase_contrast)};

        int n = mPreferenceManager.getRenderingMode();

        picker.setSingleChoiceItems(chars, n, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPreferenceManager.setRenderingMode(which);
                switch (which) {
                    case 0:
                        renderingmode.setSummary(getString(R.string.name_normal));
                        break;
                    case 1:
                        renderingmode.setSummary(getString(R.string.name_inverted));
                        break;
                    case 2:
                        renderingmode.setSummary(getString(R.string.name_grayscale));
                        break;
                    case 3:
                        renderingmode.setSummary(getString(R.string.name_inverted_grayscale));
                        break;
                    case 4:
                        renderingmode.setSummary(getString(R.string.name_increase_contrast));
                        break;
                }
            }
        });
        picker.setPositiveButton(getResources().getString(R.string.action_ok), null);
        Dialog dialog = picker.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }

    private void textEncodingPicker() {
        AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
        picker.setTitle(getResources().getString(R.string.text_encoding));
        final List<String> textEncodingList = Arrays.asList(Constants.TEXT_ENCODINGS);
        int n = textEncodingList.indexOf(mPreferenceManager.getTextEncoding());

        picker.setSingleChoiceItems(Constants.TEXT_ENCODINGS, n, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPreferenceManager.setTextEncoding(Constants.TEXT_ENCODINGS[which]);
                textEncoding.setSummary(Constants.TEXT_ENCODINGS[which]);
            }
        });
        picker.setPositiveButton(getResources().getString(R.string.action_ok), null);
        Dialog dialog = picker.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }

    private void urlBoxPicker() {
        AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
        picker.setTitle(getResources().getString(R.string.url_contents));

        int n = mPreferenceManager.getUrlBoxContentChoice();

        picker.setSingleChoiceItems(mUrlOptions, n, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPreferenceManager.setUrlBoxContentChoice(which);
                if (which < mUrlOptions.length) {
                    urlcontent.setSummary(mUrlOptions[which]);
                }
            }
        });
        picker.setPositiveButton(getResources().getString(R.string.action_ok), null);
        Dialog dialog = picker.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }
}
