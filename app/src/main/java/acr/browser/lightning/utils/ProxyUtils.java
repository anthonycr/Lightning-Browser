package acr.browser.lightning.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.util.Log;

import net.i2p.android.ui.I2PAndroidHelper;

import javax.inject.Inject;
import javax.inject.Singleton;

import acr.browser.lightning.BrowserApp;
import acr.browser.lightning.R;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.constant.Proxy;
import acr.browser.lightning.dialog.BrowserDialog;
import acr.browser.lightning.extensions.ActivityExtensions;
import acr.browser.lightning.preference.DeveloperPreferences;
import acr.browser.lightning.preference.UserPreferences;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import info.guardianproject.netcipher.webkit.WebkitProxy;

@Singleton
public final class ProxyUtils {

    private static final String TAG = "ProxyUtils";

    // Helper
    private static boolean sI2PHelperBound;
    private static boolean sI2PProxyInitialized;

    @Inject UserPreferences mUserPreferences;
    @Inject DeveloperPreferences mDeveloperPreferences;
    @Inject I2PAndroidHelper mI2PHelper;

    @Inject
    public ProxyUtils() {
        BrowserApp.getAppComponent().inject(this);
    }

    /*
     * If Orbot/Tor or I2P is installed, prompt the user if they want to enable
     * proxying for this session
     */
    public void checkForProxy(@NonNull final Activity activity) {
        int proxyChoice = mUserPreferences.getProxyChoice();

        final boolean orbotInstalled = OrbotHelper.isOrbotInstalled(activity);
        boolean orbotChecked = mDeveloperPreferences.getCheckedForTor();
        boolean orbot = orbotInstalled && !orbotChecked;

        boolean i2pInstalled = mI2PHelper.isI2PAndroidInstalled();
        boolean i2pChecked = mDeveloperPreferences.getCheckedForI2P();
        boolean i2p = i2pInstalled && !i2pChecked;

        // TODO Is the idea to show this per-session, or only once?
        if (proxyChoice != Constants.NO_PROXY && (orbot || i2p)) {
            if (orbot) {
                mDeveloperPreferences.setCheckedForTor(true);
            }
            if (i2p) {
                mDeveloperPreferences.setCheckedForI2P(true);
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);

            if (orbotInstalled && i2pInstalled) {
                String[] proxyChoices = activity.getResources().getStringArray(R.array.proxy_choices_array);
                builder.setTitle(activity.getResources().getString(R.string.http_proxy))
                    .setSingleChoiceItems(proxyChoices, mUserPreferences.getProxyChoice(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mUserPreferences.setProxyChoice(which);
                            }
                        })
                    .setPositiveButton(activity.getResources().getString(R.string.action_ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mUserPreferences.getProxyChoice() != Constants.NO_PROXY) {
                                    initializeProxy(activity);
                                }
                            }
                        });
            } else {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                mUserPreferences.setProxyChoice(orbotInstalled ?
                                    Constants.PROXY_ORBOT : Constants.PROXY_I2P);
                                initializeProxy(activity);
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                mUserPreferences.setProxyChoice(Constants.NO_PROXY);
                                break;
                        }
                    }
                };

                builder.setMessage(orbotInstalled ? R.string.use_tor_prompt : R.string.use_i2p_prompt)
                    .setPositiveButton(R.string.yes, dialogClickListener)
                    .setNegativeButton(R.string.no, dialogClickListener);
            }
            Dialog dialog = builder.show();
            BrowserDialog.setDialogSize(activity, dialog);
        }
    }

    /*
     * Initialize WebKit Proxying
     */
    private void initializeProxy(@NonNull Activity activity) {
        String host;
        int port;

        switch (mUserPreferences.getProxyChoice()) {
            case Constants.NO_PROXY:
                // We shouldn't be here
                return;
            case Constants.PROXY_ORBOT:
                if (!OrbotHelper.isOrbotRunning(activity)) {
                    OrbotHelper.requestStartTor(activity);
                }
                host = "localhost";
                port = 8118;
                break;
            case Constants.PROXY_I2P:
                sI2PProxyInitialized = true;
                if (sI2PHelperBound && !mI2PHelper.isI2PAndroidRunning()) {
                    mI2PHelper.requestI2PAndroidStart(activity);
                }
                host = "localhost";
                port = 4444;
                break;
            default:
                host = mUserPreferences.getProxyHost();
                port = mUserPreferences.getProxyPort();
                break;
            case Constants.PROXY_MANUAL:
                host = mUserPreferences.getProxyHost();
                port = mUserPreferences.getProxyPort();
                break;
        }

        try {
            WebkitProxy.setProxy(BrowserApp.class.getName(), activity.getApplicationContext(), null, host, port);
        } catch (Exception e) {
            Log.d(TAG, "error enabling web proxying", e);
        }

    }

    public boolean isProxyReady(@NonNull Activity activity) {
        if (mUserPreferences.getProxyChoice() == Constants.PROXY_I2P) {
            if (!mI2PHelper.isI2PAndroidRunning()) {
                ActivityExtensions.snackbar(activity, R.string.i2p_not_running);
                return false;
            } else if (!mI2PHelper.areTunnelsActive()) {
                ActivityExtensions.snackbar(activity, R.string.i2p_tunnels_not_ready);
                return false;
            }
        }

        return true;
    }

    public void updateProxySettings(@NonNull Activity activity) {
        if (mUserPreferences.getProxyChoice() != Constants.NO_PROXY) {
            initializeProxy(activity);
        } else {
            try {
                WebkitProxy.resetProxy(BrowserApp.class.getName(), activity.getApplicationContext());
            } catch (Exception e) {
                Log.e(TAG, "Unable to reset proxy", e);
            }

            sI2PProxyInitialized = false;
        }
    }

    public void onStop() {
        mI2PHelper.unbind();
        sI2PHelperBound = false;
    }

    public void onStart(final Activity activity) {
        if (mUserPreferences.getProxyChoice() == Constants.PROXY_I2P) {
            // Try to bind to I2P Android
            mI2PHelper.bind(new I2PAndroidHelper.Callback() {
                @Override
                public void onI2PAndroidBound() {
                    sI2PHelperBound = true;
                    if (sI2PProxyInitialized && !mI2PHelper.isI2PAndroidRunning())
                        mI2PHelper.requestI2PAndroidStart(activity);
                }
            });
        }
    }

    @Proxy
    public static int sanitizeProxyChoice(int choice, @NonNull Activity activity) {
        switch (choice) {
            case Constants.PROXY_ORBOT:
                if (!OrbotHelper.isOrbotInstalled(activity)) {
                    choice = Constants.NO_PROXY;
                    ActivityExtensions.snackbar(activity, R.string.install_orbot);
                }
                break;
            case Constants.PROXY_I2P:
                I2PAndroidHelper ih = new I2PAndroidHelper(activity.getApplication());
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
