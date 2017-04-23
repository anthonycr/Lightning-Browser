package acr.browser.lightning.search.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.support.v4.app.NotificationCompat.Builder;
import android.widget.RemoteViews;

import acr.browser.lightning.R;
import acr.browser.lightning.activity.MainActivity;

import static acr.browser.lightning.constant.Constants.INTENT_ACTION_SEARCH;

/* renamed from: com.baidu.browser.inter.a.c */
public final class NotificationUtil {
    public static final int REQUEST_CODE_SEARCH_NOTIFICATION = 9999;
    public static final int REQUEST_CODE_ARTICLE_NOTIFICATION = 8888;

    private static Notification getNotification(Context context) {
        try {
            Builder priority = new Builder(context).setSmallIcon(R.drawable.ic_search_notification_small_icon).setAutoCancel(false).setOngoing(true).setPriority(2);
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_search);
            NotificationUtil.setPendingIntent(context, remoteViews);

            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra(INTENT_ACTION_SEARCH, INTENT_ACTION_SEARCH);
            intent.setAction(INTENT_ACTION_SEARCH);
            PendingIntent activity = PendingIntent.getActivity(context, REQUEST_CODE_SEARCH_NOTIFICATION, intent, 0);
            Notification build = priority
                    .setContent(remoteViews)
                    .setContentIntent(activity)
                    .build();
            if (VERSION.SDK_INT >= 16) {
                remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_search);
                remoteViews.setTextViewText(R.id.notify_title, context.getResources().getString(R.string.search_notification_title));
                setPendingIntent(context, remoteViews);
                intent = new Intent(context, MainActivity.class);
                intent.putExtra(INTENT_ACTION_SEARCH, INTENT_ACTION_SEARCH);
                intent.setAction(INTENT_ACTION_SEARCH);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, REQUEST_CODE_SEARCH_NOTIFICATION, intent, 0);
                remoteViews.setOnClickPendingIntent(R.id.notify_container, pendingIntent);
                build.bigContentView = remoteViews;
            }
            return build;
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
    }

    public static void showSearchNotification(Context context) {
        try {
            context.startService(new Intent(context, CommonPersistentService.class));
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(REQUEST_CODE_SEARCH_NOTIFICATION, getNotification(context));
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    private static void setPendingIntent(Context context, RemoteViews remoteViews) {
        Intent intent = new Intent(context, SearchNotifySettingActivity.class);
        intent.putExtra("from", "1");
        remoteViews.setOnClickPendingIntent(R.id.notify_setting, PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
    }

    public static void cancelNotification(Context context, int code) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(code);
    }
}
