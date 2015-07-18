/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Browser;
import android.support.v7.app.AlertDialog;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebIconDatabase;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewDatabase;

import acr.browser.lightning.R;
import acr.browser.lightning.database.HistoryDatabase;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.utils.Utils;

public class PrivacySettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final String SETTINGS_LOCATION = "location";
    private static final String SETTINGS_THIRDPCOOKIES = "third_party";
    private static final String SETTINGS_SAVEPASSWORD = "password";
    private static final String SETTINGS_CACHEEXIT = "clear_cache_exit";
    private static final String SETTINGS_HISTORYEXIT = "clear_history_exit";
    private static final String SETTINGS_COOKIEEXIT = "clear_cookies_exit";
    private static final String SETTINGS_SYNCHISTORY = "sync_history";
    private static final String SETTINGS_CLEARCACHE = "clear_cache";
    private static final String SETTINGS_CLEARHISTORY = "clear_history";
    private static final String SETTINGS_CLEARCOOKIES = "clear_cookies";

    private static final int API = android.os.Build.VERSION.SDK_INT;
    private Activity mActivity;
    private PreferenceManager mPreferences;
    private CheckBoxPreference cblocation, cb3cookies, cbsavepasswords, cbcacheexit, cbhistoryexit,
            cbcookiesexit, cbsynchistory;
    private boolean mSystemBrowser;
    private Handler messageHandler;

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
        mSystemBrowser = mPreferences.getSystemBrowserPresent();

        Preference clearcache = findPreference(SETTINGS_CLEARCACHE);
        Preference clearhistory = findPreference(SETTINGS_CLEARHISTORY);
        Preference clearcookies = findPreference(SETTINGS_CLEARCOOKIES);
        cblocation = (CheckBoxPreference) findPreference(SETTINGS_LOCATION);
        cb3cookies = (CheckBoxPreference) findPreference(SETTINGS_THIRDPCOOKIES);
        cbsavepasswords = (CheckBoxPreference) findPreference(SETTINGS_SAVEPASSWORD);
        cbcacheexit = (CheckBoxPreference) findPreference(SETTINGS_CACHEEXIT);
        cbhistoryexit = (CheckBoxPreference) findPreference(SETTINGS_HISTORYEXIT);
        cbcookiesexit = (CheckBoxPreference) findPreference(SETTINGS_COOKIEEXIT);
        cbsynchistory = (CheckBoxPreference) findPreference(SETTINGS_SYNCHISTORY);

        clearcache.setOnPreferenceClickListener(this);
        clearhistory.setOnPreferenceClickListener(this);
        clearcookies.setOnPreferenceClickListener(this);
        cblocation.setOnPreferenceChangeListener(this);
        cb3cookies.setOnPreferenceChangeListener(this);
        cbsavepasswords.setOnPreferenceChangeListener(this);
        cbcacheexit.setOnPreferenceChangeListener(this);
        cbhistoryexit.setOnPreferenceChangeListener(this);
        cbcookiesexit.setOnPreferenceChangeListener(this);
        cbsynchistory.setOnPreferenceChangeListener(this);

        cblocation.setChecked(mPreferences.getLocationEnabled());
        cbsavepasswords.setChecked(mPreferences.getSavePasswordsEnabled());
        cbcacheexit.setChecked(mPreferences.getClearCacheExit());
        cbhistoryexit.setChecked(mPreferences.getClearHistoryExitEnabled());
        cbcookiesexit.setChecked(mPreferences.getClearCookiesExitEnabled());
        cb3cookies.setChecked(mPreferences.getBlockThirdPartyCookiesEnabled());

        cb3cookies.setEnabled(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);

        if (!mSystemBrowser) {
            cbsynchistory.setChecked(false);
            cbsynchistory.setEnabled(false);
            cbsynchistory.setSummary(getResources().getString(R.string.stock_browser_unavailable));
        } else {
            cbsynchistory.setEnabled(true);
            cbsynchistory.setChecked(mPreferences.getSyncHistoryEnabled());
            cbsynchistory.setSummary(getResources().getString(R.string.stock_browser_available));
        }

        messageHandler = new MessageHandler(mActivity);
    }

    private static class MessageHandler extends Handler {

        final Context mHandlerContext;

        public MessageHandler(Context context) {
            this.mHandlerContext = context;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Utils.showToast(mHandlerContext, mHandlerContext.getResources()
                            .getString(R.string.message_clear_history));
                    break;
                case 2:
                    Utils.showToast(mHandlerContext, mHandlerContext.getResources().getString(
                            R.string.message_cookies_cleared));
                    break;
            }
            super.handleMessage(msg);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case SETTINGS_CLEARCACHE:
                clearCache();
                return true;
            case SETTINGS_CLEARHISTORY:
                clearHistoryDialog();
                return true;
            case SETTINGS_CLEARCOOKIES:
                clearCookiesDialog();
                return true;
            default:
                return false;
        }
    }

    private void clearHistoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(getResources().getString(R.string.title_clear_history));
        builder.setMessage(getResources().getString(R.string.dialog_history))
                .setPositiveButton(getResources().getString(R.string.action_yes),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                Thread clear = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        clearHistory();
                                    }
                                });
                                clear.start();
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.action_no),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                            }
                        }).show();
    }

    private void clearCookiesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(getResources().getString(R.string.title_clear_cookies));
        builder.setMessage(getResources().getString(R.string.dialog_cookies))
                .setPositiveButton(getResources().getString(R.string.action_yes),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                Thread clear = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        clearCookies();
                                    }
                                });
                                clear.start();
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.action_no),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                            }
                        }).show();
    }

    private void clearCache() {
        WebView webView = new WebView(mActivity);
        webView.clearCache(true);
        webView.destroy();
        Utils.showToast(mActivity, getResources().getString(R.string.message_cache_cleared));
    }

    @SuppressWarnings("deprecation")
    private void clearHistory() {
        mActivity.deleteDatabase(HistoryDatabase.DATABASE_NAME);
        WebViewDatabase m = WebViewDatabase.getInstance(mActivity);
        m.clearFormData();
        m.clearHttpAuthUsernamePassword();
        if (API < 18) {
            m.clearUsernamePassword();
            WebIconDatabase.getInstance().removeAllIcons();
        }
        if (mSystemBrowser) {
            try {
                Browser.clearHistory(mActivity.getContentResolver());
            } catch (Exception ignored) {
            }
        }
        Utils.trimCache(mActivity);
        messageHandler.sendEmptyMessage(1);
    }

    @SuppressWarnings("deprecation")
    private void clearCookies() {
        // TODO Break out web storage deletion into its own option/action
        // TODO clear web storage for all sites that are visited in Incognito mode
        WebStorage storage = WebStorage.getInstance();
        storage.deleteAllData();
        CookieManager c = CookieManager.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            c.removeAllCookies(null);
        } else {
            CookieSyncManager.createInstance(mActivity);
            c.removeAllCookie();
        }
        messageHandler.sendEmptyMessage(2);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // switch preferences
        switch (preference.getKey()) {
            case SETTINGS_LOCATION:
                mPreferences.setLocationEnabled((Boolean) newValue);
                cblocation.setChecked((Boolean) newValue);
                return true;
            case SETTINGS_THIRDPCOOKIES:
                mPreferences.setBlockThirdPartyCookiesEnabled((Boolean) newValue);
                cb3cookies.setChecked((Boolean) newValue);
                return true;
            case SETTINGS_SAVEPASSWORD:
                mPreferences.setSavePasswordsEnabled((Boolean) newValue);
                cbsavepasswords.setChecked((Boolean) newValue);
                return true;
            case SETTINGS_CACHEEXIT:
                mPreferences.setClearCacheExit((Boolean) newValue);
                cbcacheexit.setChecked((Boolean) newValue);
                return true;
            case SETTINGS_HISTORYEXIT:
                mPreferences.setClearHistoryExitEnabled((Boolean) newValue);
                cbhistoryexit.setChecked((Boolean) newValue);
                return true;
            case SETTINGS_COOKIEEXIT:
                mPreferences.setClearCookiesExitEnabled((Boolean) newValue);
                cbcookiesexit.setChecked((Boolean) newValue);
                return true;
            case SETTINGS_SYNCHISTORY:
                mPreferences.setSyncHistoryEnabled((Boolean) newValue);
                cbsynchistory.setChecked((Boolean) newValue);
                return true;
            default:
                return false;
        }
    }
}
