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

/* renamed from: com.baidu.browser.inter.a.c */
public final class NotificationUtil {
    private static Notification getNotification(Context context) {
        try {
            Builder priority = new Builder(context).setSmallIcon(R.drawable.ic_search_notification_small_icon).setAutoCancel(false).setOngoing(true).setPriority(2);
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.search_notification);
            Intent intent = new Intent(context, MainActivity.class);
            intent.setAction("action.notify.searchbar");
            PendingIntent activity = PendingIntent.getActivity(context, 0, intent, 134217728);
            NotificationUtil.setPendingIntent(context, remoteViews);
            Notification build = priority.setContent(remoteViews).setContentIntent(activity).build();
            if (VERSION.SDK_INT >= 16) {
                remoteViews = new RemoteViews(context.getPackageName(), R.layout.search_notification);
                remoteViews.setTextViewText(R.id.notify_title, context.getResources().getString(R.string.search_notification_title));
                /*if (d == null || d.size() <= 0) {
                    remoteViews.setViewVisibility(R.id.notify_layout_hotword, 8);
                } else {*/
                intent = new Intent(context, MainActivity.class);
                intent.setAction("action.notify.searchbar");
                intent.putExtra("show_keyword", true);
                remoteViews.setOnClickPendingIntent(R.id.notify_container, PendingIntent.getActivity(context, 1, intent, 268435456));
                remoteViews.setOnClickPendingIntent(R.id.notify_container, PendingIntent.getActivity(context, 1, intent, 268435456));
                //}
                setPendingIntent(context, remoteViews);
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
            notificationManager.notify(9999, getNotification(context));
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    private static void setPendingIntent(Context context, RemoteViews remoteViews) {
        Intent intent = new Intent(context, BdSearchNotifySettingActivity.class);
        intent.putExtra("from", "1");
        remoteViews.setOnClickPendingIntent(R.id.notify_setting, PendingIntent.getActivity(context, 0, intent, 134217728));
    }

    public static void cancelNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(9999);
    }
}
