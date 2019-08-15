package net.i2p.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import net.i2p.android.lib.helper.R;
import net.i2p.android.router.service.IRouterState;
import net.i2p.android.router.service.State;

/**
 * @author str4d
 * @since 0.2
 */
public class I2PAndroidHelper {
    public static final String URI_I2P_ANDROID = "net.i2p.android";
    public static final String URI_I2P_ANDROID_DONATE = "net.i2p.android.donate";
    public static final String URI_I2P_ANDROID_LEGACY = "net.i2p.android.router";
    public static final String URI_I2P_ANDROID_DEBUG = "net.i2p.android.debug";

    public static final int REQUEST_START_I2P = 9857;

    private static final String ROUTER_SERVICE_CLASS = "net.i2p.android.router.service.RouterService";

    private static final String LOG_TAG = "I2PHelperLib";

    public interface Callback {
        void onI2PAndroidBound();
    }

    private final Context mContext;
    private final boolean mUseDebug;
    private boolean mTriedBindState;
    private IRouterState mStateService;
    private Callback mCallback;

    public I2PAndroidHelper(Context context) {
        mContext = context;
        mUseDebug = false;
    }

    /**
     * @param useDebug Enable usage against debug builds of I2P Android.
     */
    public I2PAndroidHelper(Context context, boolean useDebug) {
        mContext = context;
        mUseDebug = useDebug;
    }

    /**
     * Try to bind to I2P Android. Call this method from
     * {@link android.app.Activity#onStart()}.
     *
     * @param callback will be called once the helper has bound to the I2P app.
     * @since 0.7
     */
    public void bind(Callback callback) {
        mCallback = callback;
        bind();
    }

    /**
     * Try to bind to I2P Android. Call this method from
     * {@link android.app.Activity#onStart()}.
     * <p/>
     * If you need to be notified as soon as the helper has been bound to the
     * I2P app, use {@link I2PAndroidHelper#bind(Callback)} instead.
     */
    public void bind() {
        Log.i(LOG_TAG, "Binding to I2P Android");
        Intent i2pIntent = getI2PAndroidIntent();
        if (i2pIntent != null) {
            Log.i(LOG_TAG, i2pIntent.toString());
            try {
                mTriedBindState = mContext.bindService(
                        i2pIntent, mStateConnection, Context.BIND_AUTO_CREATE);
                if (!mTriedBindState)
                    Log.w(LOG_TAG, "Could not bind: bindService failed");
            } catch (SecurityException e) {
                // Old version of I2P Android (pre-0.9.13), cannot use
                mStateService = null;
                mTriedBindState = false;
                Log.w(LOG_TAG, "Could not bind: I2P Android version is too old");
            }
        } else
            Log.w(LOG_TAG, "Could not bind: I2P Android not installed");
    }

    /**
     * Try to bind to I2P Android, using the provided ServiceConnection and
     * flags. Call this method from
     * {@link android.app.Service#onStartCommand(android.content.Intent, int, int)}.
     * The ServiceConnection will be provided with an {@link android.os.IBinder}
     * that can be converted to an
     * {@link net.i2p.android.router.service.IRouterState} with
     * <code>IRouterState.Stub.asInterface(IBinder)</code>.
     */
    public boolean bind(ServiceConnection serviceConnection, int flags) {
        Log.i(LOG_TAG, "Binding to I2P Android with provided ServiceConnection");
        Intent i2pIntent = getI2PAndroidIntent();
        if (i2pIntent != null) {
            Log.i(LOG_TAG, i2pIntent.toString());
            try {
                boolean rv = mContext.bindService(
                        i2pIntent, serviceConnection, flags);
                if (!rv)
                    Log.w(LOG_TAG, "Could not bind: bindService failed");
                return rv;
            } catch (SecurityException e) {
                // Old version of I2P Android (pre-0.9.13), cannot use
                Log.w(LOG_TAG, "Could not bind: I2P Android version is too old");
                return false;
            }
        } else {
            Log.w(LOG_TAG, "Could not bind: I2P Android not installed");
            return false;
        }
    }

