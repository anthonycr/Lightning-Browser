package acr.browser.lightning.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import java.util.Calendar;

import acr.browser.lightning.search.notification.ArticleNotificationService;

public class ArticleAlarmReceiver extends BroadcastReceiver {
 
    private static final String DEBUG_TAG = ArticleAlarmReceiver.class.getSimpleName();

    public static void setRecurringArticleAlarm(Context context) {
        Calendar updateTime = Calendar.getInstance();
        updateTime.set(Calendar.HOUR_OF_DAY, 14);

        Intent downloader = new Intent(context, ArticleAlarmReceiver.class);
        PendingIntent recurringIntent = PendingIntent.getBroadcast(context, 0, downloader,
                PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, updateTime.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, recurringIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        if (TextUtils.equals(intent.getAction(), "android.intent.action.BOOT_COMPLETED")) {
            setRecurringArticleAlarm(context);
            return;
        }
        Log.d(DEBUG_TAG, "Recurring alarm; requesting article service");
        Intent articleIntent = new Intent(context, ArticleNotificationService.class);
        context.startService(articleIntent);
    }
 
}