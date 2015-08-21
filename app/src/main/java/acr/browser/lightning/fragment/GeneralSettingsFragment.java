/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

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
    private static final String SETTINGS_USERAGENT = "agent";
    private static final String SETTINGS_DOWNLOAD = "download";
    private static final String SETTINGS_HOME = "home";
    private static final String SETTINGS_SEARCHENGINE = "search";
    private static final String SETTINGS_GOOGLESUGGESTIONS = "google_suggestions";
    private static final String SETTINGS_DRAWERTABS = "cb_drawertabs";

    private Activity mActivity;
    private static final int API = android.os.Build.VERSION.SDK_INT;
    private PreferenceManager mPreferences;
    private CharSequence[] mProxyChoices;
    private Preference proxy, useragent, downloadloc, home, searchengine;
    private String mDownloadLocation;
    private int mAgentChoice;
    private String mHomepage;
    private CheckBoxPreference cbFlash, cbAds, cbImages, cbJsScript, cbColorMode, cbgooglesuggest, cbDrawerTabs;

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
        useragent = findPreference(SETTINGS_USERAGENT);
        downloadloc = findPreference(SETTINGS_DOWNLOAD);
        home = findPreference(SETTINGS_HOME);
        searchengine = findPreference(SETTINGS_SEARCHENGINE);

        cbFlash = (CheckBoxPreference) findPreference(SETTINGS_FLASH);
        cbAds = (CheckBoxPreference) findPreference(SETTINGS_ADS);
        cbImages = (CheckBoxPreference) findPreference(SETTINGS_IMAGES);
        cbJsScript = (CheckBoxPreference) findPreference(SETTINGS_JAVASCRIPT);
        cbColorMode = (CheckBoxPreference) findPreference(SETTINGS_COLORMODE);
        cbgooglesuggest = (CheckBoxPreference) findPreference(SETTINGS_GOOGLESUGGESTIONS);
        cbDrawerTabs = (CheckBoxPreference) findPreference(SETTINGS_DRAWERTABS);

        proxy.setOnPreferenceClickListener(this);
        useragent.setOnPreferenceClickListener(this);
        downloadloc.setOnPreferenceClickListener(this);
        home.setOnPreferenceClickListener(this);
        searchengine.setOnPreferenceClickListener(this);
        cbFlash.setOnPreferenceChangeListener(this);
        cbAds.setOnPreferenceChangeListener(this);
        cbImages.setOnPreferenceChangeListener(this);
        cbJsScript.setOnPreferenceChangeListener(this);
        cbColorMode.setOnPreferenceChangeListener(this);
        cbgooglesuggest.setOnPreferenceChangeListener(this);
        cbDrawerTabs.setOnPreferenceChangeListener(this);

        mAgentChoice = mPreferences.getUserAgentChoice();
        mHomepage = mPreferences.getHomepage();
        mDownloadLocation = mPreferences.getDownloadDirectory();
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

        setSearchEngineSummary(mPreferences.getSearchChoice());

        downloadloc.setSummary(Constants.EXTERNAL_STORAGE + '/' + mDownloadLocation);

        if (mHomepage.contains("about:home")) {
            home.setSummary(getResources().getString(R.string.action_homepage));
        } else if (mHomepage.contains("about:blank")) {
            home.setSummary(getResources().getString(R.string.action_blank));
        } else if (mHomepage.contains("about:bookmarks")) {
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
        cbgooglesuggest.setChecked(mPreferences.getGoogleSearchSuggestionsEnabled());
        cbDrawerTabs.setChecked(mPreferences.getShowTabsInDrawer(true));
    }

    private void searchUrlPicker() {
        final AlertDialog.Builder urlPicker = new AlertDialog.Builder(mActivity);
        urlPicker.setTitle(getResources().getString(R.string.custom_url));
        final EditText getSearchUrl = new EditText(mActivity);
        String mSearchUrl = mPreferences.getSearchUrl();
        getSearchUrl.setText(mSearchUrl);
        urlPicker.setView(getSearchUrl);
        urlPicker.setPositiveButton(getResources().getString(R.string.action_ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text = getSearchUrl.getText().toString();
                        mPreferences.setSearchUrl(text);
                        searchengine.setSummary(getResources().getString(R.string.custom_url) + ": "
                                + text);
                    }
                });
        urlPicker.show();
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

        eProxyHost.setText(mPreferences.getProxyHost());
        eProxyPort.setText(Integer.toString(mPreferences.getProxyPort()));

        new AlertDialog.Builder(mActivity)
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
                            proxyPort = mPreferences.getProxyPort();
                        }
                        mPreferences.setProxyHost(proxyHost);
                        mPreferences.setProxyPort(proxyPort);
                        proxy.setSummary(proxyHost + ":" + proxyPort);
                    }
                }).show();
    }

    private void searchDialog() {
        AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
        picker.setTitle(getResources().getString(R.string.title_search_engine));
        CharSequence[] chars = {getResources().getString(R.string.custom_url), "Google",
                "Ask", "Bing", "Yahoo", "StartPage", "StartPage (Mobile)",
                "DuckDuckGo (Privacy)", "DuckDuckGo Lite (Privacy)", "Baidu (Chinese)",
                "Yandex (Russian)"};

        int n = mPreferences.getSearchChoice();

        picker.setSingleChoiceItems(chars, n, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPreferences.setSearchChoice(which);
                setSearchEngineSummary(which);
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

    private void homepageDialog() {
        AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
        picker.setTitle(getResources().getString(R.string.home));
        mHomepage = mPreferences.getHomepage();
        int n;
        if (mHomepage.contains("about:home")) {
            n = 1;
        } else if (mHomepage.contains("about:blank")) {
            n = 2;
        } else if (mHomepage.contains("about:bookmarks")) {
            n = 3;
        } else {
            n = 4;
        }

        picker.setSingleChoiceItems(R.array.homepage, n - 1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which + 1) {
                            case 1:
                                mPreferences.setHomepage("about:home");
                                home.setSummary(getResources().getString(R.string.action_homepage));
                                break;
                            case 2:
                                mPreferences.setHomepage("about:blank");
                                home.setSummary(getResources().getString(R.string.action_blank));
                                break;
                            case 3:
                                mPreferences.setHomepage("about:bookmarks");
                                home.setSummary(getResources().getString(R.string.action_bookmarks));
                                break;
                            case 4:
                                homePicker();
                                break;
                        }
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

    private void homePicker() {
        final AlertDialog.Builder homePicker = new AlertDialog.Builder(mActivity);
        homePicker.setTitle(getResources().getString(R.string.title_custom_homepage));
        final EditText getHome = new EditText(mActivity);
        mHomepage = mPreferences.getHomepage();
        if (!mHomepage.startsWith("about:")) {
            getHome.setText(mHomepage);
        } else {
            getHome.setText("http://www.google.com");
        }
        homePicker.setView(getHome);
        homePicker.setPositiveButton(getResources().getString(R.string.action_ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text = getHome.getText().toString();
                        mPreferences.setHomepage(text);
                        home.setSummary(text);
                    }
                });
        homePicker.show();
    }

    private void downloadLocDialog() {
        AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
        picker.setTitle(getResources().getString(R.string.title_download_location));
        mDownloadLocation = mPreferences.getDownloadDirectory();
        int n;
        if (mDownloadLocation.contains(Environment.DIRECTORY_DOWNLOADS)) {
            n = 1;
        } else {
            n = 2;
        }

        picker.setSingleChoiceItems(R.array.download_folder, n - 1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which + 1) {
                            case 1:
                                mPreferences.setDownloadDirectory(Environment.DIRECTORY_DOWNLOADS);
                                downloadloc.setSummary(Constants.EXTERNAL_STORAGE + '/'
                                        + Environment.DIRECTORY_DOWNLOADS);
                                break;
                            case 2:
                                downPicker();
                                break;
                        }
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

    private void agentDialog() {
        AlertDialog.Builder agentPicker = new AlertDialog.Builder(mActivity);
        agentPicker.setTitle(getResources().getString(R.string.title_user_agent));
        mAgentChoice = mPreferences.getUserAgentChoice();
        agentPicker.setSingleChoiceItems(R.array.user_agent, mAgentChoice - 1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPreferences.setUserAgentChoice(which + 1);
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
        agentPicker.setNeutralButton(getResources().getString(R.string.action_ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        agentPicker.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Log.i("Cancelled", "");
            }
        });
        agentPicker.show();
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
                        mPreferences.setUserAgentString(text);
                        useragent.setSummary(getResources().getString(R.string.agent_custom));
                    }
                });
        agentStringPicker.show();
    }

    private void downPicker() {
        final AlertDialog.Builder downLocationPicker = new AlertDialog.Builder(mActivity);
        LinearLayout layout = new LinearLayout(mActivity);
        downLocationPicker.setTitle(getResources().getString(R.string.title_download_location));
        final EditText getDownload = new EditText(mActivity);
        getDownload.setText(mPreferences.getDownloadDirectory());

        int padding = Utils.dpToPx(10);

        TextView v = new TextView(mActivity);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        v.setTextColor(Color.DKGRAY);
        v.setText(Constants.EXTERNAL_STORAGE + '/');
        v.setPadding(padding, padding, 0, padding);
        layout.addView(v);
        layout.addView(getDownload);
        if (API < Build.VERSION_CODES.JELLY_BEAN) {
            layout.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.edit_text));
        } else {
            Drawable drawable;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                drawable = getResources().getDrawable(android.R.drawable.edit_text, getActivity().getTheme());
            } else {
                drawable = getResources().getDrawable(android.R.drawable.edit_text);
            }
            layout.setBackground(drawable);
        }
        downLocationPicker.setView(layout);
        downLocationPicker.setPositiveButton(getResources().getString(R.string.action_ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text = getDownload.getText().toString();
                        mPreferences.setDownloadDirectory(text);
                        downloadloc.setSummary(Constants.EXTERNAL_STORAGE + '/' + text);
                    }
                });
        downLocationPicker.show();
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
    public boolean onPreferenceClick(Preference preference) {
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
                if (!Utils.isFlashInstalled(mActivity) && cbFlash.isChecked()) {
                    Utils.createInformativeDialog(mActivity, R.string.title_warning, R.string.dialog_adobe_not_installed);
                    cbFlash.setEnabled(false);
                    mPreferences.setFlashSupport(0);
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
            case SETTINGS_GOOGLESUGGESTIONS:
                mPreferences.setGoogleSearchSuggestionsEnabled((Boolean) newValue);
                cbgooglesuggest.setChecked((Boolean) newValue);
                return true;
            case  SETTINGS_DRAWERTABS:
                mPreferences.setShowTabsInDrawer((Boolean) newValue);
                cbDrawerTabs.setChecked((Boolean) newValue);
            default:
                return false;
        }
    }
}
