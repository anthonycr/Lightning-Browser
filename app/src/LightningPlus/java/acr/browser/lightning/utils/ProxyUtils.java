package acr.browser.lightning.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import net.i2p.android.ui.I2PAndroidHelper;

import acr.browser.lightning.R;
import acr.browser.lightning.activity.BrowserApp;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.preference.PreferenceManager;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import info.guardianproject.netcipher.web.WebkitProxy;

/**
 * 6/4/2015 Anthony Restaino
 */
public class ProxyUtils {
    // Helper
    private static I2PAndroidHelper mI2PHelper;
    private static boolean mI2PHelperBound;
    private static boolean mI2PProxyInitialized;
    private static PreferenceManager mPreferences;
    private static ProxyUtils mInstance;

    private ProxyUtils(Context context) {
        mPreferences = PreferenceManager.getInstance();
        mI2PHelper = new I2PAndroidHelper(context);
    }

    public static ProxyUtils getInstance(@NonNull Context context) {
        if (mInstance == null) {
            mInstance = new ProxyUtils(context);
        }
        return mInstance;
    }

    /*
     * If Orbot/Tor or I2P is installed, prompt the user if they want to enable
	 * proxying for this session
	 */
    public void checkForProxy(final Activity activity) {
        boolean useProxy = mPreferences.getUseProxy();

        final boolean orbotInstalled = OrbotHelper.isOrbotInstalled(activity);
        boolean orbotChecked = mPreferences.getCheckedForTor();
        boolean orbot = orbotInstalled && !orbotChecked;

        boolean i2pInstalled = mI2PHelper.isI2PAndroidInstalled();
        boolean i2pChecked = mPreferences.getCheckedForI2P();
        boolean i2p = i2pInstalled && !i2pChecked;

        // TODO Is the idea to show this per-session, or only once?
        if (!useProxy && (orbot || i2p)) {
            if (orbot) mPreferences.setCheckedForTor(true);
            if (i2p) mPreferences.setCheckedForI2P(true);
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);

            if (orbotInstalled && i2pInstalled) {
                String[] proxyChoices = activity.getResources().getStringArray(R.array.proxy_choices_array);
                builder.setTitle(activity.getResources().getString(R.string.http_proxy))
                        .setSingleChoiceItems(proxyChoices, mPreferences.getProxyChoice(),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mPreferences.setProxyChoice(which);
                                    }
                                })
                        .setNeutralButton(activity.getResources().getString(R.string.action_ok),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (mPreferences.getUseProxy())
                                            initializeProxy(activity);
                                    }
                                });
            } else {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                mPreferences.setProxyChoice(orbotInstalled ?
                                        Constants.PROXY_ORBOT : Constants.PROXY_I2P);
                                initializeProxy(activity);
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                mPreferences.setProxyChoice(Constants.NO_PROXY);
                                break;
                        }
                    }
                };

                builder.setMessage(orbotInstalled ? R.string.use_tor_prompt : R.string.use_i2p_prompt)
                        .setPositiveButton(R.string.yes, dialogClickListener)
                        .setNegativeButton(R.string.no, dialogClickListener);
            }
            builder.show();
        }
    }

    /*
     * Initialize WebKit Proxying
	 */
    private void initializeProxy(Activity activity) {
        String host;
        int port;

        switch (mPreferences.getProxyChoice()) {
            case Constants.NO_PROXY:
                // We shouldn't be here
                return;

            case Constants.PROXY_ORBOT:
                if (!OrbotHelper.isOrbotRunning(activity))
                    OrbotHelper.requestStartTor(activity);
                host = "localhost";
                port = 8118;
                break;

            case Constants.PROXY_I2P:
                mI2PProxyInitialized = true;
                if (mI2PHelperBound && !mI2PHelper.isI2PAndroidRunning()) {
                    mI2PHelper.requestI2PAndroidStart(activity);
                }
                host = "localhost";
                port = 4444;
                break;

            default:
                host = mPreferences.getProxyHost();
                port = mPreferences.getProxyPort();
        }

        try {
            WebkitProxy.setProxy(BrowserApp.class.getName(), activity.getApplicationContext(), null, host, port);
        } catch (Exception e) {
            Log.d(Constants.TAG, "error enabling web proxying", e);
        }

    }

    public boolean isProxyReady(Activity activity) {
        if (mPreferences.getProxyChoice() == Constants.PROXY_I2P) {
            if (!mI2PHelper.isI2PAndroidRunning()) {
                Utils.showSnackbar(activity, R.string.i2p_not_running);
                return false;
            } else if (!mI2PHelper.areTunnelsActive()) {
                Utils.showSnackbar(activity, R.string.i2p_tunnels_not_ready);
                return false;
            }
        }

        return true;
    }

    public void updateProxySettings(Activity activity) {
        if (mPreferences.getUseProxy()) {
            initializeProxy(activity);
        } else {
            try {
                WebkitProxy.resetProxy(BrowserApp.class.getName(), activity.getApplicationContext());
            } catch (Exception e) {
                e.printStackTrace();
            }

            mI2PProxyInitialized = false;
        }
    }

    public void onStop() {
        mI2PHelper.unbind();
        mI2PHelperBound = false;
    }

    public void onStart(final Activity activity) {
        if (mPreferences.getProxyChoice() == Constants.PROXY_I2P) {
            // Try to bind to I2P Android
            mI2PHelper.bind(new I2PAndroidHelper.Callback() {
                @Override
                public void onI2PAndroidBound() {
                    mI2PHelperBound = true;
                    if (mI2PProxyInitialized && !mI2PHelper.isI2PAndroidRunning())
                        mI2PHelper.requestI2PAndroidStart(activity);
                }
            });
        }
    }

    public int setProxyChoice(int choice, Activity activity) {
        switch (choice) {
            case Constants.PROXY_ORBOT:
                if (!OrbotHelper.isOrbotInstalled(activity)) {
                    choice = Constants.NO_PROXY;
                    Utils.showSnackbar(activity, R.string.install_orbot);
                }
                break;

            case Constants.PROXY_I2P:
                I2PAndroidHelper ih = new I2PAndroidHelper(activity.getApplicationContext());
                if (!ih.isI2PAndroidInstalled()) {
                    choice = Constants.NO_PROXY;
                    ih.promptToInstall(activity);
                }
                break;
            case Constants.PROXY_MANUAL:
                break;
        }
        return choice;
    }
}
