package acr.browser.lightning.utils;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Copyright 8/22/2015 Anthony Restaino
 */
public class PermissionsManager {

    private static PermissionsManager mInstance;
    private Set<String> mPendingRequests = new HashSet<>();

    public static PermissionsManager getInstance() {
        if (mInstance == null) {
            mInstance = new PermissionsManager();
        }
        return mInstance;
    }

    public void requestPermissionsIfNecessary(Activity activity, @NonNull String[] permissions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || activity == null) {
            return;
        }
        List<String> permList = new ArrayList<>();
        for (String perm : permissions) {
            if (activity.checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED
                    && !mPendingRequests.contains(perm)) {
                permList.add(perm);
            }
        }
        if (!permList.isEmpty()) {
            String[] permsToRequest = permList.toArray(new String[permList.size()]);
            mPendingRequests.addAll(permList);
            activity.requestPermissions(permsToRequest, 1);
        }

    }

    public static boolean checkPermission(Activity activity, @NonNull String permission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        } else if (activity == null) {
            return false;
        }
        return activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
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

    public void notifyPermissionsChange(String[] permissions) {
        for (String perm : permissions) {
            mPendingRequests.remove(perm);
        }
    }

}
