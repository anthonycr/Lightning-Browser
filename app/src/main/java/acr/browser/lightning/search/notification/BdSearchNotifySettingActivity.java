package acr.browser.lightning.search.notification;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.preference.PreferenceManager;

public class BdSearchNotifySettingActivity extends Activity implements View.OnClickListener {

    public BdSearchNotifySettingActivity() {
        BrowserApp.getAppComponent().inject(this);
    }

    @Inject
    PreferenceManager mPreferenceManager;

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.dialog_cancel:
                mPreferenceManager.setSearchNotificationEnabled(false);
                NotificationUtil.cancelNotification(this);
                startService(new Intent(this, CommonPersistentService.class));
                finish();
            case R.id.dialog_ok:
                finish();
            default:
        }
    }

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(1);
        setContentView(R.layout.dialog_search_notification_switch);
        //((LinearLayout) findViewById(R.id.content_panel)).addView(new C1556r(getApplicationContext()));   //  gif
        ((Button) findViewById(R.id.dialog_ok)).setOnClickListener(this);
        ((Button) findViewById(R.id.dialog_cancel)).setOnClickListener(this);
    }

    protected void onDestroy() {
        super.onDestroy();
    }
}
