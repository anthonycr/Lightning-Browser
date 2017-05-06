package acr.browser.lightning.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class MemoryLeakUtils {

    private static final String TAG = "MemoryLeakUtils";

    @Nullable private static Method sFinishInputLocked = null;

    /**
     * Clears the mNextServedView and mServedView in
     * InputMethodManager and keeps them from leaking.
     *
     * @param application the application needed to get
     *                    the InputMethodManager that is
     *                    leaking the views.
     */
    public static void clearNextServedView(Activity activity, @NonNull Application application) {

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

    public static abstract class LifecycleAdapter implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

        @Override
        public void onActivityStarted(Activity activity) {}

        @Override
        public void onActivityResumed(Activity activity) {}

        @Override
        public void onActivityPaused(Activity activity) {}

        @Override
        public void onActivityStopped(Activity activity) {}

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

        @Override
        public void onActivityDestroyed(Activity activity) {}
    }


}