    private Intent getI2PAndroidIntent() {
        Intent intent = new Intent("net.i2p.android.router.service.IRouterState");
        if (isAppInstalled(URI_I2P_ANDROID))
            intent.setClassName(URI_I2P_ANDROID, ROUTER_SERVICE_CLASS);
        else if (isAppInstalled(URI_I2P_ANDROID_DONATE))
            intent.setClassName(URI_I2P_ANDROID_DONATE, ROUTER_SERVICE_CLASS);
        else if (isAppInstalled(URI_I2P_ANDROID_LEGACY))
            intent.setClassName(URI_I2P_ANDROID_LEGACY, ROUTER_SERVICE_CLASS);
        else
            intent = null;
        if (mUseDebug && isAppInstalled(URI_I2P_ANDROID_DEBUG)) {
            Log.w(LOG_TAG, "Using debug build of I2P Android");
            intent.setClassName(URI_I2P_ANDROID_DEBUG, ROUTER_SERVICE_CLASS);
        }
        return intent;
    }

    /**
     * Unbind from I2P Android. Call this method from
     * {@link android.app.Activity#onStop()}.
     */
    public void unbind() {
        if (mTriedBindState)
            mContext.unbindService(mStateConnection);
        mTriedBindState = false;
        mCallback = null;
    }

    private final ServiceConnection mStateConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mStateService = IRouterState.Stub.asInterface(service);
            Log.i(LOG_TAG, "Bound to I2P Android");
            if (mCallback != null)
                mCallback.onI2PAndroidBound();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            Log.w(LOG_TAG, "I2P Android disconnected unexpectedly");
            mStateService = null;
        }
    };

    /**
     * Check if I2P Android is installed.
     *
     * @return true if I2P Android is installed, false otherwise.
     */
    public boolean isI2PAndroidInstalled() {
        return (mUseDebug && isAppInstalled(URI_I2P_ANDROID_DEBUG)) ||
                isAppInstalled(URI_I2P_ANDROID) ||
                isAppInstalled(URI_I2P_ANDROID_DONATE) ||
                isAppInstalled(URI_I2P_ANDROID_LEGACY);
    }

    private boolean isAppInstalled(String uri) {
        PackageManager pm = mContext.getPackageManager();
        boolean installed;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            installed = false;
        }
        return installed;
    }

    /**
     * Show dialog - install I2P Android from market or F-Droid.
     *
     * @param activity the Activity this method has been called from.
     */
    public void promptToInstall(final Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.install_i2p_android)
                .setMessage(R.string.you_must_have_i2p_android)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String uriMarket = activity.getString(R.string.market_i2p_android);
                        Uri uri = Uri.parse(uriMarket);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        activity.startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                });
        builder.show();
    }

    /**
     * Check if I2P Android is running. If {@link I2PAndroidHelper#bind()}
     * has not been called previously, this will always return false.
     * <p/>
     * Do not call this from {@link Activity#onResume()}. The Android lifecycle
     * calls that method before binding the helper to the I2P app, so this will
     * always return false. Use {@link I2PAndroidHelper#bind(Callback)} instead
     * and call this method inside the callback.
     *
     * @return true if I2P Android is running, false otherwise.
     */
    public boolean isI2PAndroidRunning() {
        if (mStateService == null)
            return false;

        try {
            return mStateService.isStarted();
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Failed to communicate with I2P Android", e);
            return false;
        }
    }

    /**
     * Show dialog - request that I2P Android be started.
     *
     * @param activity the Activity this method has been called from.
     */
    public void requestI2PAndroidStart(final Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.start_i2p_android)
                .setMessage(R.string.would_you_like_to_start_i2p_android)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent("net.i2p.android.router.START_I2P");
                        activity.startActivityForResult(i, REQUEST_START_I2P);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        builder.show();
    }

    /**
     * Check if tunnels are active. This is the best indicator that the default
     * tunnels (such as the HTTP proxy tunnel at localhost:4444) are ready to
     * be used. If {@link I2PAndroidHelper#bind()} has not been called
     * previously, this will always return false.
     * <p/>
     * Do not call this from {@link Activity#onResume()}. The Android lifecycle
     * calls that method before binding the helper to the I2P app, so this will
     * always return false. Use {@link I2PAndroidHelper#bind(Callback)} instead
     * and call this method inside the callback.
     *
     * @return true if tunnels are active, false otherwise.
     * @since 0.7
     */
    public boolean areTunnelsActive() {
        if (mStateService == null)
            return false;

        try {
            return mStateService.getState() == State.ACTIVE;
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Failed to communicate with I2P Android", e);
            return false;
        }
    }
}
