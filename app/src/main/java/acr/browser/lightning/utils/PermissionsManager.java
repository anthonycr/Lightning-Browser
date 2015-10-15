package acr.browser.lightning.utils;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class to help you manage your permissions
 */
public final class PermissionsManager {

    private final static PermissionsManager INSTANCE = new PermissionsManager();
    private final Set<String> mPendingRequests = new HashSet<>(1);
    private final List<PermissionResult> mPendingActions = new ArrayList<>(1);

    public static PermissionsManager getInstance() {
        return INSTANCE;
    }

    /**
     * This method adds the {@link PermissionResult} to the current list
     * of pending actions that will be completed when the permissions are
     * received. The list of permissions passed to this method are registered
     * in the PermissionResult object so that it will be notified of changes
     * made to these permissions.
     *
     * @param permissions the required permissions for the result to be executed
     * @param result      the result to add to the current list of pending actions
     */
    private synchronized void addPendingAction(@NonNull String[] permissions, @Nullable PermissionResult result) {
        if (result == null) {
            return;
        }
        result.registerPermissions(permissions);
        mPendingActions.add(result);
    }

    /**
     * This method should be used to execute a {@link PermissionResult} for the array
     * of permissions passed to this method. This method will request the permissions if
     * they need to be requested (i.e. we don't have permission yet) and will add the
     * PermissionResult to the queue to be notified of permissions being granted or
     * denied. In the case of pre-Android Marshmallow, permissions will be granted immediately.
     * The Activity variable is nullable, but if it is null, the method will fail to execute.
     * This is only nullable as a courtesy for Fragments where getActivity() may yeild null
     * if the Fragment is not currently added to its parent Activity.
     *
     * @param activity    the activity necessary to request the permissions
     * @param permissions the list of permissions to request for the {@link PermissionResult}
     * @param result      the PermissionResult to notify when the permissions are granted or denied
     */
    public synchronized void requestPermissionsIfNecessaryForResult(@Nullable Activity activity,
                                                                    @NonNull String[] permissions,
                                                                    @Nullable PermissionResult result) {
        if (activity == null) {
            return;
        }
        addPendingAction(permissions, result);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            for (String perm : permissions) {
                if (result != null) {
                    result.onResult(perm, PackageManager.PERMISSION_GRANTED);
                }
            }
            return;
        }
        List<String> permList = new ArrayList<>(1);
        for (String perm : permissions) {
            if (ActivityCompat.checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED) {
                if (!mPendingRequests.contains(perm)) {
                    permList.add(perm);
                }
            } else {
                if (result != null) {
                    result.onResult(perm, PackageManager.PERMISSION_GRANTED);
                }
            }
        }
        if (!permList.isEmpty()) {
            String[] permsToRequest = permList.toArray(new String[permList.size()]);
            mPendingRequests.addAll(permList);
            ActivityCompat.requestPermissions(activity, permsToRequest, 1);
        }

    }

    /**
     * This method notifies the PermissionsManager that the permissions have change. It should
     * be called from the Activity callback onRequestPermissionsResult() with the variables
     * passed to that method. It will notify all the pending PermissionResult objects currently
     * in the queue, and will remove the permissions request from the list of pending requests.
     *
     * @param permissions the permissions that have changed
     * @param results     the values for each permission
     */
    public synchronized void notifyPermissionsChange(@NonNull String[] permissions, @NonNull int[] results) {
        int size = permissions.length;
        if (results.length < size) {
            size = results.length;
        }
        for (int n = 0; n < size; n++) {
            for (PermissionResult result : mPendingActions) {
                result.onResult(permissions[n], results[n]);
                mPendingRequests.remove(permissions[n]);
            }
        }
    }

    /**
     * This abstract class should be used to create an if/else action that the PermissionsManager
     * can execute when the permissions you request are granted or denied. Simple use involves
     * creating an anonymous instance of it and passing that instance to the
     * requestPermissionsIfNecessaryForResult method. The result will be sent back to you as
     * either onGranted (all permissions have been granted), or onDenied (a required permission
     * has been denied). Ideally you put your functionality in the onGranted method and notify
     * the user what won't work in the onDenied method.
     */
    public static abstract class PermissionResult {

        private final Set<String> mPermissions = new HashSet<>(1);

        public abstract void onGranted();

        public abstract void onDenied(String permission);

        /**
         * This method is called when a particular permission has changed.
         * This method will be called for all permissions, so this method determines
         * if the permission affects the state or not and whether it can proceed with
         * calling onGranted or if onDenied should be called.
         *
         * @param permission the permission that changed
         * @param result     the result for that permission
         */
        public synchronized final void onResult(String permission, int result) {
            if (result == PackageManager.PERMISSION_GRANTED) {
                mPermissions.remove(permission);
                if (mPermissions.isEmpty()) {
                    onGranted();
                }
            } else {
                onDenied(permission);
            }
        }

        /**
         * This method registers the PermissionResult object for the specified permissions
         * so that it will know which permissions to look for changes to. The PermissionResult
         * will then know to look out for changes to these permissions.
         *
         * @param perms the permissions to listen for
         */
        public synchronized final void registerPermissions(@NonNull String[] perms) {
            Collections.addAll(mPermissions, perms);
        }
    }

}
