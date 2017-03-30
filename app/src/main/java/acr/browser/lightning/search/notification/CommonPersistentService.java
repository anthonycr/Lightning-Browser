package acr.browser.lightning.search.notification;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class CommonPersistentService extends Service {
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        /*ClipboardMgr c = ClipboardMgr.m1048c();
        c.f1244a = (ClipboardManager) BdApplication.m5347b().getSystemService("clipboard");
        c.f1244a.addPrimaryClipChangedListener(c);
        c.f1245b = new HomeKeyReceive(c);
        registerReceiver(c.f1245b, c.f1245b.f1241a);*/
    }

    public void onDestroy() {
        super.onDestroy();
        //unregisterReceiver(ClipboardMgr.m1048c().f1245b);
        AppBootReceiver.startService(getApplicationContext());
    }

    public int onStartCommand(Intent intent, int i, int i2) {
//        startForeground(99999, new Notification());
        return START_STICKY;
    }
}
