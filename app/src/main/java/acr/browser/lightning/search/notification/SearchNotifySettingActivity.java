package acr.browser.lightning.search.notification;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.preference.PreferenceManager;

import static acr.browser.lightning.search.notification.NotificationUtil.REQUEST_CODE_SEARCH_NOTIFICATION;

public class SearchNotifySettingActivity extends Activity implements View.OnClickListener {

    public SearchNotifySettingActivity() {
        BrowserApp.getAppComponent().inject(this);
    }

    @Inject
    PreferenceManager mPreferenceManager;

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(1);
        setContentView(R.layout.dialog_search_notification_switch);
        //((LinearLayout) findViewById(R.id.content_panel)).addView(new C1556r(getApplicationContext()));   //  gif
        findViewById(R.id.dialog_cancel).setOnClickListener(this);
        findViewById(R.id.dialog_ok).setOnClickListener(this);
        findViewById(R.id.persist_notification_close_id).setOnClickListener(this);
//        Window window = this.getWindow();
//        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
//                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);

    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.dialog_ok:
                finish();
                break;
            case R.id.dialog_cancel:
                mPreferenceManager.setSearchNotificationEnabled(false);
                NotificationUtil.cancelNotification(this, REQUEST_CODE_SEARCH_NOTIFICATION);
                startService(new Intent(this, CommonPersistentService.class));
                finish();
                break;
            case R.id.persist_notification_close_id:
                finish();
                break;
            default:
                break;
        }
    }

    protected void onDestroy() {
        super.onDestroy();
    }
}
