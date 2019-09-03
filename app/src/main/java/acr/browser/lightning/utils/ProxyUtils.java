package acr.browser.lightning.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.util.Log;

import net.i2p.android.ui.I2PAndroidHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import acr.browser.lightning.BrowserApp;
import acr.browser.lightning.R;
import acr.browser.lightning.browser.ProxyChoice;
import acr.browser.lightning.dialog.BrowserDialog;
import acr.browser.lightning.extensions.ActivityExtensions;
import acr.browser.lightning.extensions.AlertDialogExtensionsKt;
import acr.browser.lightning.preference.DeveloperPreferences;
import acr.browser.lightning.preference.UserPreferences;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import info.guardianproject.netcipher.webkit.WebkitProxy;
import kotlin.Pair;
import kotlin.Unit;

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
        final ProxyChoice currentProxyChoice = mUserPreferences.getProxyChoice();

        final boolean orbotInstalled = OrbotHelper.isOrbotInstalled(activity);
        boolean orbotChecked = mDeveloperPreferences.getCheckedForTor();
        boolean orbot = orbotInstalled && !orbotChecked;

        boolean i2pInstalled = mI2PHelper.isI2PAndroidInstalled();
        boolean i2pChecked = mDeveloperPreferences.getCheckedForI2P();
        boolean i2p = i2pInstalled && !i2pChecked;

        // Do only once per install
        if (currentProxyChoice != ProxyChoice.NONE && (orbot || i2p)) {
            if (orbot) {
                mDeveloperPreferences.setCheckedForTor(true);
            }
            if (i2p) {
                mDeveloperPreferences.setCheckedForI2P(true);
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);

            if (orbotInstalled && i2pInstalled) {
                String[] proxyChoices = activity.getResources().getStringArray(R.array.proxy_choices_array);
                final List<ProxyChoice> values = Arrays.asList(ProxyChoice.NONE, ProxyChoice.ORBOT, ProxyChoice.I2P);
                final List<Pair<ProxyChoice, String>> list = new ArrayList<>();
                for (ProxyChoice proxyChoice : values) {
                    list.add(new Pair<>(proxyChoice, proxyChoices[proxyChoice.getValue()]));
                }
                builder.setTitle(activity.getResources().getString(R.string.http_proxy));
                AlertDialogExtensionsKt.withSingleChoiceItems(builder, list, mUserPreferences.getProxyChoice(), newProxyChoice -> {
                    mUserPreferences.setProxyChoice(newProxyChoice);
                    return Unit.INSTANCE;
                });
                builder.setPositiveButton(activity.getResources().getString(R.string.action_ok),
                    (dialog, which) -> {
                        if (mUserPreferences.getProxyChoice() != ProxyChoice.NONE) {
                            initializeProxy(activity);
                        }
                    });
            } else {
                DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            mUserPreferences.setProxyChoice(orbotInstalled
                                ? ProxyChoice.ORBOT
                                : ProxyChoice.I2P);
                            initializeProxy(activity);
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            mUserPreferences.setProxyChoice(ProxyChoice.NONE);
                            break;
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
            case NONE:
                // We shouldn't be here
                return;
            case ORBOT:
                if (!OrbotHelper.isOrbotRunning(activity)) {
                    OrbotHelper.requestStartTor(activity);
                }
                host = "localhost";
                port = 8118;
                break;
            case I2P:
                sI2PProxyInitialized = true;
                if (sI2PHelperBound && !mI2PHelper.isI2PAndroidRunning()) {
                    mI2PHelper.requestI2PAndroidStart(activity);
                }
                host = "localhost";
                port = 4444;
                break;
            default:
            case MANUAL:
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
        if (mUserPreferences.getProxyChoice() == ProxyChoice.I2P) {
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
        if (mUserPreferences.getProxyChoice() != ProxyChoice.NONE) {
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
        if (mUserPreferences.getProxyChoice() == ProxyChoice.I2P) {
            // Try to bind to I2P Android
            mI2PHelper.bind(() -> {
                sI2PHelperBound = true;
                if (sI2PProxyInitialized && !mI2PHelper.isI2PAndroidRunning())
                    mI2PHelper.requestI2PAndroidStart(activity);
            });
        }
    }

    public static ProxyChoice sanitizeProxyChoice(ProxyChoice choice, @NonNull Activity activity) {
        switch (choice) {
            case ORBOT:
                if (!OrbotHelper.isOrbotInstalled(activity)) {
                    choice = ProxyChoice.NONE;
                    ActivityExtensions.snackbar(activity, R.string.install_orbot);
                }
                break;
            case I2P:
                I2PAndroidHelper ih = new I2PAndroidHelper(activity.getApplication());
                if (!ih.isI2PAndroidInstalled()) {
                    choice = ProxyChoice.NONE;
                    ih.promptToInstall(activity);
                }
                break;
            case MANUAL:
                break;
        }
        return choice;
    }
}
