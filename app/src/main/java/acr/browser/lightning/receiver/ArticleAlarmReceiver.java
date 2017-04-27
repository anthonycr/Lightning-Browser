package acr.browser.lightning.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import java.util.Calendar;

import javax.inject.Inject;

import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.search.notification.ArticleNotificationService;

public class ArticleAlarmReceiver extends BroadcastReceiver {
 
    private static final String DEBUG_TAG = ArticleAlarmReceiver.class.getSimpleName();
    private static final long THREE_DAYS_IN_MILLI = 1000 * 60 * 60 * 24 * 3;

    @Inject
    PreferenceManager mPreferenceManager;

    public static void setRecurringArticleAlarm(Context context) {
        Calendar updateTime = Calendar.getInstance();

        updateTime.set(Calendar.HOUR_OF_DAY, 15);

        Intent downloader = new Intent(context, ArticleAlarmReceiver.class);
        PendingIntent recurringIntent = PendingIntent.getBroadcast(context, 0, downloader,
                PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, updateTime.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, recurringIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BrowserApp.getAppComponent()!=null && this!=null){
            BrowserApp.getAppComponent().inject(this);
        }else{
            return;
        }
        if (intent == null) {
            return;
        }
        if (TextUtils.equals(intent.getAction(), "android.intent.action.BOOT_COMPLETED") ||
                TextUtils.equals(intent.getAction(), "com.android.vending.INSTALL_REFERRER")) {
            setRecurringArticleAlarm(context);
            return;
        }
        if (mPreferenceManager!=null){
            long lastNotification = mPreferenceManager.getNextAppNotification();
            if (lastNotification == 0){
                setFutureNotification();//set notification time to 3 days from now.
                return;
            }else if (System.currentTimeMillis() > lastNotification){
                Log.d(DEBUG_TAG, "Recurring alarm; requesting article service");
                Intent articleIntent = new Intent(context, ArticleNotificationService.class);
                context.startService(articleIntent);
            }
        }
    }

    private void setFutureNotification() {
        if (mPreferenceManager!=null){
            mPreferenceManager.setNextAppNotification(System.currentTimeMillis() + THREE_DAYS_IN_MILLI);
        }
    }

}