/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.webkit.WebView;

import com.anthonycr.bonsai.Completable;
import com.anthonycr.bonsai.CompletableAction;
import com.anthonycr.bonsai.CompletableOnSubscribe;
import com.anthonycr.bonsai.CompletableSubscriber;
import com.anthonycr.bonsai.Schedulers;

import acr.browser.lightning.R;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.dialog.BrowserDialog;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.utils.Utils;
import acr.browser.lightning.utils.WebUtils;
import acr.browser.lightning.view.LightningView;

public class PrivacySettingsFragment extends LightningPreferenceFragment implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final String SETTINGS_LOCATION = "location";
    private static final String SETTINGS_THIRDPCOOKIES = "third_party";
    private static final String SETTINGS_SAVEPASSWORD = "password";
    private static final String SETTINGS_CACHEEXIT = "clear_cache_exit";
    private static final String SETTINGS_HISTORYEXIT = "clear_history_exit";
    private static final String SETTINGS_COOKIEEXIT = "clear_cookies_exit";
    private static final String SETTINGS_CLEARCACHE = "clear_cache";
    private static final String SETTINGS_CLEARHISTORY = "clear_history";
    private static final String SETTINGS_CLEARCOOKIES = "clear_cookies";
    private static final String SETTINGS_CLEARWEBSTORAGE = "clear_webstorage";
    private static final String SETTINGS_WEBSTORAGEEXIT = "clear_webstorage_exit";
    private static final String SETTINGS_DONOTTRACK = "do_not_track";
    private static final String SETTINGS_IDENTIFYINGHEADERS = "remove_identifying_headers";

    private Activity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BrowserApp.getAppComponent().inject(this);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference_privacy);

        mActivity = getActivity();

        initPrefs();
    }

    private void initPrefs() {
        Preference clearcache = findPreference(SETTINGS_CLEARCACHE);
        Preference clearhistory = findPreference(SETTINGS_CLEARHISTORY);
        Preference clearcookies = findPreference(SETTINGS_CLEARCOOKIES);
        Preference clearwebstorage = findPreference(SETTINGS_CLEARWEBSTORAGE);

        CheckBoxPreference cblocation = (CheckBoxPreference) findPreference(SETTINGS_LOCATION);
        if(!mPreferenceManager.isContian(PreferenceManager.Name.LOCATION))
            mPreferenceManager.setLocationEnabled(cblocation.isChecked());

        CheckBoxPreference cb3cookies = (CheckBoxPreference) findPreference(SETTINGS_THIRDPCOOKIES);
        if(!mPreferenceManager.isContian(PreferenceManager.Name.BLOCK_THIRD_PARTY))
            mPreferenceManager.setBlockThirdPartyCookiesEnabled(cb3cookies.isChecked());

        CheckBoxPreference cbsavepasswords = (CheckBoxPreference) findPreference(SETTINGS_SAVEPASSWORD);
        if(!mPreferenceManager.isContian(PreferenceManager.Name.SAVE_PASSWORDS))
            mPreferenceManager.setSavePasswordsEnabled(cbsavepasswords.isChecked());

        CheckBoxPreference cbcacheexit = (CheckBoxPreference) findPreference(SETTINGS_CACHEEXIT);
        if(!mPreferenceManager.isContian(PreferenceManager.Name.CLEAR_CACHE_EXIT))
            mPreferenceManager.setClearCacheExit(cbcacheexit.isChecked());

        CheckBoxPreference cbhistoryexit = (CheckBoxPreference) findPreference(SETTINGS_HISTORYEXIT);
        if(!mPreferenceManager.isContian(PreferenceManager.Name.CLEAR_HISTORY_EXIT))
            mPreferenceManager.setClearHistoryExitEnabled(cbhistoryexit.isChecked());

        CheckBoxPreference cbcookiesexit = (CheckBoxPreference) findPreference(SETTINGS_COOKIEEXIT);
        if(!mPreferenceManager.isContian(PreferenceManager.Name.CLEAR_COOKIES_EXIT))
            mPreferenceManager.setClearCookiesExitEnabled(cbcookiesexit.isChecked());

        CheckBoxPreference cbwebstorageexit = (CheckBoxPreference) findPreference(SETTINGS_WEBSTORAGEEXIT);
        if(!mPreferenceManager.isContian(PreferenceManager.Name.CLEAR_WEBSTORAGE_EXIT))
            mPreferenceManager.setClearWebStorageExitEnabled(cbwebstorageexit.isChecked());

        CheckBoxPreference cbDoNotTrack = (CheckBoxPreference) findPreference(SETTINGS_DONOTTRACK);
        if(!mPreferenceManager.isContian(PreferenceManager.Name.DO_NOT_TRACK))
            mPreferenceManager.setDoNotTrackEnabled(cbDoNotTrack.isChecked());

        CheckBoxPreference cbIdentifyingHeaders = (CheckBoxPreference) findPreference(SETTINGS_IDENTIFYINGHEADERS);
        if(!mPreferenceManager.isContian(PreferenceManager.Name.IDENTIFYING_HEADERS))
            mPreferenceManager.setRemoveIdentifyingHeadersEnabled(cbIdentifyingHeaders.isChecked());

        clearcache.setOnPreferenceClickListener(this);
        clearhistory.setOnPreferenceClickListener(this);
        clearcookies.setOnPreferenceClickListener(this);
        clearwebstorage.setOnPreferenceClickListener(this);

        cblocation.setOnPreferenceChangeListener(this);
        cb3cookies.setOnPreferenceChangeListener(this);
        cbsavepasswords.setOnPreferenceChangeListener(this);
        cbcacheexit.setOnPreferenceChangeListener(this);
        cbhistoryexit.setOnPreferenceChangeListener(this);
        cbcookiesexit.setOnPreferenceChangeListener(this);
        cbwebstorageexit.setOnPreferenceChangeListener(this);
        cbDoNotTrack.setOnPreferenceChangeListener(this);
        cbIdentifyingHeaders.setOnPreferenceChangeListener(this);

        cblocation.setChecked(mPreferenceManager.getLocationEnabled());
        cbsavepasswords.setChecked(mPreferenceManager.getSavePasswordsEnabled());
        cbcacheexit.setChecked(mPreferenceManager.getClearCacheExit());
        cbhistoryexit.setChecked(mPreferenceManager.getClearHistoryExitEnabled());
        cbcookiesexit.setChecked(mPreferenceManager.getClearCookiesExitEnabled());
        cb3cookies.setChecked(mPreferenceManager.getBlockThirdPartyCookiesEnabled());
        cbwebstorageexit.setChecked(mPreferenceManager.getClearWebStorageExitEnabled());
        cbDoNotTrack.setChecked(mPreferenceManager.getDoNotTrackEnabled() && Utils.doesSupportHeaders());
        cbIdentifyingHeaders.setChecked(mPreferenceManager.getRemoveIdentifyingHeadersEnabled() && Utils.doesSupportHeaders());

        cbDoNotTrack.setEnabled(Utils.doesSupportHeaders());
        cbIdentifyingHeaders.setEnabled(Utils.doesSupportHeaders());

        String identifyingHeadersSummary = LightningView.HEADER_REQUESTED_WITH + ", " + LightningView.HEADER_WAP_PROFILE;
        cbIdentifyingHeaders.setSummary(identifyingHeadersSummary);

        cb3cookies.setEnabled(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);

    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
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
            case SETTINGS_CLEARWEBSTORAGE:
                clearWebStorage();
                return true;
            default:
                return false;
        }
    }

    private void clearHistoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(getResources().getString(R.string.title_clear_history));
        Dialog dialog = builder.setMessage(getResources().getString(R.string.dialog_history))
            .setPositiveButton(getResources().getString(R.string.action_yes),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        clearHistory()
                            .subscribeOn(Schedulers.io())
                            .observeOn(Schedulers.main())
                            .subscribe(new CompletableOnSubscribe() {
                                @Override
                                public void onComplete() {
                                    Utils.showSnackbar(getActivity(), R.string.message_clear_history);
                                }
                            });
                    }
                })
            .setNegativeButton(getResources().getString(R.string.action_no), null).show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }

    private void clearCookiesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(getResources().getString(R.string.title_clear_cookies));
        builder.setMessage(getResources().getString(R.string.dialog_cookies))
            .setPositiveButton(getResources().getString(R.string.action_yes),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        clearCookies()
                            .subscribeOn(Schedulers.io())
                            .observeOn(Schedulers.main())
                            .subscribe(new CompletableOnSubscribe() {
                                @Override
                                public void onComplete() {
                                    Utils.showSnackbar(getActivity(), R.string.message_cookies_cleared);
                                }
                            });
                    }
                })
            .setNegativeButton(getResources().getString(R.string.action_no), null).show();
    }

    private void clearCache() {
        WebView webView = new WebView(mActivity);
        webView.clearCache(true);
        webView.destroy();
        Utils.showSnackbar(mActivity, R.string.message_cache_cleared);
    }

    @NonNull
    private Completable clearHistory() {
        return Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                Activity activity = getActivity();
                if (activity != null) {
                    WebUtils.clearHistory(activity);
                    subscriber.onComplete();
                }
                subscriber.onError(new RuntimeException("Activity was null in clearHistory"));
            }
        });
    }

    @NonNull
    private Completable clearCookies() {
        return Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                Activity activity = getActivity();
                if (activity != null) {
                    WebUtils.clearCookies(activity);
                    subscriber.onComplete();
                }
                subscriber.onError(new RuntimeException("Activity was null in clearCookies"));
            }
        });
    }

    private void clearWebStorage() {
        WebUtils.clearWebStorage();
        Utils.showSnackbar(getActivity(), R.string.message_web_storage_cleared);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case SETTINGS_LOCATION:
                mPreferenceManager.setLocationEnabled((Boolean) newValue);
                return true;
            case SETTINGS_THIRDPCOOKIES:
                mPreferenceManager.setBlockThirdPartyCookiesEnabled((Boolean) newValue);
                return true;
            case SETTINGS_SAVEPASSWORD:
                mPreferenceManager.setSavePasswordsEnabled((Boolean) newValue);
                return true;
            case SETTINGS_CACHEEXIT:
                mPreferenceManager.setClearCacheExit((Boolean) newValue);
                return true;
            case SETTINGS_HISTORYEXIT:
                mPreferenceManager.setClearHistoryExitEnabled((Boolean) newValue);
                return true;
            case SETTINGS_COOKIEEXIT:
                mPreferenceManager.setClearCookiesExitEnabled((Boolean) newValue);
                return true;
            case SETTINGS_WEBSTORAGEEXIT:
                mPreferenceManager.setClearWebStorageExitEnabled((Boolean) newValue);
                return true;
            case SETTINGS_DONOTTRACK:
                mPreferenceManager.setDoNotTrackEnabled((Boolean) newValue);
                return true;
            case SETTINGS_IDENTIFYINGHEADERS:
                mPreferenceManager.setRemoveIdentifyingHeadersEnabled((Boolean) newValue);
                return true;
            default:
                return false;
        }
    }
}
