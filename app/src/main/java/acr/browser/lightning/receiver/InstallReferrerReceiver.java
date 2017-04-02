package acr.browser.lightning.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.net.URLDecoder;
import java.util.regex.Pattern;

import javax.inject.Inject;

import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.preference.PreferenceManager;

/**
 * Created by Viacheslav Titov on 08.02.2017.
 */

public class InstallReferrerReceiver extends BroadcastReceiver {

    private static String TAG = InstallReferrerReceiver.class.getSimpleName();
    private static final Pattern UTF8_STRRIP_SPACE = Pattern.compile("%(?![0-9a-fA-F]{2})");
    private static final Pattern UTF8_STRRIP_PLUS = Pattern.compile("\\+");

    @Inject
    PreferenceManager mPreferenceManager;

    public void onReceive(Context context, Intent intent) {
        if (this==null || intent==null || context==null){
            return;
        }
        BrowserApp.getAppComponent().inject(this);
        Log.i(TAG, "Received referrer");
        try {
            String referrer = "";
            String appId = "";
            // Make sure this is the intent we expect - it always should be.
            if ((null != intent) && (intent.getAction().equals("com.android.vending.INSTALL_REFERRER"))) {
                // This intent should have a referrer string attached to it.
                String rawReferrer = intent.getStringExtra("referrer");
                appId = intent.getStringExtra("id");
                if (null != rawReferrer) {
                    // The string is usually URL Encoded, so we need to decode it.
                    rawReferrer = UTF8_STRRIP_SPACE.matcher(rawReferrer).replaceAll("%25");
                    rawReferrer = UTF8_STRRIP_PLUS.matcher(rawReferrer).replaceAll("%2B");
                    referrer = URLDecoder.decode(rawReferrer, "UTF-8");
                    mPreferenceManager.setReferrer(referrer);
                } else {
                    mPreferenceManager.setReferrer(referrer);
                }
                if (appId != null) {
                    mPreferenceManager.setReferrerAppId(appId);
                } else {
                    mPreferenceManager.setReferrerAppId("");
                }
            } else {
                mPreferenceManager.setReferrer(referrer);
                mPreferenceManager.setReferrerAppId(appId);
            }
        } catch (Exception e) {
            Log.e(TAG, "ReferrerReceiver catch" + e.getMessage());
        }
    }
}
