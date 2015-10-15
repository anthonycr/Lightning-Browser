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
 * Copyright 8/22/2015 Anthony Restaino
 */
public final class PermissionsManager {

    private static PermissionsManager mInstance;
    private final Set<String> mPendingRequests = new HashSet<>();
    private final List<PermissionResult> mPendingActions = new ArrayList<>();

    public static PermissionsManager getInstance() {
        if (mInstance == null) {
            mInstance = new PermissionsManager();
        }
        return mInstance;
    }

    private void addPendingAction(@NonNull String[] permissions, @Nullable PermissionResult result) {
        if (result == null) {
            return;
        }
        result.addPermissions(permissions);
        mPendingActions.add(result);
    }

    public void requestPermissionsIfNecessary(@Nullable Activity activity, @NonNull String[] permissions, @Nullable PermissionResult result) {
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
        List<String> permList = new ArrayList<>();
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

    public static boolean checkPermissions(Activity activity, @NonNull String[] permissions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        } else if (activity == null) {
            return false;
        }
        boolean permissionsNecessary = true;
        for (String perm : permissions) {
            permissionsNecessary &= activity.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED;
        }
        return permissionsNecessary;
    }

    public void notifyPermissionsChange(@NonNull String[] permissions, @NonNull int[] results) {
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

    public static abstract class PermissionResult {

        private Set<String> mPermissions = new HashSet<>();

        public abstract void onGranted();

        public abstract void onDenied(String permission);

        public void onResult(String permission, int result) {
            if (result == PackageManager.PERMISSION_GRANTED) {
                mPermissions.remove(permission);
                if (mPermissions.isEmpty()) {
                    onGranted();
                }
            } else {
                onDenied(permission);
            }
        }

        public void addPermissions(@NonNull String[] perms) {
            Collections.addAll(mPermissions, perms);
        }
    }

}
