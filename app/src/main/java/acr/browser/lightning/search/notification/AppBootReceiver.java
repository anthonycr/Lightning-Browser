package acr.browser.lightning.search.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

public class AppBootReceiver extends BroadcastReceiver {

    public static void startService(Context context) {
        context.startService(new Intent(context, CommonPersistentService.class));
    }

    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        CharSequence action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            return;
        }
        if (TextUtils.equals(action, "android.intent.action.BOOT_COMPLETED")) {
            Intent intent2 = new Intent(context, CommonPersistentService.class);
            intent2.setAction("acr.browser.lightning.search.notification.CommonPersistentService.action.uploadChannelHeartbeat");
            context.startService(intent2);
            NotificationUtil.showSearchNotification(context);
        }
    }
}
