package acr.browser.lightning.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;

public class SplashActivity extends AppCompatActivity {

    public static final String TAG = SplashActivity.class.getPackage() + "." + SplashActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Handler(Looper.myLooper()).post(
                new Runnable() {
                    @Override
                    public void run() {
//                        //init Exception tracking
//                        ACRA.init(SearchDemoAppContext.this);
//                        ACRAConfiguration conf = new ACRAConfiguration();
//                        conf.setBuildConfigClass(BuildConfig.class);
//                        ACRA.setConfig(conf);
//                        ACRA.getErrorReporter().setReportSender(new HockeySender("0d2c7f75c56543b5b9534acb5b61bc83", "18e009c76367409aa2e8c34b9ee2575c", SearchDemoAppContext.this));


                        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
                            @Override
                            protected String doInBackground(Void... params) {
                                //init Mobitech search SDK with the user's advertising ID
                                String userAdvId = "";
                                try {

                                    AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(SplashActivity.this);
                                    if (!adInfo.isLimitAdTrackingEnabled()) {
                                        userAdvId = adInfo.getId();
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, e.getMessage(), e);
                                }
                                return userAdvId;
                            }

                            @Override
                            protected void onPostExecute(String userAdvId) {
                                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                                SplashActivity.this.startActivity(intent);
                                finish();
                            }

                        };
                        task.execute();
                    }
                }
        );


    }
}
