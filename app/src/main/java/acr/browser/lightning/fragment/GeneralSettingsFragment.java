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
import android.view.View;
import android.widget.EditText;

import acr.browser.lightning.R;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.utils.ProxyUtils;
import acr.browser.lightning.utils.Utils;

public class GeneralSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final String SETTINGS_PROXY = "proxy";
    private static final String SETTINGS_FLASH = "cb_flash";
    private static final String SETTINGS_ADS = "cb_ads";
    private static final String SETTINGS_IMAGES = "cb_images";
    private static final String SETTINGS_JAVASCRIPT = "cb_javascript";
    private static final String SETTINGS_COLORMODE = "cb_colormode";

    private Activity mActivity;
    private static final int API = android.os.Build.VERSION.SDK_INT;
    private PreferenceManager mPreferences;
    private CharSequence[] mProxyChoices;
    private Preference proxy;
    private CheckBoxPreference cbFlash, cbAds, cbImages, cbJsScript, cbColorMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference_general);

        mActivity = getActivity();

        initPrefs();
    }

    private void initPrefs() {
        // mPreferences storage
        mPreferences = PreferenceManager.getInstance();

        proxy = findPreference(SETTINGS_PROXY);
        cbFlash = (CheckBoxPreference) findPreference(SETTINGS_FLASH);
        cbAds = (CheckBoxPreference) findPreference(SETTINGS_ADS);
        cbImages = (CheckBoxPreference) findPreference(SETTINGS_IMAGES);
        cbJsScript = (CheckBoxPreference) findPreference(SETTINGS_JAVASCRIPT);
        cbColorMode = (CheckBoxPreference) findPreference(SETTINGS_COLORMODE);

        proxy.setOnPreferenceClickListener(this);
        cbFlash.setOnPreferenceChangeListener(this);
        cbAds.setOnPreferenceChangeListener(this);
        cbImages.setOnPreferenceChangeListener(this);
        cbJsScript.setOnPreferenceChangeListener(this);
        cbColorMode.setOnPreferenceChangeListener(this);

        mProxyChoices = getResources().getStringArray(R.array.proxy_choices_array);
        int choice = mPreferences.getProxyChoice();
        if (choice == Constants.PROXY_MANUAL) {
            proxy.setSummary(mPreferences.getProxyHost() + ":" + mPreferences.getProxyPort());
        } else {
            proxy.setSummary(mProxyChoices[choice]);
        }

        if (API >= 19) {
            mPreferences.setFlashSupport(0);
        }

        int flashNum = mPreferences.getFlashSupport();
        boolean imagesBool = mPreferences.getBlockImagesEnabled();
        boolean enableJSBool = mPreferences.getJavaScriptEnabled();

        proxy.setEnabled(Constants.FULL_VERSION);
        cbAds.setEnabled(Constants.FULL_VERSION);
        cbFlash.setEnabled(API < 19);

        cbImages.setChecked(imagesBool);
        cbJsScript.setChecked(enableJSBool);
        cbFlash.setChecked(flashNum > 0);
        cbAds.setChecked(Constants.FULL_VERSION && mPreferences.getAdBlockEnabled());
        cbColorMode.setChecked(mPreferences.getColorModeEnabled());
    }

    private void getFlashChoice() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(mActivity.getResources().getString(R.string.title_flash));
        builder.setMessage(getResources().getString(R.string.flash))
                .setCancelable(true)
                .setPositiveButton(getResources().getString(R.string.action_manual),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                mPreferences.setFlashSupport(1);
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.action_auto),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mPreferences.setFlashSupport(2);
                            }
                        }).setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                mPreferences.setFlashSupport(0);
            }

        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void proxyChoicePicker() {
        AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
        picker.setTitle(getResources().getString(R.string.http_proxy));
        picker.setSingleChoiceItems(mProxyChoices, mPreferences.getProxyChoice(),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setProxyChoice(which);
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

    private void setProxyChoice(int choice) {
        ProxyUtils utils = ProxyUtils.getInstance(mActivity);
        switch (choice) {
            case Constants.PROXY_ORBOT:
                choice = utils.setProxyChoice(choice, mActivity);
                break;
            case Constants.PROXY_I2P:
                choice = utils.setProxyChoice(choice, mActivity);
                break;
            case Constants.PROXY_MANUAL:
                manualProxyPicker();
                break;
        }

        mPreferences.setProxyChoice(choice);
        if (choice < mProxyChoices.length)
            proxy.setSummary(mProxyChoices[choice]);
    }

    public void manualProxyPicker() {
        View v = mActivity.getLayoutInflater().inflate(R.layout.picker_manual_proxy, null);
        final EditText eProxyHost = (EditText) v.findViewById(R.id.proxyHost);
        final EditText eProxyPort = (EditText) v.findViewById(R.id.proxyPort);
        eProxyHost.setText(mPreferences.getProxyHost());
        eProxyPort.setText(Integer.toString(mPreferences.getProxyPort()));

        new AlertDialog.Builder(mActivity)
                .setTitle(R.string.manual_proxy)
                .setView(v)
                .setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String proxyHost = eProxyHost.getText().toString();
                        int proxyPort = Integer.parseInt(eProxyPort.getText().toString());
                        mPreferences.setProxyHost(proxyHost);
                        mPreferences.setProxyPort(proxyPort);
                        proxy.setSummary(proxyHost + ":" + proxyPort);
                    }
                })
                .show();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case SETTINGS_PROXY:
                proxyChoicePicker();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // switch preferences
        switch (preference.getKey()) {
            case SETTINGS_FLASH:
                if (cbFlash.isChecked()) {
                    getFlashChoice();
                } else {
                    mPreferences.setFlashSupport(0);
                }

                // TODO: fix toast on flash cb click
                if (!Utils.isFlashInstalled(mActivity) && cbFlash.isChecked()) {
                    Utils.createInformativeDialog(mActivity,
                            mActivity.getResources().getString(R.string.title_warning),
                            mActivity.getResources().getString(R.string.dialog_adobe_not_installed));
                    cbFlash.setEnabled(false);
                    mPreferences.setFlashSupport(0);
                } else if ((API >= 17) && cbFlash.isChecked()) {
                    Utils.createInformativeDialog(mActivity,
                            mActivity.getResources().getString(R.string.title_warning),
                            mActivity.getResources().getString(R.string.dialog_adobe_unsupported));
                }
                cbFlash.setChecked((Boolean) newValue);
                return true;
            case SETTINGS_ADS:
                mPreferences.setAdBlockEnabled((Boolean) newValue);
                cbAds.setChecked((Boolean) newValue);
                return true;
            case SETTINGS_IMAGES:
                mPreferences.setBlockImagesEnabled((Boolean) newValue);
                cbImages.setChecked((Boolean) newValue);
                return true;
            case SETTINGS_JAVASCRIPT:
                mPreferences.setJavaScriptEnabled((Boolean) newValue);
                cbJsScript.setChecked((Boolean) newValue);
                return true;
            case SETTINGS_COLORMODE:
                mPreferences.setColorModeEnabled((Boolean) newValue);
                cbColorMode.setChecked((Boolean) newValue);
                return true;
            default:
                return false;
        }
    }
}
