package acr.browser.lightning.utils;

import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Class for tracking app time in background.
 * If time in background was more than 5 minutes,
 * then we probably should show a new tab
 */
public class VisibilityManager {

    private static final String TAG = VisibilityManager.class.getSimpleName();
    public static boolean showNewTab = false;
    private static Timer mActivityTransitionTimer;
    private static TimerTask mActivityTransitionTimerTask;
    private static long timeSentToBackground;
    private static final long MAX_ACTIVITY_TRANSITION_TIME_MS = 5000;

    /**
     * Called on Activity.onStop()
     */
    public static void startActivityTransitionTimer() {
        Log.d(TAG, "startActivityTransitionTimer: ");
        mActivityTransitionTimer = new Timer();
        mActivityTransitionTimerTask = new TimerTask() {
            public void run() {
                timeSentToBackground = System.currentTimeMillis();
            }
        };

        mActivityTransitionTimer.schedule(mActivityTransitionTimerTask, MAX_ACTIVITY_TRANSITION_TIME_MS);
    }

    /**
     * Called on Activity.onStart()
     */
    public static void stopActivityTransitionTimer() {
        Log.d(TAG, "stopActivityTransitionTimer: ");
        if (mActivityTransitionTimerTask != null) {
            mActivityTransitionTimerTask.cancel();
        }
        if (mActivityTransitionTimer != null) {
            mActivityTransitionTimer.cancel();
        }
        //showNewTab = timeSentToBackground != 0 && System.currentTimeMillis() - timeSentToBackground > TimeUnit.SECONDS.toMillis(5); //  for test
        showNewTab = timeSentToBackground != 0 && System.currentTimeMillis() - timeSentToBackground > TimeUnit.MINUTES.toMillis(5);
        timeSentToBackground = 0;
    }

    /**
     * Resets variables
     */
    public static void resetShowNewTab() {
        showNewTab = false;
        timeSentToBackground = 0;
    }
}