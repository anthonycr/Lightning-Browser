/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import acr.browser.lightning.R;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.dialog.BrowserDialog;
import acr.browser.lightning.download.DownloadHandler;
import acr.browser.lightning.utils.ProxyUtils;
import acr.browser.lightning.utils.ThemeUtils;
import acr.browser.lightning.utils.Utils;

import static acr.browser.lightning.preference.PreferenceManager.*;

public class GeneralSettingsFragment extends LightningPreferenceFragment implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final String SETTINGS_PROXY = "proxy";
    private static final String SETTINGS_FLASH = "cb_flash";
    private static final String SETTINGS_ADS = "cb_ads";
    private static final String SETTINGS_IMAGES = "cb_images";
    private static final String SETTINGS_JAVASCRIPT = "cb_javascript";
    private static final String SETTINGS_COLORMODE = "cb_colormode";
    private static final String SETTINGS_USERAGENT = "agent";
    private static final String SETTINGS_DOWNLOAD = "download";
    private static final String SETTINGS_HOME = "home";
    private static final String SETTINGS_SEARCHENGINE = "search";
    private static final String SETTINGS_SUGGESTIONS = "suggestions_choice";
    private Activity mActivity;
    private static final int API = android.os.Build.VERSION.SDK_INT;
    private CharSequence[] mProxyChoices;
    private Preference proxy, useragent, downloadloc, home, searchengine, searchsSuggestions;
    private String mDownloadLocation;
    private int mAgentChoice;
    private String mHomepage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference_general);

        mActivity = getActivity();

        initPrefs();
    }

    private void initPrefs() {
        proxy = findPreference(SETTINGS_PROXY);
        useragent = findPreference(SETTINGS_USERAGENT);
        downloadloc = findPreference(SETTINGS_DOWNLOAD);
        home = findPreference(SETTINGS_HOME);
        searchengine = findPreference(SETTINGS_SEARCHENGINE);
        searchsSuggestions = findPreference(SETTINGS_SUGGESTIONS);

        CheckBoxPreference cbFlash = (CheckBoxPreference) findPreference(SETTINGS_FLASH);
        CheckBoxPreference cbAds = (CheckBoxPreference) findPreference(SETTINGS_ADS);
        CheckBoxPreference cbImages = (CheckBoxPreference) findPreference(SETTINGS_IMAGES);
        CheckBoxPreference cbJsScript = (CheckBoxPreference) findPreference(SETTINGS_JAVASCRIPT);
        CheckBoxPreference cbColorMode = (CheckBoxPreference) findPreference(SETTINGS_COLORMODE);

        proxy.setOnPreferenceClickListener(this);
        useragent.setOnPreferenceClickListener(this);
        downloadloc.setOnPreferenceClickListener(this);
        home.setOnPreferenceClickListener(this);
        searchsSuggestions.setOnPreferenceClickListener(this);
        searchengine.setOnPreferenceClickListener(this);
        cbFlash.setOnPreferenceChangeListener(this);
        cbAds.setOnPreferenceChangeListener(this);
        cbImages.setOnPreferenceChangeListener(this);
        cbJsScript.setOnPreferenceChangeListener(this);
        cbColorMode.setOnPreferenceChangeListener(this);

        mAgentChoice = mPreferenceManager.getUserAgentChoice();
        mHomepage = mPreferenceManager.getHomepage();
        mDownloadLocation = mPreferenceManager.getDownloadDirectory();
        mProxyChoices = getResources().getStringArray(R.array.proxy_choices_array);

        int choice = mPreferenceManager.getProxyChoice();
        if (choice == Constants.PROXY_MANUAL) {
            proxy.setSummary(mPreferenceManager.getProxyHost() + ':' + mPreferenceManager.getProxyPort());
        } else {
            proxy.setSummary(mProxyChoices[choice]);
        }

        if (API >= Build.VERSION_CODES.KITKAT) {
            mPreferenceManager.setFlashSupport(0);
        }

        setSearchEngineSummary(mPreferenceManager.getSearchChoice());

        downloadloc.setSummary(mDownloadLocation);

        switch (mPreferenceManager.getSearchSuggestionChoice()) {
            case SUGGESTION_GOOGLE:
                searchsSuggestions.setSummary(R.string.powered_by_google);
                break;
            case SUGGESTION_DUCK:
                searchsSuggestions.setSummary(R.string.powered_by_duck);
                break;
            case SUGGESTION_NONE:
                searchsSuggestions.setSummary(R.string.search_suggestions_off);
                break;
        }


        if (mHomepage.contains(Constants.SCHEME_HOMEPAGE)) {
            home.setSummary(getResources().getString(R.string.action_homepage));
        } else if (mHomepage.contains(Constants.SCHEME_BLANK)) {
            home.setSummary(getResources().getString(R.string.action_blank));
        } else if (mHomepage.contains(Constants.SCHEME_BOOKMARKS)) {
            home.setSummary(getResources().getString(R.string.action_bookmarks));
        } else {
            home.setSummary(mHomepage);
        }

        switch (mAgentChoice) {
            case 1:
                useragent.setSummary(getResources().getString(R.string.agent_default));
                break;
            case 2:
                useragent.setSummary(getResources().getString(R.string.agent_desktop));
                break;
            case 3:
                useragent.setSummary(getResources().getString(R.string.agent_mobile));
                break;
            case 4:
                useragent.setSummary(getResources().getString(R.string.agent_custom));
        }

        int flashNum = mPreferenceManager.getFlashSupport();
        boolean imagesBool = mPreferenceManager.getBlockImagesEnabled();
        boolean enableJSBool = mPreferenceManager.getJavaScriptEnabled();

        cbAds.setEnabled(Constants.FULL_VERSION);

        if (API < Build.VERSION_CODES.KITKAT) {
            cbFlash.setEnabled(true);
        } else {
            cbFlash.setEnabled(false);
            cbFlash.setSummary(getResources().getString(R.string.flash_not_supported));
        }

        cbImages.setChecked(imagesBool);
        cbJsScript.setChecked(enableJSBool);
        cbFlash.setChecked(flashNum > 0);
        cbAds.setChecked(Constants.FULL_VERSION && mPreferenceManager.getAdBlockEnabled());
        cbColorMode.setChecked(mPreferenceManager.getColorModeEnabled());
    }

    private void searchUrlPicker() {
        final AlertDialog.Builder urlPicker = new AlertDialog.Builder(mActivity);
        urlPicker.setTitle(getResources().getString(R.string.custom_url));
        final EditText getSearchUrl = new EditText(mActivity);
        String mSearchUrl = mPreferenceManager.getSearchUrl();
        getSearchUrl.setText(mSearchUrl);
        urlPicker.setView(getSearchUrl);
        urlPicker.setPositiveButton(getResources().getString(R.string.action_ok),
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String text = getSearchUrl.getText().toString();
                    mPreferenceManager.setSearchUrl(text);
                    searchengine.setSummary(getResources().getString(R.string.custom_url) + ": "
                        + text);
                }
            });
        Dialog dialog = urlPicker.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
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
                        mPreferenceManager.setFlashSupport(1);
                    }
                })
            .setNegativeButton(getResources().getString(R.string.action_auto),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPreferenceManager.setFlashSupport(2);
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                mPreferenceManager.setFlashSupport(0);
            }

        });
        AlertDialog alert = builder.create();
        alert.show();
        BrowserDialog.setDialogSize(mActivity, alert);
    }

    private void proxyChoicePicker() {
        AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
        picker.setTitle(getResources().getString(R.string.http_proxy));
        picker.setSingleChoiceItems(mProxyChoices, mPreferenceManager.getProxyChoice(),
            new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    setProxyChoice(which);
                }
            });
        picker.setPositiveButton(getResources().getString(R.string.action_ok), null);
        Dialog dialog = picker.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }

    private void setProxyChoice(@Constants.PROXY int choice) {
        switch (choice) {
            case Constants.PROXY_ORBOT:
                choice = ProxyUtils.setProxyChoice(choice, mActivity);
                break;
            case Constants.PROXY_I2P:
                choice = ProxyUtils.setProxyChoice(choice, mActivity);
                break;
            case Constants.PROXY_MANUAL:
                manualProxyPicker();
                break;
        }

        mPreferenceManager.setProxyChoice(choice);
        if (choice < mProxyChoices.length)
            proxy.setSummary(mProxyChoices[choice]);
    }

    private void manualProxyPicker() {
        View v = mActivity.getLayoutInflater().inflate(R.layout.picker_manual_proxy, null);
        final EditText eProxyHost = (EditText) v.findViewById(R.id.proxyHost);
        final EditText eProxyPort = (EditText) v.findViewById(R.id.proxyPort);

        // Limit the number of characters since the port needs to be of type int
        // Use input filters to limite the EditText length and determine the max
        // length by using length of integer MAX_VALUE
        int maxCharacters = Integer.toString(Integer.MAX_VALUE).length();
        InputFilter[] filterArray = new InputFilter[1];
        filterArray[0] = new InputFilter.LengthFilter(maxCharacters - 1);
        eProxyPort.setFilters(filterArray);

        eProxyHost.setText(mPreferenceManager.getProxyHost());
        eProxyPort.setText(Integer.toString(mPreferenceManager.getProxyPort()));

        Dialog dialog = new AlertDialog.Builder(mActivity)
            .setTitle(R.string.manual_proxy)
            .setView(v)
            .setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String proxyHost = eProxyHost.getText().toString();
                    int proxyPort;
                    try {
                        // Try/Catch in case the user types an empty string or a number
                        // larger than max integer
                        proxyPort = Integer.parseInt(eProxyPort.getText().toString());
                    } catch (NumberFormatException ignored) {
                        proxyPort = mPreferenceManager.getProxyPort();
                    }
                    mPreferenceManager.setProxyHost(proxyHost);
                    mPreferenceManager.setProxyPort(proxyPort);
                    proxy.setSummary(proxyHost + ':' + proxyPort);
                }
            }).show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }

    private void searchDialog() {
        AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
        picker.setTitle(getResources().getString(R.string.title_search_engine));
        CharSequence[] chars = {getResources().getString(R.string.custom_url), "Google",
            "Ask", "Bing", "Yahoo", "StartPage", "StartPage (Mobile)",
            "DuckDuckGo (Privacy)", "DuckDuckGo Lite (Privacy)", "Baidu (Chinese)",
            "Yandex (Russian)"};

        int n = mPreferenceManager.getSearchChoice();

        picker.setSingleChoiceItems(chars, n, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPreferenceManager.setSearchChoice(which);
                setSearchEngineSummary(which);
            }
        });
        picker.setPositiveButton(getResources().getString(R.string.action_ok), null);
        Dialog dialog = picker.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }

    private void homepageDialog() {
        AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
        picker.setTitle(getResources().getString(R.string.home));
        mHomepage = mPreferenceManager.getHomepage();
        int n;
        switch (mHomepage) {
            case Constants.SCHEME_HOMEPAGE:
                n = 0;
                break;
            case Constants.SCHEME_BLANK:
                n = 1;
                break;
            case Constants.SCHEME_BOOKMARKS:
                n = 2;
                break;
            default:
                n = 3;
                break;
        }

        picker.setSingleChoiceItems(R.array.homepage, n,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which + 1) {
                        case 1:
                            mPreferenceManager.setHomepage(Constants.SCHEME_HOMEPAGE);
                            home.setSummary(getResources().getString(R.string.action_homepage));
                            break;
                        case 2:
                            mPreferenceManager.setHomepage(Constants.SCHEME_BLANK);
                            home.setSummary(getResources().getString(R.string.action_blank));
                            break;
                        case 3:
                            mPreferenceManager.setHomepage(Constants.SCHEME_BOOKMARKS);
                            home.setSummary(getResources().getString(R.string.action_bookmarks));
                            break;
                        case 4:
                            homePicker();
                            break;
                    }
                }
            });
        picker.setPositiveButton(getResources().getString(R.string.action_ok), null);
        Dialog dialog = picker.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }

    private void suggestionsDialog() {
        AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
        picker.setTitle(getResources().getString(R.string.search_suggestions));

        int currentChoice = 2;

        switch (mPreferenceManager.getSearchSuggestionChoice()) {
            case SUGGESTION_GOOGLE:
                currentChoice = 0;
                break;
            case SUGGESTION_DUCK:
                currentChoice = 1;
                break;
            case SUGGESTION_NONE:
                currentChoice = 2;
                break;
        }

        picker.setSingleChoiceItems(R.array.suggestions, currentChoice,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:
                            mPreferenceManager.setSearchSuggestionChoice(Suggestion.SUGGESTION_GOOGLE);
                            searchsSuggestions.setSummary(R.string.powered_by_google);
                            break;
                        case 1:
                            mPreferenceManager.setSearchSuggestionChoice(Suggestion.SUGGESTION_DUCK);
                            searchsSuggestions.setSummary(R.string.powered_by_duck);
                            break;
                        case 2:
                            mPreferenceManager.setSearchSuggestionChoice(Suggestion.SUGGESTION_NONE);
                            searchsSuggestions.setSummary(R.string.search_suggestions_off);
                            break;
                    }
                }
            });
        picker.setPositiveButton(getResources().getString(R.string.action_ok), null);
        Dialog dialog = picker.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }

    private void homePicker() {
        final AlertDialog.Builder homePicker = new AlertDialog.Builder(mActivity);
        homePicker.setTitle(getResources().getString(R.string.title_custom_homepage));
        final EditText getHome = new EditText(mActivity);
        mHomepage = mPreferenceManager.getHomepage();
        if (!mHomepage.startsWith(Constants.ABOUT)) {
            getHome.setText(mHomepage);
        } else {
            String defaultUrl = "https://www.google.com";
            getHome.setText(defaultUrl);
        }
        homePicker.setView(getHome);
        homePicker.setPositiveButton(getResources().getString(R.string.action_ok),
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String text = getHome.getText().toString();
                    mPreferenceManager.setHomepage(text);
                    home.setSummary(text);
                }
            });
        Dialog dialog = homePicker.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }

    private void downloadLocDialog() {
        AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
        picker.setTitle(getResources().getString(R.string.title_download_location));
        mDownloadLocation = mPreferenceManager.getDownloadDirectory();
        int n;
        if (mDownloadLocation.contains(Environment.DIRECTORY_DOWNLOADS)) {
            n = 0;
        } else {
            n = 1;
        }

        picker.setSingleChoiceItems(R.array.download_folder, n,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:
                            mPreferenceManager.setDownloadDirectory(DownloadHandler.DEFAULT_DOWNLOAD_PATH);
                            downloadloc.setSummary(DownloadHandler.DEFAULT_DOWNLOAD_PATH);
                            break;
                        case 1:
                            downPicker();
                            break;
                    }
                }
            });
        picker.setPositiveButton(getResources().getString(R.string.action_ok), null);
        Dialog dialog = picker.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }

    private void agentDialog() {
        AlertDialog.Builder agentPicker = new AlertDialog.Builder(mActivity);
        agentPicker.setTitle(getResources().getString(R.string.title_user_agent));
        mAgentChoice = mPreferenceManager.getUserAgentChoice();
        agentPicker.setSingleChoiceItems(R.array.user_agent, mAgentChoice - 1,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mPreferenceManager.setUserAgentChoice(which + 1);
                    switch (which + 1) {
                        case 1:
                            useragent.setSummary(getResources().getString(R.string.agent_default));
                            break;
                        case 2:
                            useragent.setSummary(getResources().getString(R.string.agent_desktop));
                            break;
                        case 3:
                            useragent.setSummary(getResources().getString(R.string.agent_mobile));
                            break;
                        case 4:
                            useragent.setSummary(getResources().getString(R.string.agent_custom));
                            agentPicker();
                            break;
                    }
                }
            });
        agentPicker.setPositiveButton(getResources().getString(R.string.action_ok), null);
        Dialog dialog = agentPicker.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }

    private void agentPicker() {
        final AlertDialog.Builder agentStringPicker = new AlertDialog.Builder(mActivity);
        agentStringPicker.setTitle(getResources().getString(R.string.title_user_agent));
        final EditText getAgent = new EditText(mActivity);
        agentStringPicker.setView(getAgent);
        agentStringPicker.setPositiveButton(getResources().getString(R.string.action_ok),
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String text = getAgent.getText().toString();
                    mPreferenceManager.setUserAgentString(text);
                    useragent.setSummary(getResources().getString(R.string.agent_custom));
                }
            });
        Dialog dialog = agentStringPicker.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }

    private void downPicker() {
        final AlertDialog.Builder downLocationPicker = new AlertDialog.Builder(mActivity);
        LinearLayout layout = new LinearLayout(mActivity);
        downLocationPicker.setTitle(getResources().getString(R.string.title_download_location));
        final EditText getDownload = new EditText(mActivity);
        getDownload.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
        getDownload.setText(mPreferenceManager.getDownloadDirectory());
        final int errorColor = ContextCompat.getColor(getActivity(), R.color.error_red);
        final int regularColor = ThemeUtils.getTextColor(getActivity());
        getDownload.setTextColor(regularColor);
        getDownload.addTextChangedListener(new DownloadLocationTextWatcher(getDownload, errorColor, regularColor));
        getDownload.setText(mPreferenceManager.getDownloadDirectory());

        layout.addView(getDownload);
        downLocationPicker.setView(layout);
        downLocationPicker.setPositiveButton(getResources().getString(R.string.action_ok),
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String text = getDownload.getText().toString();
                    text = DownloadHandler.addNecessarySlashes(text);
                    mPreferenceManager.setDownloadDirectory(text);
                    downloadloc.setSummary(text);
                }
            });
        Dialog dialog = downLocationPicker.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }

    private void setSearchEngineSummary(int which) {
        switch (which) {
            case 0:
                searchUrlPicker();
                break;
            case 1:
                searchengine.setSummary("Google");
                break;
            case 2:
                searchengine.setSummary("Ask");
                break;
            case 3:
                searchengine.setSummary("Bing");
                break;
            case 4:
                searchengine.setSummary("Yahoo");
                break;
            case 5:
                searchengine.setSummary("StartPage");
                break;
            case 6:
                searchengine.setSummary("StartPage (Mobile)");
                break;
            case 7:
                searchengine.setSummary("DuckDuckGo");
                break;
            case 8:
                searchengine.setSummary("DuckDuckGo Lite");
                break;
            case 9:
                searchengine.setSummary("Baidu");
                break;
            case 10:
                searchengine.setSummary("Yandex");
        }
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        switch (preference.getKey()) {
            case SETTINGS_PROXY:
                proxyChoicePicker();
                return true;
            case SETTINGS_USERAGENT:
                agentDialog();
                return true;
            case SETTINGS_DOWNLOAD:
                downloadLocDialog();
                return true;
            case SETTINGS_HOME:
                homepageDialog();
                return true;
            case SETTINGS_SEARCHENGINE:
                searchDialog();
                return true;
            case SETTINGS_SUGGESTIONS:
                suggestionsDialog();
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
        switch (preference.getKey()) {
            case SETTINGS_FLASH:
                if (!Utils.isFlashInstalled(mActivity) && checked) {
                    Utils.createInformativeDialog(mActivity, R.string.title_warning, R.string.dialog_adobe_not_installed);
                    mPreferenceManager.setFlashSupport(0);
                    return false;
                } else {
                    if (checked) {
                        getFlashChoice();
                    } else {
                        mPreferenceManager.setFlashSupport(0);
                    }
                }
                return true;
            case SETTINGS_ADS:
                mPreferenceManager.setAdBlockEnabled(checked);
                return true;
            case SETTINGS_IMAGES:
                mPreferenceManager.setBlockImagesEnabled(checked);
                return true;
            case SETTINGS_JAVASCRIPT:
                mPreferenceManager.setJavaScriptEnabled(checked);
                return true;
            case SETTINGS_COLORMODE:
                mPreferenceManager.setColorModeEnabled(checked);
                return true;
            default:
                return false;
        }
    }

    private static class DownloadLocationTextWatcher implements TextWatcher {
        private final EditText getDownload;
        private final int errorColor;
        private final int regularColor;

        public DownloadLocationTextWatcher(EditText getDownload, int errorColor, int regularColor) {
            this.getDownload = getDownload;
            this.errorColor = errorColor;
            this.regularColor = regularColor;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(@NonNull Editable s) {
            if (!DownloadHandler.isWriteAccessAvailable(s.toString())) {
                this.getDownload.setTextColor(this.errorColor);
            } else {
                this.getDownload.setTextColor(this.regularColor);
            }
        }
    }
}
