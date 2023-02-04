package acr.browser.lightning.utils;

import android.app.Activity;
import android.app.Dialog;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import acr.browser.lightning.BrowserApp;
import acr.browser.lightning.R;
import acr.browser.lightning.browser.proxy.ProxyChoice;
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

    private final UserPreferences userPreferences;
    private final DeveloperPreferences developerPreferences;

    @Inject
    public ProxyUtils(UserPreferences userPreferences,
                      DeveloperPreferences developerPreferences) {
        this.userPreferences = userPreferences;
        this.developerPreferences = developerPreferences;
    }

    /*
     * If Orbot/Tor is installed, prompt the user if they want to enable
     * proxying for this session
     */
    public void checkForProxy(@NonNull final Activity activity) {
        final ProxyChoice currentProxyChoice = userPreferences.getProxyChoice();

        final boolean orbotInstalled = OrbotHelper.isOrbotInstalled(activity);
        boolean orbotChecked = developerPreferences.getCheckedForTor();
        boolean orbot = orbotInstalled && !orbotChecked;

        // Do only once per install
        if (currentProxyChoice != ProxyChoice.NONE && orbot) {
            developerPreferences.setCheckedForTor(true);
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);

            String[] proxyChoices = activity.getResources().getStringArray(R.array.proxy_choices_array);
            final List<ProxyChoice> values = Arrays.asList(ProxyChoice.NONE, ProxyChoice.ORBOT);
            final List<Pair<ProxyChoice, String>> list = new ArrayList<>();
            for (ProxyChoice proxyChoice : values) {
                list.add(new Pair<>(proxyChoice, proxyChoices[proxyChoice.getValue()]));
            }
            builder.setTitle(activity.getResources().getString(R.string.http_proxy));
            AlertDialogExtensionsKt.withSingleChoiceItems(builder, list, userPreferences.getProxyChoice(), newProxyChoice -> {
                userPreferences.setProxyChoice(newProxyChoice);
                return Unit.INSTANCE;
            });
            builder.setPositiveButton(activity.getResources().getString(R.string.action_ok),
                (dialog, which) -> {
                    if (userPreferences.getProxyChoice() != ProxyChoice.NONE) {
                        initializeProxy(activity);
                    }
                });
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

        switch (userPreferences.getProxyChoice()) {
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
            default:
            case MANUAL:
                host = userPreferences.getProxyHost();
                port = userPreferences.getProxyPort();
                break;
        }

        try {
            WebkitProxy.setProxy(BrowserApp.class.getName(), activity.getApplicationContext(), null, host, port);
        } catch (Exception e) {
            Log.d(TAG, "error enabling web proxying", e);
        }

    }

    public void updateProxySettings(@NonNull Activity activity) {
        if (userPreferences.getProxyChoice() != ProxyChoice.NONE) {
            initializeProxy(activity);
        } else {
            try {
                WebkitProxy.resetProxy(BrowserApp.class.getName(), activity.getApplicationContext());
            } catch (Exception e) {
                Log.e(TAG, "Unable to reset proxy", e);
            }
        }
    }

    public void onStart() {
    }

    public static ProxyChoice sanitizeProxyChoice(ProxyChoice choice, @NonNull Activity activity) {
        switch (choice) {
            case ORBOT:
                if (!OrbotHelper.isOrbotInstalled(activity)) {
                    choice = ProxyChoice.NONE;
                    ActivityExtensions.snackbar(activity, R.string.install_orbot);
                }
                break;
            case MANUAL:
                break;
        }
        return choice;
    }
}
