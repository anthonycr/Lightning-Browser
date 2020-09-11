package acr.browser.lightning.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class MemoryLeakUtils {

    private static final String TAG = "MemoryLeakUtils";

    @Nullable private static Method sFinishInputLocked = null;

    private MemoryLeakUtils() {}

    /**
     * Clears the mNextServedView and mServedView in
     * InputMethodManager and keeps them from leaking.
     *
     * @param application the application needed to get
     *                    the InputMethodManager that is
     *                    leaking the views.
     */
    public static void clearNextServedView(@NonNull Activity activity, @NonNull Application application) {

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            // This shouldn't be a problem on N
            return;
        }

        InputMethodManager imm = (InputMethodManager) application.getSystemService(Context.INPUT_METHOD_SERVICE);

        if (sFinishInputLocked == null) {
            try {
                sFinishInputLocked = InputMethodManager.class.getDeclaredMethod("finishInputLocked");
            } catch (NoSuchMethodException e) {
                Log.d(TAG, "Unable to find method in clearNextServedView", e);
            }
        }

        boolean isCurrentActivity = false;

        try {
            Field servedViewField = InputMethodManager.class.getDeclaredField("mNextServedView");
            servedViewField.setAccessible(true);
            Object servedView = servedViewField.get(imm);
            if (servedView instanceof View) {
                isCurrentActivity = ((View) servedView).getContext() == activity;
            }
        } catch (NoSuchFieldException e) {
            Log.d(TAG, "Unable to get mNextServedView field", e);
        } catch (IllegalAccessException e) {
            Log.d(TAG, "Unable to access mNextServedView field", e);
        }

        if (sFinishInputLocked != null && isCurrentActivity) {
            sFinishInputLocked.setAccessible(true);
            try {
                sFinishInputLocked.invoke(imm);
            } catch (Exception e) {
                Log.d(TAG, "Unable to invoke method in clearNextServedView", e);
            }
        }

    }

    @SuppressWarnings("NoopMethodInAbstractClass")
    public static abstract class LifecycleAdapter implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {}

        @Override
        public void onActivityStarted(@NonNull Activity activity) {}

        @Override
        public void onActivityResumed(@NonNull Activity activity) {}

        @Override
        public void onActivityPaused(@NonNull Activity activity) {}

        @Override
        public void onActivityStopped(@NonNull Activity activity) {}

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {}
    }


}
